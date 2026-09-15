#!/usr/bin/env bash
#
# pam-deploy.sh — ship Docker stacks to a Credit server through the
# BeyondTrust PasswordSafe (PAM) SSH proxy and run their latest.sh.
#
# The PAM compound username lands us directly as the service account on the
# target (no sudo/su on the box): ssh -p 4422 'rmb.co.za\<you>@docker@<host>' passwordsafe.fnb.co.za
#
# A STACK is a directory that follows the Credit deploy convention
# (docker-compose.yml, latest.sh, env.properties.example, .env.example).
# A TARGET is a server plus its PAM route. A MANIFEST is a release: which build
# of which stack goes to an environment, applied in order in ONE session.
#   deploy/stacks/<stack>.env       what to ship, where it lives on the server, how to verify
#   deploy/targets/<target>.env     host, environment, PAM proxy/domain/account
#   deploy/releases/<env>.env       stack=build lines (the release manifest, versioned)
#
# Usage:
#   deploy/pam-deploy.sh <stack> <target> [-b <build>] [options]        one stack
#   deploy/pam-deploy.sh --release <manifest> <target> [options]         every stack in the manifest
#     -b <build>     image tag for the single stack (default: latest)
#     -u <user>      your AD user for PAM (required unless $SERVER_USER is set)
#     --only a,b     with --release: deploy only these stacks from the manifest
#     --force-env    overwrite env.properties/.env on the server from the examples
#                    (DESTRUCTIVE: replaces passwords/secrets; a backup is kept)
#     --yes          skip the "override existing deployment?" confirmations (unattended runs)
#     --no-run       upload files only; do not run latest.sh
#     --exec "<cmd>" run a command in the stack's remote directory instead of deploying
#     --dry-run      print the remote script; no connection
#     --verbose      ssh -vvv (use when PAM rejects the login, to see why)
#
# Examples:
#   deploy/pam-deploy.sh --release deploy/releases/dev.env rmb-rbvdvcsdm01      # roll the platform
#   deploy/pam-deploy.sh --release deploy/releases/dev.env rmb-rbvdvcsdm01 --only credit-shell,wsa-remote
#   deploy/pam-deploy.sh co-api-gateway rmb-rbvdvcsdm01 -b 42
#   deploy/pam-deploy.sh nginx rmb-rbvdvcsdm01 --exec "docker exec nginx nginx -T | grep 'location /gateway/'"
#
# Password: taken from $SERVER_PASSWORD when sshpass is installed (hands-off);
# otherwise ssh prompts once and the whole remote script runs in that session.
# Safety: an existing deployment is shown and must be confirmed; what is replaced
# is backed up; env.properties is never replaced without --force-env; a taken host
# port aborts that stack; a Docker bridge that hijacked the registry route is
# repaired on the server before anything is pulled (deploy/remote/fix-registry-route.sh).

set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$DEPLOY_DIR/.." && pwd)"
usage() { sed -n '3,42p' "$0"; exit "${1:-0}"; }

# ---- arguments ---------------------------------------------------------------
MANIFEST=""; STACK=""; TARGET=""; BUILD_NUMBER="latest"; ONLY=""
AD_USER="${SERVER_USER:-}"
FORCE_ENV=0; NO_RUN=0; DRY_RUN=0; VERBOSE=0; ASSUME_YES=0; EXEC_CMD=""
POSITIONAL=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --release) MANIFEST="$2"; shift 2 ;;
    --only) ONLY="$2"; shift 2 ;;
    -b) BUILD_NUMBER="$2"; shift 2 ;;
    -u) AD_USER="$2"; shift 2 ;;
    --force-env) FORCE_ENV=1; shift ;;
    --yes) ASSUME_YES=1; shift ;;
    --no-run) NO_RUN=1; shift ;;
    --exec) EXEC_CMD="$2"; shift 2 ;;
    --dry-run) DRY_RUN=1; shift ;;
    --verbose) VERBOSE=1; shift ;;
    -h|--help) usage 0 ;;
    -*) echo "ERROR: unknown option $1" >&2; usage 1 ;;
    *) POSITIONAL+=("$1"); shift ;;
  esac
