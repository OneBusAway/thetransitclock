#!/usr/bin/env bash
#
# quickstart.sh — automate the docker compose flow from docs/quickstart.md.
#
# Usage:
#   ./quickstart.sh --gtfs-zip PATH --avl-url URL [--db-password PASS]
#
# Each step is guarded by a sentinel file in .deploy/.state/, so re-running
# resumes from where it left off. To force a clean slate:
#   docker compose down -v && rm -rf .deploy

set -euo pipefail

# ── arg parsing ─────────────────────────────────────────────────────────────
GTFS_ZIP=""
AVL_URL=""
DB_PASSWORD=""
AGENCY_ID="1"          # must match docker-compose.yml's JAVA_OPTS
AGENCY_DB="wmata"      # must match docker-compose.yml's POSTGRES_DB

usage() {
  sed -n '3,12p' "$0" | sed 's/^# \{0,1\}//'
  exit "${1:-0}"
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --gtfs-zip)    GTFS_ZIP="$2"; shift 2 ;;
    --avl-url)     AVL_URL="$2"; shift 2 ;;
    --db-password) DB_PASSWORD="$2"; shift 2 ;;
    -h|--help)     usage 0 ;;
    *)             echo "unknown flag: $1" >&2; usage 1 ;;
  esac
done

[[ -n "$GTFS_ZIP" ]] || { echo "--gtfs-zip is required" >&2; usage 1; }
[[ -n "$AVL_URL"  ]] || { echo "--avl-url is required"  >&2; usage 1; }
[[ -f "$GTFS_ZIP" ]] || { echo "gtfs zip not found: $GTFS_ZIP" >&2; exit 1; }

# ── helpers ─────────────────────────────────────────────────────────────────
STATE_DIR=".deploy/.state"
log()  { printf '\n\033[1;34m==> %s\033[0m\n' "$*"; }
done_step() { touch "$STATE_DIR/$1"; }
needs_step() { [[ ! -f "$STATE_DIR/$1" ]]; }

require() {
  command -v "$1" >/dev/null 2>&1 || { echo "missing prerequisite: $1" >&2; exit 1; }
}

# ── 0. prerequisites ────────────────────────────────────────────────────────
require docker
docker compose version >/dev/null 2>&1 || {
  echo "docker compose v2 required (got '$(docker --version)')" >&2; exit 1;
}

# ── 1. .env ─────────────────────────────────────────────────────────────────
log "Bootstrapping .env"
if [[ ! -f .env ]]; then
  cp .env.example .env
fi

# Resolve DB password: explicit flag overrides .env. If neither has it,
# bail — we won't make up a value that the user can't reproduce later.
current_pw=$(grep -E '^TRANSITCLOCK_DB_PASSWORD=' .env | cut -d= -f2- || true)
if [[ -n "$DB_PASSWORD" ]]; then
  new_pw="$DB_PASSWORD"
  # sed -i is portable across BSD (macOS) and GNU when given a backup suffix.
  sed -i.bak "s|^TRANSITCLOCK_DB_PASSWORD=.*|TRANSITCLOCK_DB_PASSWORD=$new_pw|" .env
  rm -f .env.bak
elif [[ -n "$current_pw" ]]; then
  : # use whatever's in .env
else
  echo "TRANSITCLOCK_DB_PASSWORD is empty in .env. Set it (or pass --db-password)." >&2
  exit 1
fi

# ── 2. .deploy/ skeleton + per-deployment files ─────────────────────────────
log "Preparing .deploy/"
mkdir -p .deploy/conf .deploy/ddl .deploy/logs "$STATE_DIR"

# Don't overwrite user-customized configs on re-run.
[[ -f .deploy/conf/transitclockConfig.xml     ]] || cp docs/examples/transitclockConfig.xml     .deploy/conf/
[[ -f .deploy/conf/postgres_hibernate.cfg.xml ]] || cp docs/examples/postgres_hibernate.cfg.xml .deploy/conf/

# Patch the freshly-copied config exactly once. The marker survives so user
# edits aren't stomped by a re-run.
if needs_step config-patched; then
  sed -i.bak "s|<agencyId>02</agencyId>|<agencyId>${AGENCY_ID}</agencyId>|" .deploy/conf/transitclockConfig.xml
  sed -i.bak "s|<gtfsRealtimeFeedURI>[^<]*</gtfsRealtimeFeedURI>|<gtfsRealtimeFeedURI>${AVL_URL}</gtfsRealtimeFeedURI>|" .deploy/conf/transitclockConfig.xml
  rm -f .deploy/conf/transitclockConfig.xml.bak
  done_step config-patched
fi

cp "$GTFS_ZIP" .deploy/gtfs.zip

# Export so $TRANSITCLOCK_DB_PASSWORD reaches `docker compose run` invocations
# below. (Compose itself reads .env directly for ${VAR} substitution; this
# export is for shell-side use inside the bash -c blocks.)
set -a; source .env; set +a

# ── 3. build images ─────────────────────────────────────────────────────────
if needs_step images-built; then
  log "Building images (first build can take 5–10 minutes)"
  docker compose build
  done_step images-built
fi

# ── 4. start postgres ───────────────────────────────────────────────────────
log "Starting Postgres"
docker compose up -d db

log "Waiting for Postgres to report healthy"
for _ in $(seq 1 60); do
  status=$(docker inspect -f '{{.State.Health.Status}}' transitclock-db 2>/dev/null || echo "starting")
  [[ "$status" == "healthy" ]] && break
  sleep 2
