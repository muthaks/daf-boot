#!/bin/bash
# DAF (daf-boot) Deployment Script: daf-api, daf-rules, daf-docgen, daf-soap
# Pulls the tag from env.properties, restarts the services, verifies them, tails logs
# Usage: ./latest.sh

set -e

COMPOSE_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$COMPOSE_DIR"

if [ ! -f env.properties ]; then
  echo "ERROR: env.properties not found (copy env.properties.example and fill it in)"
  exit 1
fi

# Export env.properties so docker compose can interpolate variables in the YAML. Line by line, as the deploy
# tool does: values hold spaces and cron wildcards, and names that are not shell identifiers (the legacy
# authentication.jwt.secret) are left to env_file, which passes them to the containers as they are.
while IFS= read -r line || [ -n "$line" ]; do
  line=$(printf "%s" "$line" | tr -d "\r")
  case "$line" in ""|\#*|*[[:space:]]=*) continue ;; esac
  [[ "$line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]] && export "$line"
done < env.properties

# Compose variables: env.properties or the server's .env may set them; otherwise the platform registry
# and the server's own resolver (the subnet defaults in docker-compose.yml).
export DOCKER_REGISTRY="${DOCKER_REGISTRY:-crsk-docker-private.rmb.co.za}"
[ -n "${DNS_SERVER:-}" ] || export DNS_SERVER="$(awk '/^nameserver/{print $2; exit}' /etc/resolv.conf 2>/dev/null)"

# A blank value replaces the code's default with nothing; without DAF's system sign-in there is no
# Credit Data or mail token. Sign-in and Credit Data calls would fail.
MISSING=""
for v in AF_AUTHSERVICE_BASE AF_APPLICATIONSERVICE_BASE AF_COUNTERPARTYSERVICE_BASE AF_FACILITYSERVICE_BASE AF_REFDATASERVICE_BASE; do
  [ -n "${!v:-}" ] || MISSING="$MISSING $v"
done
# DAF's system sign-in, by its name or an earlier one.
for pair in AF_SYSTEM_TOKEN_URL:AF_MAILSERVICE_SYSTEM_TOKEN_URL AF_SYSTEM_CALLER:AF_MAILSERVICE_CALLER_SYSTEM \
            AF_SYSTEM_BOOTSTRAP_CLIENT_SECRET:AF_SYSTEM_CLIENT_SECRET:AF_MAILSERVICE_CLIENT_SECRET; do
  IFS=: read -r -a names <<< "$pair"
  found=""; for n in "${names[@]}"; do [ -n "${!n:-}" ] && found=1; done
  [ -n "$found" ] || MISSING="$MISSING ${names[0]}"
done
if [ -n "$MISSING" ]; then
  echo "ERROR: set these in env.properties before deploying:$MISSING"
  exit 1
fi

echo "=========================================="
echo " daf-boot - Pulling ${BUILD_NUMBER:-latest} image..."
echo "=========================================="
# Registry access from Docker on these servers is unreliable, so a failed pull is a warning and
# the deploy continues with the image already on the box. A tag that was never pulled fails at "up".
docker compose pull || echo "Warning: Pull failed. Using local image if available."

echo ""
echo "=========================================="
echo " daf-boot - Restarting..."
echo "=========================================="
docker compose down
docker compose up -d

echo ""
echo "=========================================="
echo " daf-boot - Verifying..."
echo "=========================================="
verify() {
  local url="$1" status=000
  for i in $(seq 1 60); do
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
verify "http://localhost:${API_PORT:-14736}/approvalforum/api/actuator/health"
verify "http://localhost:${RULES_PORT:-14737}/approvalforumdrools/api/actuator/health"
verify "http://localhost:${DOCGEN_PORT:-14738}/docgen/actuator/health"
verify "http://localhost:${SOAP_PORT:-14739}/approvalforum-soap/approvalforumexternal/ws/approvalForumExternal.wsdl"

echo ""
echo "=========================================="
echo " daf-boot - Tailing logs..."
echo "=========================================="
exec docker compose logs -f