done
if [[ -n "$MANIFEST" ]]; then
  [[ ${#POSITIONAL[@]} -eq 1 ]] || { echo "ERROR: --release <manifest> needs exactly one <target>" >&2; usage 1; }
  TARGET="${POSITIONAL[0]}"
  [[ -f "$MANIFEST" ]] || MANIFEST="$DEPLOY_DIR/releases/$MANIFEST.env"
  [[ -f "$MANIFEST" ]] || { echo "ERROR: manifest not found: $MANIFEST" >&2; exit 1; }
  [[ -z "$EXEC_CMD" ]] || { echo "ERROR: --exec is for a single stack" >&2; exit 1; }
else
  [[ ${#POSITIONAL[@]} -eq 2 ]] || usage 1
  STACK="${POSITIONAL[0]}"; TARGET="${POSITIONAL[1]}"
fi

[[ -n "$AD_USER" ]] || { echo "ERROR: no AD user: pass -u <your AD user> or export SERVER_USER" >&2; exit 1; }

TARGET_FILE="$DEPLOY_DIR/targets/$TARGET.env"
[[ -f "$TARGET_FILE" ]] || { echo "ERROR: unknown target '$TARGET' (see deploy/targets/)" >&2; exit 1; }
# shellcheck disable=SC1090
source "$TARGET_FILE"
: "${TARGET_HOST:?target file must set TARGET_HOST}"
: "${TARGET_ENV:?target file must set TARGET_ENV}"
PAM_PROXY_HOST="${PAM_PROXY_HOST:-passwordsafe.fnb.co.za}"
PAM_PROXY_PORT="${PAM_PROXY_PORT:-4422}"
PAM_DOMAIN="${PAM_DOMAIN:-rmb.co.za}"
PAM_ACCOUNT="${PAM_ACCOUNT:-docker}"
REMOTE_ROOT="${REMOTE_ROOT:-/opt/docker}"
REGISTRY_HOST="${REGISTRY_HOST:-crsk-docker-private.rmb.co.za}"
PAM_USER="${PAM_DOMAIN}\\${AD_USER}@${PAM_ACCOUNT}@${TARGET_HOST}"

# ---- the list of (stack, build) to deploy --------------------------------------
PLAN=()   # "stack=build"
if [[ -n "$MANIFEST" ]]; then
  while IFS= read -r line; do
    line="${line%%#*}"; line="${line//[$'\r ']/}"
    [[ -z "$line" ]] && continue
    s="${line%%=*}"; b="${line#*=}"
    if [[ -n "$ONLY" ]] && ! grep -qx "$s" <<<"${ONLY//,/$'\n'}"; then continue; fi
    PLAN+=("$s=${b:-latest}")
  done < "$MANIFEST"
  [[ ${#PLAN[@]} -gt 0 ]] || { echo "ERROR: nothing to deploy from $MANIFEST${ONLY:+ (after --only $ONLY)}" >&2; exit 1; }
else
  PLAN=("$STACK=$BUILD_NUMBER")
fi

# ---- helpers -------------------------------------------------------------------
# Files travel as base64 inside the one remote script (the PAM proxy may not
# allow scp). CRLF is stripped so Windows checkouts deploy cleanly.
emit_file() {  # emit_file <local> <remote-name>  (gzip + base64 keeps the ssh command small)
  local b64; b64=$(tr -d '\r' < "$1" | gzip -9c | base64 | tr -d '\n')
  printf 'echo "%s" | base64 -d | gunzip > "%s"\n' "$b64" "$2"
}

# Loads deploy/stacks/<stack>.env into the STACK_* variables used below.
load_stack() {  # load_stack <stack>
  local f="$DEPLOY_DIR/stacks/$1.env"
  [[ -f "$f" ]] || { echo "ERROR: unknown stack '$1' (see deploy/stacks/)" >&2; exit 1; }
  unset STACK_DIR REMOTE_NAME FILES TAG_VAR PORT_VARS PORT_GUARD VERIFY_URL LOG_TAIL_SECONDS
  # shellcheck disable=SC1090
  source "$f"
  : "${STACK_DIR:?$f must set STACK_DIR}"
  REMOTE_NAME="${REMOTE_NAME:-$1}"
  REMOTE_DIR="$REMOTE_ROOT/$REMOTE_NAME"
  FILES="${FILES:-docker-compose.yml latest.sh env.properties.example .env.example}"
  TAG_VAR="${TAG_VAR:-BUILD_NUMBER}"
  PORT_VARS="${PORT_VARS:-}"
  PORT_GUARD="${PORT_GUARD:-fail}"   # fail (default): abort this stack on a taken port; bump: next free one
  VERIFY_URL="${VERIFY_URL:-}"
  LOG_TAIL_SECONDS="${LOG_TAIL_SECONDS:-20}"
  SRC_DIR="$REPO_ROOT/$STACK_DIR"
  [[ -d "$SRC_DIR" ]] || { echo "ERROR: stack directory not found: $SRC_DIR" >&2; exit 1; }
  # Compose networks must pin a subnet (cat-utils docs/DEPLOYMENT.md): an unpinned bridge takes
  # Docker's next default pool (172.18.0.0/16 …) and hijacks the host route to the
  # registry, after which no image can be pulled on that server.
  local compose
  for compose in "$SRC_DIR"/docker-compose.yml "$SRC_DIR"/docker-compose.yaml; do
    [[ -f "$compose" ]] || continue
    if grep -qE '^\s*driver:\s*bridge' "$compose" && ! grep -qE '^\s*-\s*subnet:' "$compose"; then
      echo "ERROR: $(basename "$compose") in $STACK_DIR declares a bridge network without a pinned subnet." >&2
      echo "       Add ipam.config.subnet (a /29 in 172.31.100.0/24, see cat-utils docs/DEPLOYMENT.md) before deploying." >&2
      exit 1
    fi
  done
}

# The remote script segment that deploys one stack. Runs inside a subshell on
# the server so a failure (verify, port) marks the stack failed and the
# release continues with the next one.
# One remote function holds the deploy logic and is sent once; each stack in the
# plan only contributes its (gzipped) files plus one call. Keeps the ssh command
# small enough for the PAM proxy.
REMOTE_FUNCS='
# compose interpolates ${VAR}s from the environment (latest.sh exports env.properties the same way); load it
# here too so "docker compose ps" and the port checks see the same values as the running stack
load_env_properties() {
  [ -f env.properties ] || return 0
  local line
  while IFS= read -r line || [ -n "$line" ]; do
    line=$(printf "%s" "$line" | tr -d "\r")
    case "$line" in ""|\#*|*[[:space:]]=*) continue ;; esac
    [[ "$line" =~ ^[A-Za-z_][A-Za-z0-9_]*= ]] && export "$line"
  done < env.properties
}
deploy_stack() {  # deploy_stack <stack> <dir> <tag_var> <build> <port_vars> <port_guard> <verify_url> <tail_seconds> <files>
  local STACK="$1" DIR="$2" TAG_VAR="$3" BUILD="$4" PORT_VARS="$5" PORT_GUARD="$6" VERIFY_URL="$7" TAIL="$8" FILES="$9"
  cd "$DIR" || return 1
  load_env_properties
  echo; echo "########## $STACK -> $DIR ($TAG_VAR=$BUILD) ##########"
  local STAMP; STAMP=$(date +%Y%m%d%H%M%S)
  if [ -f docker-compose.yml ] || [ -f docker-compose.yaml ] || [ -f env.properties ]; then
    echo
    echo "!! EXISTING DEPLOYMENT FOUND in $DIR"
    echo "   files:      $(ls -m 2>/dev/null)"
    [ -f .env ] && echo "   current tag: $(grep -E "^$TAG_VAR=" .env | tail -1 || echo "(none)")"
    [ -f env.properties ] && echo "   env.properties: $(grep -cvE "^\s*(#|$)" env.properties) settings (may include passwords/secrets)"
    echo "   containers:"; docker compose ps -a --format "     {{.Name}}  {{.Image}}  {{.Status}}" 2>/dev/null || true
    echo
    echo "Continuing will:"
    echo "   - replace docker-compose.yml, latest.sh and the *.example files (backup in .deploy-backup/$STAMP)"
    echo "   - set $TAG_VAR=$BUILD in .env"
    if [ "$FORCE_ENV" = 1 ]; then echo "   - OVERWRITE env.properties from the example — passwords/secrets in it are LOST (backup kept)"; else echo "   - keep env.properties as it is (new keys appended, platform ports reset)"; fi
    if [ "$NO_RUN" = 1 ]; then echo "   - not restart anything (--no-run)"; else echo "   - stop the running containers, pull the image and start them again (./latest.sh)"; fi
    echo
    if [ "$ASSUME_YES" = 1 ]; then
      echo "(--yes given; overriding the existing deployment)"
    elif [ -r /dev/tty ]; then
      # drop keystrokes typed while the previous stack was running (a stray Enter or
      # Ctrl-C would otherwise answer this prompt)
      while IFS= read -r -t 0.3 _junk < /dev/tty; do :; done
      printf "Override the existing deployment of %s on %s? Type yes to continue: " "$STACK" "$(hostname)"
      local ANSWER; read -r ANSWER < /dev/tty || ANSWER=
      [ "$ANSWER" = yes ] || { echo "Skipped — nothing changed for $STACK."; return 3; }
    else
      echo "No terminal available to confirm. Re-run with --yes to override."; return 3
    fi
    mkdir -p .deploy-backup/$STAMP
    local f; for f in docker-compose.yml docker-compose.yaml latest.sh env.properties.example .env.example .env; do [ -f "$f" ] && cp -p "$f" .deploy-backup/$STAMP/; done
    if [ "$FORCE_ENV" = 1 ] && [ -f env.properties ]; then cp -p env.properties .deploy-backup/$STAMP/ && chmod 600 .deploy-backup/$STAMP/env.properties; echo "env.properties backed up to .deploy-backup/$STAMP/"; fi
  fi
  unpack_files   # defined per stack just before the call
  chmod +x ./*.sh 2>/dev/null || true
  echo "uploaded: $FILES"

  # runtime config: created from the examples once; kept afterwards unless --force-env
  if [ -f env.properties.example ] && { [ ! -f env.properties ] || [ "$FORCE_ENV" = 1 ]; }; then
    cp env.properties.example env.properties && echo "env.properties written from example — REVIEW IT"
  fi
  if [ -f .env.example ] && { [ ! -f .env ] || [ "$FORCE_ENV" = 1 ]; }; then cp .env.example .env; fi
  # env.properties keeps the server values (passwords, hosts) but must know every key
  # the stack now uses: append keys the example introduced (default), never overwrite.
  local line key
  if [ -f env.properties.example ] && [ -f env.properties ]; then
    while IFS= read -r line; do
      key=${line%%=*}
      case "$line" in ""|"#"*) continue ;; esac
      grep -qE "^${key}=" env.properties || { echo "$line" >> env.properties; echo "added $key to env.properties from the example (default) — REVIEW IT"; }
    done < env.properties.example
  fi
  load_env_properties
  touch .env
  if [ -f .env.example ]; then
    while IFS= read -r line; do
      key=${line%%=*}
      case "$line" in ""|"#"*) continue ;; esac
      grep -qE "^${key}=" .env || { echo "$line" >> .env; echo "added $key to .env from the example"; }
    done < .env.example
  fi
  if grep -qE "^DNS_SERVER=\s*$" .env; then
    local NS; NS=$(awk "/^nameserver/ {print \$2; exit}" /etc/resolv.conf 2>/dev/null || true)
    [ -n "$NS" ] && sed -i "s/^DNS_SERVER=.*/DNS_SERVER=$NS/" .env && echo "DNS_SERVER set to $NS from /etc/resolv.conf"
  fi
  if grep -qE "^$TAG_VAR=" .env; then sed -i "s/^$TAG_VAR=.*/$TAG_VAR=$BUILD/" .env; else echo "$TAG_VAR=$BUILD" >> .env; fi
  echo "--- .env"; cat .env

  # never assume a host port is free; the platform nginx pins these ports
  port_in_use() { (ss -tln 2>/dev/null || netstat -tln 2>/dev/null) | awk "{print \$4}" | grep -qE "[.:]$1\$"; }
  # a port is ours when the container publishing it (any state: a crash-looping one still holds the port
  # through docker-proxy) belongs to this compose project — the directory name, or COMPOSE_PROJECT_NAME in .env
  own_port() {
    local P; P=$( (grep -E "^COMPOSE_PROJECT_NAME=" .env 2>/dev/null || true) | tail -1 | cut -d= -f2)
    P=$(echo "${P:-$(basename "$PWD")}" | tr "[:upper:]" "[:lower:]" | sed "s/[^a-z0-9_-]//g")
    docker ps -a --filter "publish=$1" --format "{{.Label \"com.docker.compose.project\"}}" 2>/dev/null | grep -qx "$P"
  }
  local VAR CUR EXP NEW
  for VAR in $PORT_VARS; do
    CUR=$( (grep -E "^${VAR}=" env.properties 2>/dev/null || true) | tail -1 | cut -d= -f2 | tr -d "\r ")
    EXP=$( (grep -E "^${VAR}=" env.properties.example 2>/dev/null || true) | tail -1 | cut -d= -f2 | tr -d "\r ")
    if [ -n "$EXP" ] && [ "$CUR" != "$EXP" ]; then
      echo "${VAR}=${CUR:-unset} on this server differs from the platform port ${EXP} pinned by nginx — resetting to ${EXP}"
      if grep -qE "^${VAR}=" env.properties; then sed -i "s/^${VAR}=.*/${VAR}=${EXP}/" env.properties; else echo "${VAR}=${EXP}" >> env.properties; fi
      CUR=$EXP
    fi
    [ -z "$CUR" ] && continue
    if port_in_use "$CUR" && ! own_port "$CUR"; then
      if [ "$PORT_GUARD" = bump ]; then
        NEW=$CUR; while port_in_use "$NEW"; do NEW=$((NEW+1)); done
        echo "${VAR}=${CUR} is taken by another service on this host — using ${NEW}"
        sed -i "s/^${VAR}=.*/${VAR}=${NEW}/" env.properties
      else
        echo "ERROR: ${VAR}=${CUR} is already in use by another service on this host:"
        (ss -tlnp 2>/dev/null || netstat -tlnp 2>/dev/null) | grep -E "[.:]${CUR}[[:space:]]" || true
        docker ps -a --filter "publish=${CUR}" --format "       container {{.Names}} ({{.Image}}, {{.Status}}, compose project {{.Label \"com.docker.compose.project\"}})" 2>/dev/null || true
        echo "       The platform nginx vhost pins this port, so it cannot be changed silently."
        echo "       Free the port, or change ${VAR} in env.properties AND the nginx vhost, then re-run."
        return 4
      fi
    fi
  done
  [ -n "$PORT_VARS" ] && { echo "--- ports"; grep -E "^($(echo "$PORT_VARS" | tr " " "|"))=" env.properties || true; }

  [ "$NO_RUN" = 1 ] && return 0
  echo "--- registry route check (repairs a Docker bridge that hijacked the route to the registry)"
  bash "$REMOTE_ROOT/.deploy-tools/fix-registry-route.sh" "$REGISTRY_HOST" "$REMOTE_ROOT" "$DIR"
  echo "--- ./latest.sh (its log tail ends by itself after ${TAIL}s — no need to press anything)"
  timeout --foreground "${TAIL}s" ./latest.sh < /dev/null || true
  echo "--- containers"; docker compose ps
  if [ -n "$VERIFY_URL" ]; then
    # services take a while to boot (Spring ~20-30s, longer behind a gateway health check): poll, do not snapshot
    local CODE="" WAITED=0 LIMIT="${VERIFY_TIMEOUT:-150}"
    while :; do
      CODE=$(curl -sk -o /dev/null -w "%{http_code}" "$VERIFY_URL" || true)
      [ "$CODE" = 200 ] && break
      [ "$WAITED" -ge "$LIMIT" ] && break
      [ "$WAITED" -eq 0 ] && echo "waiting for $VERIFY_URL (up to ${LIMIT}s)..."
      sleep 5; WAITED=$((WAITED+5))
    done
    if [ "$CODE" = 200 ]; then echo "VERIFY OK: $VERIFY_URL -> 200 (after ${WAITED}s)"; else echo "VERIFY FAILED: $VERIFY_URL -> $CODE after ${LIMIT}s"; return 2; fi
  fi
  return 0
}
'

# ---- assemble the remote script ------------------------------------------------
REMOTE_SCRIPT="set -uo pipefail
FORCE_ENV=$FORCE_ENV; NO_RUN=$NO_RUN; ASSUME_YES=$ASSUME_YES; REMOTE_ROOT=\"$REMOTE_ROOT\"; REGISTRY_HOST=\"$REGISTRY_HOST\"
echo \"== deploy on \$(hostname) as \$(id -un): ${PLAN[*]}\"
FAILED=''; DONE=''
mkdir -p \"$REMOTE_ROOT/.deploy-tools\"
$(emit_file "$DEPLOY_DIR/remote/fix-registry-route.sh" "$REMOTE_ROOT/.deploy-tools/fix-registry-route.sh")
chmod +x \"$REMOTE_ROOT/.deploy-tools/\"*.sh 2>/dev/null || true
$REMOTE_FUNCS
"
for item in "${PLAN[@]}"; do
  stack="${item%%=*}"; build="${item#*=}"
  load_stack "$stack"
  if [[ -n "$EXEC_CMD" ]]; then
    REMOTE_SCRIPT+="mkdir -p \"$REMOTE_DIR\" && cd \"$REMOTE_DIR\" && echo \"== $stack in \$(pwd)\" && { $EXEC_CMD ; }
"
    continue
  fi
  UNPACK="unpack_files() {
"
  for spec in $FILES; do
    src="${spec%%:*}"; dst="${spec##*:}"; [[ "$spec" == *:* ]] || dst="$src"
    if [[ ! -f "$SRC_DIR/$src" ]]; then
      if [[ "$src" == *.example ]]; then echo "note: $stack: $src not in stack; skipping" >&2; continue; fi
      echo "ERROR: $src not found in $SRC_DIR" >&2; exit 1
    fi
    [[ "$dst" == */* ]] && UNPACK+="  mkdir -p \"$(dirname "$dst")\"
"
    UNPACK+="  $(emit_file "$SRC_DIR/$src" "$dst")
"
  done
  UNPACK+="}
"
  REMOTE_SCRIPT+="mkdir -p \"$REMOTE_DIR\"
$UNPACK
if ( deploy_stack \"$stack\" \"$REMOTE_DIR\" \"$TAG_VAR\" \"$build\" \"$PORT_VARS\" \"$PORT_GUARD\" \"$VERIFY_URL\" \"$LOG_TAIL_SECONDS\" \"$FILES\" )
then DONE=\"\$DONE $stack\"; else RC=\$?; case \$RC in 3) echo \"-- $stack skipped\";; *) echo \"-- $stack FAILED (exit \$RC)\";; esac; FAILED=\"\$FAILED $stack(\$RC)\"; fi
"
done
if [[ -z "$EXEC_CMD" ]]; then
  REMOTE_SCRIPT+="
echo; echo '=========================================='
echo \"deployed:\${DONE:- none}\"
[ -z \"\$FAILED\" ] && echo 'all stacks ok' || echo \"not deployed:\$FAILED\"
echo '=========================================='
[ -z \"\$FAILED\" ]
"
fi

# ---- run -----------------------------------------------------------------------
echo "=========================================="
if [[ -n "$MANIFEST" ]]; then echo " release $(basename "$MANIFEST" .env) -> $TARGET_HOST ($TARGET_ENV) via PAM"; else echo " $STACK -> $TARGET_HOST ($TARGET_ENV) via PAM"; fi
echo "=========================================="
for item in "${PLAN[@]}"; do echo "  ${item%%=*} = ${item#*=}"; done
echo "  remote root: $REMOTE_ROOT"
echo "  PAM login:   ssh -p $PAM_PROXY_PORT -l '$PAM_USER' $PAM_PROXY_HOST"
[[ -n "$EXEC_CMD" ]] && echo "  mode:        exec: $EXEC_CMD"
[[ $NO_RUN -eq 1 ]] && echo "  mode:        upload only"
[[ $FORCE_ENV -eq 1 ]] && echo "  WARNING:     --force-env will REPLACE env.properties on the server (passwords/secrets reset; backup kept)"
[[ $ASSUME_YES -eq 1 ]] && echo "  confirm:     skipped (--yes)"
echo "=========================================="

# PAM_DEPLOY_DUMP=<file> writes the full remote script (to test it locally).
if [[ -n "${PAM_DEPLOY_DUMP:-}" ]]; then printf '%s\n' "$REMOTE_SCRIPT" > "$PAM_DEPLOY_DUMP"; fi
if [[ $DRY_RUN -eq 1 ]]; then
  echo "$REMOTE_SCRIPT" | sed -E 's/(echo ")[A-Za-z0-9+\/=]{80,}(" \| base64)/\1<base64…>\2/'
  exit 0
fi

SSH_OPTS=(-p "$PAM_PROXY_PORT" -o StrictHostKeyChecking=accept-new)
# A tty lets the pre-flight confirmations read the answers on the server.
[[ $ASSUME_YES -eq 0 && -z "$EXEC_CMD" ]] && SSH_OPTS+=(-t)
[[ $VERBOSE -eq 1 ]] && SSH_OPTS+=(-vvv)

if command -v sshpass >/dev/null 2>&1 && [[ -n "${SERVER_PASSWORD:-}" ]]; then
  SSHPASS="$SERVER_PASSWORD" sshpass -e ssh "${SSH_OPTS[@]}" -l "$PAM_USER" "$PAM_PROXY_HOST" "$REMOTE_SCRIPT"
else
  echo "(enter the PAM password once at the prompt; install sshpass + set SERVER_PASSWORD to skip it)"
  ssh "${SSH_OPTS[@]}" -l "$PAM_USER" "$PAM_PROXY_HOST" "$REMOTE_SCRIPT"
fi
