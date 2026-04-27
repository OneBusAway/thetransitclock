# Quickstart (docker compose)

Get a local TransitClock deployment — Postgres, Core, REST API, web UI — running
against a real agency feed in about 15 minutes. This is the happy path. For
what each step actually does, the per-flag rationale, or troubleshooting, see
[`setup.md`](setup.md).

> **Just want it running?** The repo ships [`quickstart.sh`](../quickstart.sh)
> at the root, which automates every step below end-to-end:
>
> ```bash
> ./quickstart.sh --gtfs-zip /path/to/your-gtfs.zip \
>                 --avl-url 'https://example.org/gtfs-realtime/vehiclePositions'
> ```
>
> It generates a random Postgres password if one isn't already set, drops in
> the example config files, builds the images, runs every admin command,
> mints the API key and writes it back to `.env`, and starts Core + Tomcat.
> Each step is guarded by a sentinel in `.deploy/.state/`, so re-running
> resumes from the first incomplete step. The walkthrough below remains the
> reference for what the script is doing under the hood (and is the path to
> follow when you need to deviate).

The shipped `docker-compose.yml` pins:

- `agencyId` = `1`
- per-agency DB name = `wmata`
- core RMI hostname = `core`

Use those values verbatim in the steps below. (To run a different agency id /
DB name, edit `docker-compose.yml` first — `core.environment.JAVA_OPTS` and
`db.environment.POSTGRES_DB` — then come back.)

## Prerequisites

- Docker Desktop (or Docker Engine + compose v2)
- A GTFS static zip for your agency
- A GTFS-realtime **VehiclePositions** feed URL (not TripUpdates / Alerts)

## 1. Configure secrets and per-deployment files

```bash
cp .env.example .env
$EDITOR .env                   # set TRANSITCLOCK_DB_PASSWORD
                               # leave TRANSITCLOCK_APIKEY blank for now

mkdir -p .deploy/conf .deploy/ddl .deploy/logs
cp docs/examples/transitclockConfig.xml      .deploy/conf/
cp docs/examples/postgres_hibernate.cfg.xml  .deploy/conf/
cp /path/to/your-gtfs.zip                    .deploy/gtfs.zip
```

Edit `.deploy/conf/transitclockConfig.xml`:

- `<agencyId>` → `1`
- `<gtfsRealtimeFeedURI>` → your VehiclePositions URL

Leave `postgres_hibernate.cfg.xml` alone — connection details come from
`docker-compose.yml`'s `JAVA_OPTS` / `CATALINA_OPTS`.

## 2. Build images and start Postgres

```bash
docker compose build
docker compose up -d db
```

The init script creates the `web` and `wmata` databases and the
`transitclock` role with your `.env` password.

## 3. Generate and apply the schema

```bash
docker compose run --rm tools bash -euo pipefail -c '
  cd /workspace/transitclock
  mvn -q exec:java \
    -Dexec.mainClass=org.transitclock.applications.SchemaGenerator \
    -Dexec.args="-o /deploy/ddl -p org.transitclock.db.structs"
  mvn -q exec:java \
    -Dexec.mainClass=org.transitclock.applications.SchemaGenerator \
    -Dexec.args="-o /deploy/ddl -p org.transitclock.db.webstructs"
  psql -v ON_ERROR_STOP=1 -h db -U transitclock -d wmata \
       -f /deploy/ddl/ddl_postgres_org_transitclock_db_structs.sql
  psql -v ON_ERROR_STOP=1 -h db -U transitclock -d web \
       -f /deploy/ddl/ddl_postgres_org_transitclock_db_webstructs.sql'
```

## 4. Import GTFS

```bash
docker compose run --rm tools bash -euo pipefail -c '
  java -Xmx2g \
    -Dtransitclock.core.agencyId=1 \
    -Dtransitclock.db.dbType=postgresql \
    -Dtransitclock.db.dbHost=db \
    -Dtransitclock.db.dbName=wmata \
    -Dtransitclock.db.dbUserName=transitclock \
    -Dtransitclock.db.dbPassword="$TRANSITCLOCK_DB_PASSWORD" \
    -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
    -jar /workspace/transitclock/target/GtfsFileProcessor.jar \
    -c /etc/transitclock/transitclockConfig.xml \
    -gtfsZipFileName /deploy/gtfs.zip \
    -storeNewRevs'
```

`-storeNewRevs` makes this revision the active one. Without it Core won't
pick up the import.

## 5. Register the web agency and mint an API key

```bash
# CreateWebAgency takes positional args: agencyId hostName dbName dbType dbHost dbUser dbPass.
# hostName MUST be `core` so the API resolves Core over the docker network.
docker compose run --rm tools bash -euo pipefail -c '
  java \
    -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
    -Dtransitclock.db.dbType=postgresql -Dtransitclock.db.dbHost=db \
    -Dtransitclock.db.dbUserName=transitclock \
    -Dtransitclock.db.dbPassword="$TRANSITCLOCK_DB_PASSWORD" \
    -jar /workspace/transitclock/target/CreateWebAgency.jar \
    1 core wmata postgresql db transitclock "$TRANSITCLOCK_DB_PASSWORD"'

# CreateAPIKey reads dbName at class-init, so the -Dtransitclock.db.dbName=web
# flag is required — without it the JDBC URL ends in /null and crashes.
docker compose run --rm tools bash -euo pipefail -c '
  java \
    -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
    -Dtransitclock.db.dbType=postgresql -Dtransitclock.db.dbHost=db \
    -Dtransitclock.db.dbName=web \
    -Dtransitclock.db.dbUserName=transitclock \
    -Dtransitclock.db.dbPassword="$TRANSITCLOCK_DB_PASSWORD" \
    -jar /workspace/transitclock/target/CreateAPIKey.jar \
    -c /etc/transitclock/transitclockConfig.xml \
    -n "ops" -u "http://localhost" -e "ops@example.org" -p "555-0100" -d "Ops key"'
```

The second command prints the new key. Add it to `.env`:

```bash
TRANSITCLOCK_APIKEY=<the key from CreateAPIKey>
```

(Tomcat refuses to start without this — the JSPs hardcode it into every
page.)

## 6. Start Core and Tomcat

```bash
docker compose up -d core tomcat
```

Compose waits for Core's RMI registry to bind on 2099 before starting Tomcat,
so this is one command.

## 7. Smoke test

```bash
curl "http://localhost:8080/api/v1/key/<API_KEY>/agency/1/command/gtfs-rt/tripUpdates?format=human"
```

A non-empty TripUpdates protobuf within ~30 seconds means it's working. The
web UI is at <http://localhost:8080/web/>.

If the response is empty, give Core one or two AVL polling cycles (default 5s
each) and tail the logs:

```bash
docker compose logs -f core
ls .deploy/logs/1/core/$(date +%Y/%m/%d)/
```

## Updating GTFS later

Re-run step 4 with the new zip and `-storeNewRevs`, then restart Core:

```bash
docker compose restart core
```

Old revisions stay in the DB; `ActiveRevisions` flips to the new pair.

## When something breaks

The four most common failures are documented inline in
[`setup.md` §0](setup.md#0-containerized-deployment) (right after the smoke
test) and the broader troubleshooting table at the end of that document
covers the rest — including the Hibernate 6 / Jakarta upgrade regressions
to check for if a partial cherry-pick reintroduces them.
