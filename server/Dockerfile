# syntax=docker/dockerfile:1
#
# Docker image for the InsighKu receipt-scan API (Flask + Donut).
# Designed for Hugging Face Spaces (Docker SDK, app_port 8080, free CPU tier).
# Also runs on any Docker host.

FROM python:3.11-slim

# Pillow/torch image deps.
RUN apt-get update && apt-get install -y --no-install-recommends \
        libgl1 libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# CPU-only torch first (keeps the image small; HF free tier has no GPU).
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu

# Flask app deps (torch is intentionally NOT in requirements.txt).
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY cord_parser.py app.py ./

# Unbuffered logs so HF Space logs stream live.
ENV PYTHONUNBUFFERED=1 \
    HF_HUB_DOWNLOAD_TIMEOUT=60

# HF Spaces routes to this port (see README frontmatter app_port).
EXPOSE 8080

# Spaces run as a non-root user by default; the app writes nothing to disk.
CMD ["python", "app.py"]
