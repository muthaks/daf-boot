#!/bin/bash
# DAF Committee Agendas remote (daf-boot) Deployment Script
# Pulls the tag from .env, restarts the remote, verifies it, tails logs
# Usage: ./latest.sh

set -e

COMPOSE_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$COMPOSE_DIR"

if [ ! -f env.properties ]; then
  echo "ERROR: env.properties not found (copy env.properties.example and fill it in)"
  exit 1
fi

# Export env.properties so docker compose can interpolate variables in the YAML
export $(grep -v '^\s*#' env.properties | grep -v '^\s*$' | xargs -d '\n')

if [ -z "${DAF_API_URL:-}" ]; then
  echo "ERROR: set DAF_API_URL in env.properties (the API gateway's /daf route, e.g. https://credit-systems-dev.rmb.co.za/gateway/daf)"
  exit 1
fi

echo "=========================================="
echo " daf-remote - Pulling ${BUILD_NUMBER:-latest} image..."
echo "=========================================="
# Registry access from Docker on these servers is unreliable, so a failed pull is a warning and
# the deploy continues with the image already on the box. A tag that was never pulled fails at "up".
docker compose pull || echo "Warning: Pull failed. Using local image if available."

echo ""
echo "=========================================="
echo " daf-remote - Restarting..."
echo "=========================================="
docker compose down
docker compose up -d

echo ""
echo "=========================================="
echo " daf-remote - Verifying..."
echo "=========================================="
verify() {
  local url="$1" status=000
  for i in $(seq 1 30); do
    status=$(curl -s -o /dev/null -w '%{http_code}' "$url" || true)
    [ "$status" = "200" ] && break
    sleep 2
  done
  if [ "$status" = "200" ]; then
    echo "OK:   $url -> 200"
  else
    echo "WARN: $url -> $status"
  fi
}
verify "http://localhost:${REMOTE_PORT:-14708}/remoteEntry.json"
echo "remote config: $(curl -s "http://localhost:${REMOTE_PORT:-14708}/assets/config.json")"

echo ""
echo "=========================================="
echo " daf-remote - Tailing logs..."
echo "=========================================="
exec docker compose logs -f
