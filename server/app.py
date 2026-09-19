"""
InsighKu receipt-scanning API — online-only Flask server.

Loads the Donut (VisionEncoderDecoder) model fine-tuned on CORD-v2 once at
startup, then exposes:
    GET  /healthz         -> {status, device, model_loaded}
    POST /scan-receipt    -> {merchant, total, items[], raw_text}

The model (`nurarief/insightku-feature`) is a private HF repo, so set HF_TOKEN
(or point MODEL_DIR at a local snapshot) before first run. After the first
download the snapshot is cached locally by huggingface_hub.

Donut is too large to run on the phone, so the Android client sends a photo
and receives structured JSON. Nothing (weights, images, results) is persisted.
"""

from __future__ import annotations

import io
import os

import torch
from flask import Flask, jsonify, request
from PIL import Image
from transformers import DonutProcessor, VisionEncoderDecoderModel

from cord_parser import parse_cord, strip_tokens

MODEL_ID = os.environ.get("MODEL_ID", "nurarief/insightku-feature")
# Optional local snapshot dir; if set, skip the HF download entirely.
MODEL_DIR = os.environ.get("MODEL_DIR", "")
HF_TOKEN = os.environ.get("HF_TOKEN", "")
API_KEY = os.environ.get("RECEIPT_API_KEY", "")
DEVICE = os.environ.get("DEVICE", "cuda" if torch.cuda.is_available() else "cpu")
MAX_IMAGE_BYTES = int(os.environ.get("MAX_IMAGE_BYTES", str(15 * 1024 * 1024)))  # 15 MiB

app = Flask(__name__)

# Loaded once at startup; shared across requests.
processor: DonutProcessor | None = None
model: VisionEncoderDecoderModel | None = None


def _load_model() -> None:
    global processor, model
    from_path = MODEL_DIR or MODEL_ID
    token = HF_TOKEN or None
    print(f"[insightku] loading model from '{from_path}' on {DEVICE} ...", flush=True)
    processor = DonutProcessor.from_pretrained(from_path, token=token)
    model = VisionEncoderDecoderModel.from_pretrained(from_path, token=token).to(DEVICE).eval()
    print("[insightku] model ready.", flush=True)


# --- routes ----------------------------------------------------------------


@app.get("/healthz")
def healthz():
    return jsonify(
        {
            "status": "ok",
            "device": DEVICE,
            "model_loaded": model is not None,
            "auth_required": bool(API_KEY),
        }
    )


@app.post("/scan-receipt")
def scan_receipt():
    if API_KEY and request.headers.get("X-API-Key") != API_KEY:
        return jsonify({"error": "unauthorized"}), 401

    if "image" not in request.files:
        return jsonify({"error": "missing 'image' field"}), 400

    file = request.files["image"]
    data = file.read()
    if not data:
        return jsonify({"error": "empty image"}), 400
    if len(data) > MAX_IMAGE_BYTES:
        return jsonify({"error": "image too large"}), 413

    if model is None or processor is None:
        return jsonify({"error": "model not loaded"}), 503

    try:
        image = Image.open(io.BytesIO(data)).convert("RGB")
    except Exception:
        return jsonify({"error": "invalid image"}), 400

    try:
        pixel_values = processor(image, return_tensors="pt").pixel_values.to(DEVICE)
        with torch.no_grad():
            out = model.generate(
                pixel_values,
                max_length=768,
                num_beams=1,
                pad_token_id=processor.tokenizer.pad_token_id,
                eos_token_id=processor.tokenizer.eos_token_id,
            )
        # Keep special tokens so parse_cord can read the CORD structure.
        token_seq = processor.tokenizer.decode(out[0], skip_special_tokens=False)
        plain = processor.tokenizer.decode(out[0], skip_special_tokens=True)
        parsed = parse_cord(token_seq)
        return jsonify(
            {
                # CORD-v2 has no merchant; left null for the client to fill.
                "merchant": None,
                "total": parsed["total"],
                "items": parsed["items"],
                # The custom fine-tune's CORD tokens aren't always flagged as
                # "special", so skip_special_tokens=True can leave them in
                # `plain`. Strip them so the client never shows token soup.
                "raw_text": strip_tokens(plain),
            }
        )
    except Exception as e:  # noqa: BLE001 - surface a stable error to the client
        return jsonify({"error": f"inference failed: {e}"}), 500


@app.errorhandler(413)
def too_large(_e):
    return jsonify({"error": "image too large"}), 413


@app.errorhandler(404)
def not_found(_e):
    return jsonify({"error": "not found"}), 404


if __name__ == "__main__":
    _load_model()
    # Dev server; use gunicorn in production: gunicorn -w 1 -b :8080 'app:app'
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", "8080")))
else:
    # When run under gunicorn (e.g. `gunicorn app:app`), load eagerly so the
    # first request doesn't stall and the worker reports readiness accurately.
    _load_model()
