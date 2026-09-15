#!/bin/bash
#
# fix-registry-route.sh — runs ON THE SERVER (uploaded by pam-deploy.sh before
# every deploy). Detects a Docker bridge network that swallowed the route to the
# image registry and repairs it without any manual work:
#
#   A compose network declared without a subnet takes Docker's next default
#   pool (172.18.0.0/16, 172.19.0.0/16, …). The host then routes that whole /16
#   into the bridge — and the registry lives at 172.18.x.x, so every pull fails
#   with "no route to host".
#
# For each such network: find the compose project that owns it, back up its
# docker-compose file, pin the network to a free /29 in 172.31.100.0/24 (the
# portal convention), take the project down (drops the bad route) and bring it
# back up — unless it is the stack being deployed right now, whose latest.sh
# does the up itself from the freshly uploaded, pinned compose file.
#
# Usage: fix-registry-route.sh <registry-host> <remote-root> <current-stack-dir>
set -u

REG_HOST="${1:-crsk-docker-private.rmb.co.za}"
REMOTE_ROOT="${2:-/opt/docker}"
CURRENT_DIR="${3:-}"
POOL_PREFIX="172.31.100"

resolve() { getent hosts "$REG_HOST" 2>/dev/null | awk '{print $1}' | head -1; }
route_dev() { ip route get "$1" 2>/dev/null | sed -n 's/.* dev \([^ ]*\).*/\1/p' | head -1; }

# next /29 in 172.31.100.0/24 not used by any existing docker network
free_subnet() {
  local used; used=$(for id in $(docker network ls -q); do docker network inspect "$id" -f '{{range .IPAM.Config}}{{.Subnet}} {{end}}'; done 2>/dev/null | tr ' ' '\n' | grep "^$POOL_PREFIX\." | cut -d. -f4 | cut -d/ -f1)
  local n=0
  while [ $n -le 248 ]; do
    if ! echo "$used" | grep -qx "$n"; then echo "$POOL_PREFIX.$n/29"; return; fi
    n=$((n+8))
  done
  echo "$POOL_PREFIX.248/29"
}

# pin_network <compose-file> <network-name-in-compose> <subnet>
pin_network() {
  local file="$1" net="$2" subnet="$3"
  cp -p "$file" "$file.bak-$(date +%Y%m%d%H%M%S)"
  awk -v net="$net" -v subnet="$subnet" '
    BEGIN { innets=0; innet=0; done=0 }
    /^networks:[[:space:]]*$/ { innets=1; print; next }
    innets && /^[^[:space:]]/ { innets=0 }                     # left the networks: block
    innets && $0 ~ "^  " net ":[[:space:]]*$" { innet=1; print; next }
    innets && innet && /^  [^[:space:]]/ { innet=0 }            # next network
    innet && !done && /^    driver:[[:space:]]*bridge/ {
      print
      print "    ipam:"; print "      config:"; print "        - subnet: " subnet
      done=1; next
    }
    { print }
    END { if (!done) exit 3 }
  ' "$file" > "$file.pinned" && mv "$file.pinned" "$file" && return 0
  rm -f "$file.pinned"; return 1
}

REG_IP=$(resolve)
if [ -z "$REG_IP" ]; then echo "registry route check: cannot resolve $REG_HOST — skipped"; exit 0; fi

for attempt in 1 2 3 4 5; do
  VIA=$(route_dev "$REG_IP")
  case "$VIA" in br-*|docker*) ;; *) [ "$attempt" = 1 ] && echo "registry route check: $REG_HOST ($REG_IP) via $VIA — ok"; exit 0 ;; esac

  echo "!! registry $REG_HOST ($REG_IP) is routed into Docker bridge $VIA (a compose network took a default pool)"
  NET_ID=""; NET_NAME=""; SUBNET=""; PROJECT=""; WORKDIR=""; CNAME=""
  for id in $(docker network ls -q); do
    if [ "${id#${VIA#br-}}" != "$id" ] || [ "$(echo "$id" | cut -c1-12)" = "${VIA#br-}" ]; then
      NET_ID="$id"
      NET_NAME=$(docker network inspect "$id" -f '{{.Name}}')
      SUBNET=$(docker network inspect "$id" -f '{{range .IPAM.Config}}{{.Subnet}}{{end}}')
      PROJECT=$(docker network inspect "$id" -f '{{index .Labels "com.docker.compose.project"}}')
      WORKDIR=$(docker network inspect "$id" -f '{{index .Labels "com.docker.compose.project.working_dir"}}')
      CNAME=$(docker network inspect "$id" -f '{{index .Labels "com.docker.compose.network"}}')
      break
    fi
  done
  echo "   network: ${NET_NAME:-?} subnet: ${SUBNET:-?} compose project: ${PROJECT:-?} dir: ${WORKDIR:-?}"

  if [ -z "$NET_ID" ]; then echo "   could not identify the network — nothing changed"; exit 0; fi
  if [ -z "$WORKDIR" ] || [ ! -d "$WORKDIR" ]; then
    echo "   not a compose-managed network (or its directory is gone); removing it after disconnecting its containers"
    for c in $(docker network inspect "$NET_ID" -f '{{range $k,$v := .Containers}}{{$k}} {{end}}'); do docker network disconnect -f "$NET_ID" "$c" || true; done
    docker network rm "$NET_ID" || true
    continue
  fi

  COMPOSE=""; for f in docker-compose.yml docker-compose.yaml; do [ -f "$WORKDIR/$f" ] && COMPOSE="$WORKDIR/$f" && break; done
  NEW_SUBNET=$(free_subnet)
  echo "   taking $PROJECT down (drops the route)"
  ( cd "$WORKDIR" && docker compose down --remove-orphans ) || true

  if [ -n "$COMPOSE" ] && [ -n "$CNAME" ]; then
    if grep -qE "^\s*-\s*subnet:" "$COMPOSE" && awk -v net="$CNAME" '$0 ~ "^  " net ":"{f=1} f && /subnet:/{found=1} END{exit !found}' "$COMPOSE"; then
      echo "   $COMPOSE already pins $CNAME — leaving the file alone"
    elif pin_network "$COMPOSE" "$CNAME" "$NEW_SUBNET"; then
      echo "   pinned $CNAME to $NEW_SUBNET in $COMPOSE (backup kept next to it)"
    else
      echo "   WARN: could not pin $CNAME in $COMPOSE automatically (no 'driver: bridge' line under it) — add ipam.config.subnet: $NEW_SUBNET"
    fi
  fi

  if [ -n "$CURRENT_DIR" ] && [ "$(cd "$WORKDIR" && pwd)" = "$(cd "$CURRENT_DIR" 2>/dev/null && pwd)" ]; then
    echo "   this is the stack being deployed — latest.sh brings it back up on the pinned subnet"
  else
    echo "   bringing $PROJECT back up"
    ( cd "$WORKDIR" && docker compose up -d ) || echo "   WARN: $PROJECT did not come back up — check $WORKDIR"
  fi
done

VIA=$(route_dev "$REG_IP")
case "$VIA" in br-*|docker*) echo "WARN: $REG_HOST still routed via $VIA — pulls will fail until this is resolved" ;; *) echo "registry route restored: $REG_HOST via $VIA" ;; esac
exit 0
