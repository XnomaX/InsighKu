---
title: Insightku Receipt
emoji: 🧾
colorFrom: blue
colorTo: green
sdk: docker
app_port: 8080
pinned: false
---

# InsighKu receipt-scan API (Flask + Donut)

Online-only server that runs the Donut model (`nurarief/insightku-feature`,
fine-tuned on CORD-v2 Indonesian receipts) and exposes a receipt-scanning
endpoint for the Android app. The model is too large to run on the phone, so
the app sends a photo and receives structured JSON.

---

## Deploy to Hugging Face Spaces (recommended, free)

This repo is ready to push to a Space. Steps:

1. Create a **new Space** at https://huggingface.co/new-space:
   - **Owner:** `nurarief` (so it can read your private model repo)
   - **Name:** e.g. `insightku-receipt`
   - **SDK:** **Docker**
   - **Visibility:** Private (recommended — it has your model behind a token)
   - **Hardware:** **CPU basic** (free, 2 vCPU / 16 GiB) — enough for Donut
2. In **Settings → Repository secrets**, add:
   - `HF_TOKEN` = your Hugging Face token (read access to `nurarief/insightku-feature`).
     Note: a Space owned by `nurarief` can already read that owner's private repos, but
     setting the token guarantees access and avoids silent 401s.
   - `RECEIPT_API_KEY` = any random string (e.g. 32 chars). **Optional** but recommended:
     if set, the Android client must send the matching `X-API-Key` header, so randoms
     can't burn your free quota. Put the same value in the app's `local.properties`.
3. Push the contents of this `server/` folder to the Space repo:

   ```bash
   cd server
   git init && git remote add space https://huggingface.co/nurarief/insightku-receipt
   git add -A && git commit -m "deploy receipt-scan API"
   git push space main
   ```

   Or just upload the files (`Dockerfile`, `app.py`, `cord_parser.py`,
   `requirements.txt`) via the Space's web UI.
4. Watch **Logs** — first build downloads torch (~200 MB), then on first run the app
   downloads `nurarief/insightku-feature` (~250 MB) and loads it. Takes a few minutes
   once; cached afterwards (free-tier storage is ephemeral, so a cold restart
   re-downloads — fast, same infra).
5. The Space's public URL is `https://nurarief-insightku-receipt.hf.space`. Health check:

   ```bash
   curl https://nurarief-insightku-receipt.hf.space/healthz
   ```

   Use this base URL in the Android app's `local.properties`:
   `receiptApiBaseUrl=https://nurarief-insightku-receipt.hf.space/`

> Cold start: after ~48 h idle the free Space sleeps and the first request takes
> ~30–60 s to wake + reload the model. For a thesis demo this is fine; for live demos,
> ping `/healthz` once beforehand.

---

## Run locally (dev / VPS)

```bash
cd server
pip install torch    # https://pytorch.org/get-started/locally/  (CPU wheel is fine)
pip install -r requirements.txt
export HF_TOKEN=<your huggingface token>
export RECEIPT_API_KEY=<shared secret>   # optional
export DEVICE=cuda                       # optional; default = cuda if available else cpu
python app.py                            # serves on :8080
# or: gunicorn -w 1 -b 0.0.0.0:8080 --timeout 120 app:app
```

Model loads once at startup. CPU inference is multi-second per image — fine for a
demo; use a GPU for speed.

## Endpoints

### `GET /healthz`
```json
{ "status": "ok", "device": "cpu", "model_loaded": true, "auth_required": false }
```

### `POST /scan-receipt`
- **Body:** `multipart/form-data`, field `image` (jpg/png, ≤ 15 MiB).
- **Header:** `X-API-Key: <secret>` (only if `RECEIPT_API_KEY` is set).
- **200 →**
```json
{
  "merchant": null,
  "total": "32000",
  "items": [
    { "name": "ES KOPI SUSU SASTRA", "qty": "1", "price": "22000" },
    { "name": "GORENAN", "qty": "2", "price": "10000" }
  ],
  "raw_text": "<skip-special-tokens plain text>"
}
```
- **400** missing/invalid image · **401** bad/missing API key · **413** too large · **500** inference failed.

> `merchant` is always `null`: the CORD-v2 schema has no store-name field. The
> Android client leaves the merchant field blank for the user to type, fills the
> amount from `total`, and the description from `items`.

## Test the parser (no model/GPU needed)

```bash
python test_parse_cord.py   # or: python -m pytest test_parse_cord.py
```

## Smoke test (model loaded)

```bash
curl -s http://localhost:8080/healthz
curl -s -F "image=@receipt.jpg" http://localhost:8080/scan-receipt
```

## Env vars

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `HF_TOKEN` | on first model download | — | read the private model repo |
| `RECEIPT_API_KEY` | no | (none) | if set, client must send `X-API-Key` |
| `DEVICE` | no | cuda if available else cpu | torch device |
| `MODEL_DIR` | no | — | use a local snapshot instead of downloading |
| `MODEL_ID` | no | `nurarief/insightku-feature` | HF repo id |
| `PORT` | no | `8080` | HTTP listen port |
| `MAX_IMAGE_BYTES` | no | `15728640` | upload cap (15 MiB) |