done
[[ "$status" == "healthy" ]] || { echo "Postgres never became healthy" >&2; exit 1; }

# ── 5. schema ───────────────────────────────────────────────────────────────
if needs_step schema-applied; then
  log "Generating and applying DDL"
  docker compose run --rm tools bash -euo pipefail -c '
    cd /workspace/transitclock
    mvn -q exec:java \
      -Dexec.mainClass=org.transitclock.applications.SchemaGenerator \
      -Dexec.args="-o /deploy/ddl -p org.transitclock.db.structs"
    mvn -q exec:java \
      -Dexec.mainClass=org.transitclock.applications.SchemaGenerator \
      -Dexec.args="-o /deploy/ddl -p org.transitclock.db.webstructs"
    psql -v ON_ERROR_STOP=1 -h db -U transitclock -d '"$AGENCY_DB"' \
         -f /deploy/ddl/ddl_postgres_org_transitclock_db_structs.sql
    psql -v ON_ERROR_STOP=1 -h db -U transitclock -d web \
         -f /deploy/ddl/ddl_postgres_org_transitclock_db_webstructs.sql
  '
  done_step schema-applied
fi

# ── 6. import GTFS ──────────────────────────────────────────────────────────
if needs_step gtfs-imported; then
  log "Importing GTFS (this can take a few minutes for a large feed)"
  docker compose run --rm tools bash -euo pipefail -c "
    java -Xmx2g \
      -Dtransitclock.core.agencyId=${AGENCY_ID} \
      -Dtransitclock.db.dbType=postgresql \
      -Dtransitclock.db.dbHost=db \
      -Dtransitclock.db.dbName=${AGENCY_DB} \
      -Dtransitclock.db.dbUserName=transitclock \
      -Dtransitclock.db.dbPassword=\"\$TRANSITCLOCK_DB_PASSWORD\" \
      -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
      -jar /workspace/transitclock/target/GtfsFileProcessor.jar \
      -c /etc/transitclock/transitclockConfig.xml \
      -gtfsZipFileName /deploy/gtfs.zip \
      -storeNewRevs
  "
  done_step gtfs-imported
fi

# ── 7. register web agency ──────────────────────────────────────────────────
if needs_step web-agency-created; then
  log "Registering web agency"
  docker compose run --rm tools bash -euo pipefail -c "
    java \
      -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
      -Dtransitclock.db.dbType=postgresql -Dtransitclock.db.dbHost=db \
      -Dtransitclock.db.dbUserName=transitclock \
      -Dtransitclock.db.dbPassword=\"\$TRANSITCLOCK_DB_PASSWORD\" \
      -jar /workspace/transitclock/target/CreateWebAgency.jar \
      ${AGENCY_ID} core ${AGENCY_DB} postgresql db transitclock \"\$TRANSITCLOCK_DB_PASSWORD\"
  "
  done_step web-agency-created
fi

# ── 8. mint API key ─────────────────────────────────────────────────────────
existing_key=$(grep -E '^TRANSITCLOCK_APIKEY=' .env | cut -d= -f2-)
if needs_step api-key-minted && [[ -z "$existing_key" ]]; then
  log "Minting REST API key"
  # CreateAPIKey prints "ApiKey [applicationName=ops, key=<value>, ...]"
  # as the last line of stdout. We compose run captures stdout fine.
  raw=$(docker compose run --rm -T tools bash -euo pipefail -c "
    java \
      -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
      -Dtransitclock.db.dbType=postgresql -Dtransitclock.db.dbHost=db \
      -Dtransitclock.db.dbName=web \
      -Dtransitclock.db.dbUserName=transitclock \
      -Dtransitclock.db.dbPassword=\"\$TRANSITCLOCK_DB_PASSWORD\" \
      -jar /workspace/transitclock/target/CreateAPIKey.jar \
      -c /etc/transitclock/transitclockConfig.xml \
      -n 'ops' -u 'http://localhost' -e 'ops@example.org' -p '555-0100' -d 'Ops key'
  ")
  api_key=$(printf '%s\n' "$raw" | sed -nE 's/.*key=([^,]+),.*/\1/p' | tail -n1)
  if [[ -z "$api_key" ]]; then
    echo "Could not parse API key from CreateAPIKey output:" >&2
    printf '%s\n' "$raw" >&2
    exit 1
  fi
  sed -i.bak "s|^TRANSITCLOCK_APIKEY=.*|TRANSITCLOCK_APIKEY=$api_key|" .env
  rm -f .env.bak
  done_step api-key-minted
  set -a; source .env; set +a
fi

api_key=$(grep -E '^TRANSITCLOCK_APIKEY=' .env | cut -d= -f2-)

# ── 9. start core + tomcat ──────────────────────────────────────────────────
log "Starting Core and Tomcat"
docker compose up -d core tomcat

# ── 10. smoke test ──────────────────────────────────────────────────────────
log "Done. Stack is up."
cat <<EOF

  Web UI:  http://localhost:8080/web/
  API:     http://localhost:8080/api/v1/key/$api_key/agency/$AGENCY_ID/

  Smoke test (give Core a few AVL polling cycles before you expect data):

    curl "http://localhost:8080/api/v1/key/$api_key/agency/$AGENCY_ID/command/gtfs-rt/tripUpdates?format=human"

  Tail Core logs:
    docker compose logs -f core

  Re-run any failed step: just run this script again. To start over:
    docker compose down -v && rm -rf .deploy

EOF
