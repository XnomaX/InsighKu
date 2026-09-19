#!/usr/bin/env bash
# Start the InsighKu receipt-scan server + a public cloudflared tunnel.
#
# Usage:  ./start.sh
# Then read the printed TUNNEL_URL — put it (with trailing /) into
# ../../local.properties as receiptApiBaseUrl, rebuild the APK, install.
#
# Quick-tunnel URLs are ephemeral: they change on every restart of this script.
# For a stable URL, set up a named cloudflared tunnel (see server/README.md).
set -euo pipefail

cd "$(dirname "$0")"

# API key — keep stable across restarts so the app keeps working without
# rebuilding. Persist it next to the model so restarts reuse it.
KEY_FILE=.api_key
if [ ! -f "$KEY_FILE" ]; then
  python3 -c "import secrets; print(secrets.token_hex(16))" > "$KEY_FILE"
fi
API_KEY="$(cat "$KEY_FILE")"

# Start the Flask server (model already downloaded under ./model).
MODEL_DIR=model DEVICE=cpu PORT=8080 RECEIPT_API_KEY="$API_KEY" \
  .venv/bin/python app.py > /tmp/insightku_server.log 2>&1 &
SERVER_PID=$!
echo "server PID $SERVER_PID (log: /tmp/insightku_server.log)"

# Wait for /healthz
for i in $(seq 1 90); do
  curl -sf http://localhost:8080/healthz >/dev/null 2>&1 && break
  kill -0 "$SERVER_PID" 2>/dev/null || { echo "server died:"; tail -30 /tmp/insightku_server.log; exit 1; }
  sleep 2
done
echo "server ready: $(curl -s http://localhost:8080/healthz)"

# Start cloudflared quick tunnel.
cloudflared tunnel --url http://localhost:8080 > /tmp/insightku_tunnel.log 2>&1 &
TUNNEL_PID=$!
echo "tunnel PID $TUNNEL_PID (log: /tmp/insightku_tunnel.log)"

URL=""
for i in $(seq 1 30); do
  URL="$(grep -oE 'https://[a-z0-9-]+\.trycloudflare\.com' /tmp/insightku_tunnel.log | head -1 || true)"
  [ -n "$URL" ] && break
  kill -0 "$TUNNEL_PID" 2>/dev/null || { echo "tunnel died:"; tail -30 /tmp/insightku_tunnel.log; exit 1; }
  sleep 2
done

cat <<EOF

=========================================================
  InsighKu receipt-scan server is UP.

  TUNNEL_URL  = $URL
  API_KEY     = $API_KEY

  Put these into ../../local.properties:
    receiptApiBaseUrl=$URL/
    receiptApiKey=$API_KEY
  Then rebuild + install the APK.
=========================================================
  Press Ctrl+C to stop both the server and the tunnel.
=========================================================
EOF

trap 'kill $SERVER_PID $TUNNEL_PID 2>/dev/null || true' INT TERM EXIT
wait
