# Production setup (command line)

This is the power-user runbook for standing up a production TheTransitClock
deployment from a clean machine. The flow is: build the shaded JARs, point them
at a Postgres database, import a GTFS static feed, then run `Core.jar` against
a GTFS-realtime vehicle-positions URL. The REST API and web UI are deployed as
WARs into Tomcat and reach Core over RMI.

This guide ignores `transitclockQuickStart` entirely. QuickStart bundles all
three tiers into a single launcher; everything below runs each tier as its own
process, which is what you want in production.

> **Docker shortcut.** The repo ships a `docker-compose.yml` plus
> `docker/Dockerfile` that bundle Postgres 17, the build, Core, and Tomcat 9
> + JDK 21 into one stack. If you're standing up a fresh deployment, jump to
> [§0 Containerized deployment](#0-containerized-deployment) — every step
> below is wired up there. The detailed §1–§10 flow is the bare-metal
> reference for when Docker isn't an option (or when you need to debug what
> a particular step is actually doing).

## 0. Containerized deployment

The included `docker-compose.yml` runs each tier as its own container on a
single docker network. The build is a multi-stage `docker/Dockerfile`:

- **builder** — runs `mvn install -DskipTests` against the full reactor.
- **tools** — `FROM builder`, plus `psql`. Used via `docker compose run
  --rm tools <cmd>` for SchemaGenerator, GtfsFileProcessor, CreateWebAgency,
  CreateAPIKey, RmiQuery — i.e. every one-shot admin command in §4–§7.
- **core-runtime** — JDK 21 + `Core.jar` (copied from builder).
- **tomcat-runtime** — Tomcat 9 + JDK 21 + `api.war`/`web.war` (copied
  from builder).

Per-deployment secrets live in a top-level `.env` (gitignored), and
the rest of the per-deployment files live in `.deploy/` (also gitignored):

```text
.env                                     (cp from .env.example;
                                          TRANSITCLOCK_DB_PASSWORD now,
                                          TRANSITCLOCK_APIKEY after step 7)
.deploy/
├── conf/
│   ├── transitclockConfig.xml           (copy of docs/examples/, with
│   │                                     your AVL feed URL filled in)
│   └── postgres_hibernate.cfg.xml       (copy of docs/examples/)
├── ddl/                                  (SchemaGenerator output — applied
│                                         to the `web` and per-agency DBs)
├── logs/                                 (mounted into Core)
└── <your-gtfs>.zip
```

The flow then is just the §1–§9 flow run inside containers:

```bash
# 0. Set up the top-level .env. TRANSITCLOCK_DB_PASSWORD now,
#    TRANSITCLOCK_APIKEY after step 7. compose substitutes both into the
#    services that need them and refuses to start a service whose
#    required value is unset (with a one-line message pointing here).
cp .env.example .env
$EDITOR .env

# 1+2. Build all images (builder runs once, runtime stages copy from it).
docker compose build

# 3. Start Postgres. docker/postgres-init.sh creates the `web` database
#    and the `transitclock` role (using TRANSITCLOCK_DB_PASSWORD from
#    .env); POSTGRES_DB creates the per-agency database.
docker compose up -d db

# 4. Generate DDL via mvn exec:java in the tools container, then apply it
#    to the right databases via psql (also in the tools container).
#
#    The `bash -euo pipefail -c` form makes a failure anywhere in the chain
#    abort the whole step (otherwise psql/mvn errors after the first &&
#    can be silently lost). `psql -v ON_ERROR_STOP=1` does the same on
#    psql's side — without it, psql continues past CREATE TABLE failures
#    and exits 0, which can leave half-applied schemas.
docker compose run --rm tools bash -euo pipefail -c "
  cd /workspace/transitclock
  mvn -q exec:java \
    -Dexec.mainClass=org.transitclock.applications.SchemaGenerator \
    -Dexec.args='-o /deploy/ddl -p org.transitclock.db.structs'
  mvn -q exec:java \
    -Dexec.mainClass=org.transitclock.applications.SchemaGenerator \
    -Dexec.args='-o /deploy/ddl -p org.transitclock.db.webstructs'
  test -s /deploy/ddl/ddl_postgres_org_transitclock_db_structs.sql
  test -s /deploy/ddl/ddl_postgres_org_transitclock_db_webstructs.sql
  grep -c 'CREATE TABLE' /deploy/ddl/ddl_postgres_org_transitclock_db_structs.sql"

docker compose run --rm tools bash -euo pipefail -c "
  psql -v ON_ERROR_STOP=1 -h db -U transitclock -d <agency-db> \
       -f /deploy/ddl/ddl_postgres_org_transitclock_db_structs.sql
  psql -v ON_ERROR_STOP=1 -h db -U transitclock -d web \
       -f /deploy/ddl/ddl_postgres_org_transitclock_db_webstructs.sql"

# 5. Drop your transitclockConfig.xml + postgres_hibernate.cfg.xml into
#    .deploy/conf/. They get bind-mounted at /etc/transitclock/ inside
#    every container.

# 6. Import GTFS.
docker compose run --rm tools bash -euo pipefail -c "java -Xmx2g \
  -Dtransitclock.core.agencyId=<id> \
  -Dtransitclock.db.dbType=postgresql \
  -Dtransitclock.db.dbHost=db \
  -Dtransitclock.db.dbName=<agency-db> \
  -Dtransitclock.db.dbUserName=transitclock \
  -Dtransitclock.db.dbPassword=changeme \
  -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
  -jar /workspace/transitclock/target/GtfsFileProcessor.jar \
  -c /etc/transitclock/transitclockConfig.xml \
  -gtfsZipFileName /deploy/<your-gtfs>.zip \
  -storeNewRevs"

# 7. CreateWebAgency takes the DB target from positional args, so it
#    doesn't need -Dtransitclock.db.dbName=web. CreateAPIKey *does* need
#    it (see the comment on the next block). hostName MUST be `core` (the
#    docker network DNS name of the core service) so the API resolves it
#    inside the network.
docker compose run --rm tools bash -euo pipefail -c "java \
  -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
  -Dtransitclock.db.dbType=postgresql -Dtransitclock.db.dbHost=db \
  -Dtransitclock.db.dbUserName=transitclock -Dtransitclock.db.dbPassword=changeme \
  -jar /workspace/transitclock/target/CreateWebAgency.jar \
  <id> core <agency-db> postgresql db transitclock changeme"

# Mint the API key. Note -Dtransitclock.db.dbName=web — see §7 below
# for why CreateAPIKey crashes without it.
docker compose run --rm tools bash -euo pipefail -c "java \
  -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
  -Dtransitclock.db.dbType=postgresql -Dtransitclock.db.dbHost=db \
  -Dtransitclock.db.dbName=web \
  -Dtransitclock.db.dbUserName=transitclock -Dtransitclock.db.dbPassword=changeme \
  -jar /workspace/transitclock/target/CreateAPIKey.jar \
  -c /etc/transitclock/transitclockConfig.xml \
  -n 'ops' -u 'http://localhost' -e 'ops@example.org' -p '555-0100' -d 'Ops key'"

# Add the printed key to .env:
#   TRANSITCLOCK_APIKEY=<the key>
# (docker compose substitutes it into tomcat's CATALINA_OPTS at boot.
# Without it tomcat refuses to start with a helpful error message.)

# 8+9. Start Core and Tomcat. Both pull DB credentials and the API key
# from .env via docker-compose.yml's interpolation.
docker compose up -d core tomcat

# Smoke test:
curl "http://localhost:8080/api/v1/key/<API_KEY>/agency/<id>/command/gtfs-rt/tripUpdates?format=human"
```

If that curl returns a non-empty TripUpdates protobuf within a minute or
two of starting Core, **you're done**. The §1–§9 sections below are the
bare-metal equivalent of what §0 just walked through and aren't required
reading; jump to **§10 Updating GTFS** for the day-2 workflow.

If the smoke test returns empty / errors, the most common causes are:

- **Empty protobuf:** Core hasn't completed a full AVL polling cycle yet
  (default 5s, but matchers need 1–2 cycles before predictions land).
  Tail `docker compose logs core | grep -i prediction` and
  `.deploy/logs/<agencyId>/core/.../avl.log.gz`.
- **`apiKey="null"` / 401 on every page:** `transitclock.apikey` in
  `tomcat.env` doesn't match the key minted in step 7. See §10.
- **"no agencies" / RMI timeouts:** the WebAgency row's `hostName`
  doesn't match the docker network DNS name (must be `core`), or
  `transitclock.db.dbName=web` is missing from `CATALINA_OPTS`. See §10.
- **No tables / DDL incomplete:** confirm with
  `docker compose exec db psql -U transitclock -d <agency-db> -c '\dt' | wc -l`.
  Should be 30+.

A couple of Docker-specific gotchas worth knowing:

- **Don't use `--mount=type=cache` for `~/.m2` in the builder stage.**
  BuildKit cache mounts live outside the image, so the downstream `tools`
  stage would inherit an empty `/root/.m2` and admin commands like
  `mvn exec:java` would refuse to resolve the inter-module dependencies.
  The committed Dockerfile drops the cache mount and lets the deps land in
  image layers; the runtime images don't change, only the build cache does.
- **Java RMI needs `-Djava.rmi.server.hostname=core`** (or whatever your
  Core service is called on the docker network). Without it Core advertises
  whatever its container IP happens to be, and Tomcat can't reach it on a
  restart. The `core` service in `docker-compose.yml` already passes this.
- **Don't combine `ENTRYPOINT ["java"]` in the Dockerfile with a shell
  `command:` in compose** — Java will try to load `sh` as a main class.
  Set `entrypoint: ["sh", "-c"]` in compose and put the full `java …`
  invocation in `command:` instead, which is what we do.
- **Non-root runtime users on Linux hosts:** `core-runtime` runs as
  UID `10001` and `tomcat-runtime` as UID `10002`, so the bind-mounted
  `.deploy/logs/` directory has to be writable by UID `10001`. macOS
  Docker Desktop's VirtioFS makes this transparent; on a Linux host
  you'll want `sudo chown -R 10001:10001 .deploy/logs` once before
  the first `docker compose up -d core`.
- **Custom HTTP headers on the AVL feed:** `PollUrlAvlModule` only
  supports HTTP basic auth via `transitclock.avl.authenticationUser` /
  `authenticationPassword`. If your provider needs a different header
  (WMATA: `api_key: <key>`) and accepts the same value as a query-string
  parameter, embed it in `gtfsRealtimeFeedURI` — that path requires no
  code changes, but **the secret will leak**:
    - `PollUrlAvlModule.getAndProcessData()` logs the full URL at INFO
      on every poll, so the key ends up in
      `${transitclock.logging.dir}/<agencyId>/core/.../avl.log.gz` and
      `core.log.gz`, rotated and gzipped but unencrypted.
    - `RmiQuery -c config` returns every config value (including
      `transitclock.avl.gtfsRealtimeFeedURI`) over RMI without any
      authentication, so anyone who can reach Core's RMI ports can read
      the key.
    - Pasting a `core.log` snippet into a GitHub issue or chat
      guarantees a leak.

  Concrete mitigations, in order of effort:
    1. Strip the parameter before sharing logs:
       `gunzip -c core.log.gz | sed -E 's/api_key=[^& ]+/api_key=REDACTED/g'`.
    2. `chmod 0700` `${transitclock.logging.dir}` and own it as the Core
       service user only.
    3. Don't expose the RMI ports (2099/2098) past localhost / the
       docker network.
    4. Subclass `PollUrlAvlModule` with a ~10-line override of
       `setRequestHeaders(URLConnection)` that reads the secret from a
       `StringConfigValue` you mark as `secret=true`, then register the
       subclass in `transitclock.modules.optionalModulesList`. This is
       the only path that keeps the secret out of `core.log` entirely.

## 1. What you need to source externally

| Input | Purpose | Where to get it |
|---|---|---|
| **GTFS static feed** (`.zip`) | Routes, stops, trips, schedule, shapes — the static skeleton TheTransitClock matches AVL onto. | Your transit agency's open-data portal, [Mobility Database](https://database.mobilitydata.org/), or [transit.land](https://www.transit.land/feeds). Must be GTFS, not GTFS-Flex. |
| **GTFS-realtime VehiclePositions feed** (URL) | Live AVL stream. Must be a [VehiclePositions](https://gtfs.org/documentation/realtime/feed-entities/vehicle-positions/) feed (not TripUpdates / Alerts). | Same agency or aggregator. The URL is polled every 5 s by default. HTTP basic auth is supported via `transitclock.avl.authenticationUser` / `…Password`; arbitrary custom headers (e.g. WMATA's `api_key:`) require subclassing `PollUrlAvlModule` or embedding the secret in the URL — see the AVL-feed gotcha further down. |
| **PostgreSQL 16+** | Persists config, GTFS, AVL, predictions, arrivals/departures, web agency registry, and API keys. | Any standard install. The shipped `docker-compose.yml` pins Postgres 17. MySQL also works (`-Dtransitclock.db.dbType=mysql`); HSQLDB is for tests only. |
| **JDK 21** | Runtime. The WARs are compiled with `--release 21` so the deployed JRE must be ≥21. | Any LTS distribution. |
| **Tomcat 9** | Hosts `api.war` and `web.war`. Not required if you only need the engine + RMI. **Tomcat 10+ won't work** — both WARs are still on `javax.servlet`, and Tomcat 10 switched to the Jakarta `jakarta.servlet` namespace. | Apache Tomcat distribution. |

Optional: a writable log directory (default `/Logs`, override with
`-Dtransitclock.logging.dir=...`), and a writable PID directory (default
`/usr/local/transitclock/`, override with `-Dtransitclock.core.pidDirectory=...`).

## 2. Build the shaded JARs

From the repo root:

```bash
mvn install -DskipTests
```

This drops the following self-contained executable JARs into
`transitclock/target/`:

| JAR | Main class | Role |
|---|---|---|
| `Core.jar` | `org.transitclock.applications.Core` | Long-running engine: ingests AVL, runs matchers, generates predictions, exposes RMI servers. |
| `SchemaGenerator.jar` | `org.transitclock.applications.SchemaGenerator` | Emits DDL from the Hibernate-annotated entity classes. |
| `GtfsFileProcessor.jar` | `org.transitclock.applications.GtfsFileProcessor` | One-shot GTFS static importer. |
| `CreateWebAgency.jar` | `org.transitclock.applications.CreateWebAgency` | Registers an agency in the `web` database so the API can route RMI lookups. |
| `CreateAPIKey.jar` | `org.transitclock.applications.CreateAPIKey` | Mints a REST API key. |
| `RmiQuery.jar` | `org.transitclock.applications.RmiQuery` | CLI for poking a running Core's RMI servers. |
| `UpdateTravelTimes.jar` | `org.transitclock.applications.UpdateTravelTimes` | Offline travel-time recompute over historical AD data. |
| `ScheduleGenerator.jar` | `org.transitclock.applications.ScheduleGenerator` | Offline schedule generation. |

Tomcat-deployable WARs land at:

- `transitclockApi/target/api.war`
- `transitclockWebapp/target/web.war`

> `SchemaGenerator.jar` has a known classloader issue when invoked as a shaded
> JAR (one-jar / `META-INF` collisions for Hibernate's `service-loader`-style
> resources). Use `mvn exec:java` instead — see step 4 below. The other shaded
> JARs run fine via `java -jar`.

## 3. Provision the databases

Create **two** Postgres databases. The split is convention plus a write-side
hardcode: `CreateWebAgency.main` writes its row into a database literally
named `web` (`String webAgencyDbName = "web";` in
`org.transitclock.applications.CreateWebAgency`), and the rest of the runbook
points the API and `CreateAPIKey` at the same name with
`-Dtransitclock.db.dbName=web`. Nothing else in the runtime forces the name —
in principle you could pick a different one if you also fix
`CreateWebAgency` — but staying with `web` is much less work. Per-agency
core data lives in a database whose name defaults to the agency ID and can
be overridden with `-Dtransitclock.db.dbName=...`.

```bash
# Example: agency id "02". This form assumes a local Postgres install
# with a `postgres` Unix user (Debian/Ubuntu/Homebrew defaults). On a
# managed service (RDS, Cloud SQL, …) run the same statements through
# whatever client you normally use, as the master/admin role.
sudo -u postgres psql <<'SQL'
CREATE USER transitclock WITH PASSWORD 'changeme';
CREATE DATABASE "02"  OWNER transitclock;   -- core data (per agency)
CREATE DATABASE web   OWNER transitclock;   -- WebAgency, ApiKey
SQL
```

This example uses a single Postgres role for both databases for simplicity;
multi-tenant setups (one Core JVM per agency, separate ops teams) often want a
distinct owner per database — substitute the second `CREATE DATABASE … OWNER`
accordingly and adjust the credentials passed to each tier in later steps.

Naming the database after the agency id is the path of least resistance — Core
falls back to using `transitclock.core.agencyId` as the database name when
`transitclock.db.dbName` is unset.

## 4. Generate and apply the schema

Two Hibernate packages, one DDL each:

- `org.transitclock.db.structs` → core tables (predictions, AVL, AD, vehicles, …)
- `org.transitclock.db.webstructs` → web layer (`WebAgency`, `ApiKey`)

Run `SchemaGenerator` via `mvn exec:java` from `transitclock/`:

```bash
cd transitclock
mvn exec:java \
  -Dexec.mainClass=org.transitclock.applications.SchemaGenerator \
  -Dexec.args="-o target -p org.transitclock.db.structs"

mvn exec:java \
  -Dexec.mainClass=org.transitclock.applications.SchemaGenerator \
  -Dexec.args="-o target -p org.transitclock.db.webstructs"
```

This writes `ddl_postgres_org_transitclock_db_structs.sql`,
`ddl_postgres_org_transitclock_db_webstructs.sql` (and `_mysql_`, `_oracle_`
variants) into `transitclock/target/`.

Apply the right pair to the right database:

```bash
psql -U transitclock -d 02  -f target/ddl_postgres_org_transitclock_db_structs.sql
psql -U transitclock -d web -f target/ddl_postgres_org_transitclock_db_webstructs.sql
```

## 5. Wire up configuration files

You need two files. Put them somewhere stable like `/etc/transitclock/`.

### 5a. `transitclockConfig.xml`

This is the runtime config TheTransitClock parses on startup. The XML root tag
becomes the property prefix — **the root must be `<transitclock>`**. A root tag
of `<transitime>` (as appears in some legacy samples shipped with the repo) is
silently ignored, because every typed `ConfigValue` in the codebase is
registered under the `transitclock.*` namespace.

A minimal working example is checked in at
[`docs/examples/transitclockConfig.xml`](examples/transitclockConfig.xml).

The keys you must set:

| XML path | Property name | Meaning |
|---|---|---|
| `<transitclock><core><agencyId>` | `transitclock.core.agencyId` | Agency identifier; also default DB name. |
| `<transitclock><modules><optionalModulesList>` | `transitclock.modules.optionalModulesList` | Semicolon-separated list of module classes to start (separator must be `;` — comma is not accepted). For GTFS-RT use `org.transitclock.avl.GtfsRealtimeModule`. |
| `<transitclock><avl><gtfsRealtimeFeedURI>` | `transitclock.avl.gtfsRealtimeFeedURI` | URL (or comma-separated URLs) of the VehiclePositions feed. |
| `<transitclock><hibernate><configFile>` | `transitclock.hibernate.configFile` | Path or classpath name of the Hibernate XML (next file). |

Optional knobs worth knowing:

- `transitclock.avl.feedPollingRateSecs` — default `5`.
- `transitclock.avl.feedTimeoutInMSecs` — default `10000`.
- `transitclock.avl.authenticationUser` / `transitclock.avl.authenticationPassword` — HTTP basic auth on the AVL feed.
- `transitclock.rmi.rmiPort` — default `2099`.
- `transitclock.rmi.secondaryRmiPort` — default `2098`.

### 5b. `postgres_hibernate.cfg.xml`

The Hibernate config. Connection URL/user/password can live here, or be
overridden by `-Dtransitclock.db.dbHost=`, `-Dtransitclock.db.dbName=`,
`-Dtransitclock.db.dbUserName=`, `-Dtransitclock.db.dbPassword=` system
properties. A minimal example is at
[`docs/examples/postgres_hibernate.cfg.xml`](examples/postgres_hibernate.cfg.xml).

Hibernate's lookup order (`HibernateUtils.getSessionFactory` /
`getConfiguration`): filesystem first, then classpath. Pass an absolute path
on the command line and you don't have to think about classpath.

> The default value of `transitclock.db.dbType` is **`mysql`**. If you're on
> Postgres you must set `-Dtransitclock.db.dbType=postgresql` (this is what
> Core uses to construct the JDBC URL when `hibernate.connection.url` isn't
> set in the cfg.xml).

## 6. Import the GTFS static feed

```bash
java -Xmx2g \
  -Dtransitclock.core.agencyId=02 \
  -Dtransitclock.db.dbType=postgresql \
  -Dtransitclock.db.dbHost=localhost \
  -Dtransitclock.db.dbUserName=transitclock \
  -Dtransitclock.db.dbPassword=changeme \
  -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
  -jar transitclock/target/GtfsFileProcessor.jar \
  -c /etc/transitclock/transitclockConfig.xml \
  -gtfsZipFileName /tmp/agency-gtfs.zip \
  -storeNewRevs
```

Critical flags:

- `-c` — points at `transitclockConfig.xml` so the processor can connect to the DB.
- `-gtfsZipFileName` — local zip; alternatively `-gtfsUrl <url>` to fetch, or `-gtfsDirectoryName <dir>` if already unzipped.
- `-storeNewRevs` — promotes the imported revision in `ActiveRevisions` so Core picks it up. Skip this and Core will keep using the previous active rev.

Useful tuning flags (full list in `transitclock/README.md`):
`-maxTravelTimeSegmentLength`, `-maxSpeedKph`, `-maxStopToPathDistance`,
`-trimPathBeforeFirstStopOfTrip`.

## 7. Register the web agency and an API key

Both commands target the `web` database, but they pick the target differently
and one of those differences trips people up. `CreateWebAgency` ignores
`transitclock.db.dbName` and writes to a database literally named `web`
(hardcoded in its `main`). `CreateAPIKey` reads `transitclock.db.dbName` at
class-init time and crashes with a `…/null` JDBC URL if you omit it. So the
first command below has no `dbName` flag and the second one does — that's
not a typo.

`CreateWebAgency` writes a row into the hardcoded `web` database. **Positional
args**, in order:

```
agencyId  hostName  dbName  dbType  dbHost  dbUserName  dbPassword
```

`hostName` is the host where Core will be reachable over RMI (this is what the
API will look up to know where to send RMI calls). For a single-box deployment,
use `localhost`.

```bash
java \
  -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
  -Dtransitclock.db.dbType=postgresql \
  -Dtransitclock.db.dbHost=localhost \
  -Dtransitclock.db.dbUserName=transitclock \
  -Dtransitclock.db.dbPassword=changeme \
  -jar transitclock/target/CreateWebAgency.jar \
  02 localhost 02 postgresql localhost transitclock changeme
```

Mint an API key. Note `-Dtransitclock.db.dbName=web` — `ApiKeyManager`
seeds its database name from `DbSetupConfig.getDbName()` when its singleton
class-inits, and without that property the JDBC URL ends in `/null` and the
command crashes. The flag is the easy form; equivalently you can put
`<db><dbName>web</dbName></db>` in the config file passed via `-c`, since
`ConfigFileReader.processConfig` runs before `ApiKeyManager` is touched:

```bash
java \
  -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
  -Dtransitclock.db.dbType=postgresql \
  -Dtransitclock.db.dbHost=localhost \
  -Dtransitclock.db.dbName=web \
  -Dtransitclock.db.dbUserName=transitclock \
  -Dtransitclock.db.dbPassword=changeme \
  -jar transitclock/target/CreateAPIKey.jar \
  -c /etc/transitclock/transitclockConfig.xml \
  -n "ops"  -u "http://localhost"  -e "ops@example.org"  -p "555-0100"  -d "Ops key"
```

The generated key is printed to stdout. Save it — REST clients pass it as a
URL segment, and the webapp needs it as a JVM property (see step 9).

## 8. Launch Core

```bash
java -Xmx4g -server \
  -Dtransitclock.core.agencyId=02 \
  -Dtransitclock.configFiles=/etc/transitclock/transitclockConfig.xml \
  -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
  -Dtransitclock.db.dbType=postgresql \
  -Dtransitclock.db.dbHost=localhost \
  -Dtransitclock.db.dbUserName=transitclock \
  -Dtransitclock.db.dbPassword=changeme \
  -Dtransitclock.logging.dir=/var/log/transitclock \
  -Dtransitclock.core.pidDirectory=/var/run/transitclock \
  -jar transitclock/target/Core.jar
```

What happens on startup:

1. Loads `transitclockConfig.xml` (semicolon-separated paths in `transitclock.configFiles` are supported).
2. Reads the active GTFS revision from `ActiveRevisions` and snapshots it into `DbConfig`.
3. Starts every class listed in `transitclock.modules.optionalModulesList` as a thread. `GtfsRealtimeModule` begins polling the feed URL.
4. Binds RMI servers (`PredictionsServer`, `VehiclesServer`, `ConfigServer`, `CommandsServer`, `CacheQueryServer`) on port **2099**, with secondary comms on **2098**. Open these ports through any firewall between Core and your Tomcat host.
5. Begins async batched writes to Postgres via `DataDbLogger`.

Logs land at `${transitclock.logging.dir}/${agencyId}/core/YYYY/MM/DD/*.log.gz`
(see the rolling-file appenders in `transitclock/src/main/resources/logback.xml`,
which is bundled into `Core.jar` and picked up automatically). Watch
`avl.log.gz` and `prediction.log.gz` to confirm AVL is being ingested and
predictions are being generated. Add `-Dlogback.configurationFile=/path/to/logback.xml`
to the Core invocation if you need a different layout (additional appenders,
JSON output, syslog, etc.).

Sanity-check from a second shell. `RmiQuery` resolves the Core RMI host
through the `WebAgency` table, so the easy form is to pass
`-Dtransitclock.rmi.rmiHost=localhost` and skip the DB lookup:

```bash
RMI_OPTS="-Dtransitclock.rmi.rmiHost=localhost"

# List all vehicles Core knows about.
java $RMI_OPTS -jar transitclock/target/RmiQuery.jar -a 02 -c vehicles

# Predictions for a specific stop. Pick any stop_id from the GTFS
# stops.txt you imported in step 6 (or fish one out with `-c routeConfig`).
java $RMI_OPTS -jar transitclock/target/RmiQuery.jar -a 02 -c preds -s <stopId>

# Predictions for everything within 1500 m of a lat/lon.
java $RMI_OPTS -jar transitclock/target/RmiQuery.jar -a 02 -c preds -lat 47.6 -lon -122.3
```

Without `transitclock.rmi.rmiHost` set, RmiQuery needs the same DB plumbing
as `CreateAPIKey` in step 7 (`-Dtransitclock.configFiles=...`,
`-Dtransitclock.hibernate.configFile=...`, `-Dtransitclock.db.dbType=postgresql`,
`-Dtransitclock.db.dbName=web`, plus user/password) — the host comes out
of the `WebAgency` row you wrote in that step.

Valid `-c` values are `vehicles`, `preds`, `routeConfig`, `config`,
`activeBlocks`, and `resetVehicle`. `preds` requires either `-s` or **both**
`-lat` and `-lon`; called without one it prints
`Error: must specify stop(s) to get predictions.` to stderr and exits.

## 9. Deploy the API and web WARs

Both WARs target Tomcat 9 (`javax.servlet`); **Tomcat 10+ won't work** because
those releases switched to the Jakarta `jakarta.servlet` namespace. Drop the
WARs into Tomcat's `webapps/`.

The API needs `transitclock.configFiles` and the same DB-related properties as
Core, because it reads the `WebAgency` and `ApiKey` tables out of the `web`
database to validate keys and route RMI lookups. The webapp shares the same
JVM and reuses those DB credentials for the report tier
(`org.transitclock.reports`, which queries the per-agency database directly
via Hibernate); the page-rendering JSPs themselves don't open a DB session,
but the report endpoints will 500 if the credentials are missing. The webapp
additionally needs `transitclock.apikey` set: every page renders JavaScript
that reads the key out of `System.getProperty("transitclock.apikey")` into
a JS `apiKey` variable and uses it as a URL segment for API calls. Without
it the JSP ships `apiKey="null"` and every request 401s. Use the key minted
in step 7.

`/etc/default/tomcat9` (or wherever you set `CATALINA_OPTS`):

```bash
CATALINA_OPTS="\
  -Dtransitclock.configFiles=/etc/transitclock/transitclockConfig.xml \
  -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
  -Dtransitclock.db.dbType=postgresql \
  -Dtransitclock.db.dbHost=localhost \
  -Dtransitclock.db.dbName=web \
  -Dtransitclock.db.dbUserName=transitclock \
  -Dtransitclock.db.dbPassword=changeme \
  -Dtransitclock.apikey=<API_KEY_FROM_STEP_7>"
```

> Setting `transitclock.db.dbName=web` is what makes the API read the
> `WebAgency` registry from the right database. Without it,
> `WebAgency.getWebAgencyDbName()` returns `null` (via
> `DbSetupConfig.getDbName()`), and that `null` gets passed straight through
> to `HibernateUtils.getSessionFactory(null, …)`. The agency-id fallback
> there only fires if the *parameter* is non-null — so `dbName` stays
> `null`, the JDBC URL is constructed as `jdbc:postgresql://<host>/null`,
> and the lookup fails. The API will then return "no agencies" until you
> set the property and redeploy.

Deploy:

```bash
sudo cp transitclockApi/target/api.war        /var/lib/tomcat9/webapps/
sudo cp transitclockWebapp/target/web.war     /var/lib/tomcat9/webapps/
sudo systemctl restart tomcat9
```

Smoke-test the GTFS-RT TripUpdates endpoint the API generates from your live
predictions:

```bash
curl "http://localhost:8080/api/v1/key/<API_KEY>/agency/02/command/gtfs-rt/tripUpdates?format=human"
```

The `format=human` query produces protobuf-as-text; drop it for the binary
GTFS-RT feed.

If the response is empty, Core hasn't generated any predictions yet — give
it at least one full polling cycle of the AVL feed and check
`prediction.log.gz` and `avl.log.gz` before assuming the API is misconfigured.

## 10. Updating GTFS

When the schedule changes, re-run `GtfsFileProcessor` with the new zip and
`-storeNewRevs`. Old revisions stay in the database (revisions are tracked by
`configRev` and `travelTimesRev`, with `ActiveRevisions` pointing at the live
pair), so Core continues serving until you flip the active rev. Restart Core
after the new rev is active.

## Troubleshooting

| Symptom | Likely cause |
|---|---|
| `Could not load in hibernate config file ...` on startup | `transitclock.hibernate.configFile` value isn't on the filesystem or classpath. Use an absolute path. |
| Core starts but no AVL is processed | `optionalModulesList` is empty, the feed URL is wrong, or `transitclock.avl.shouldProcessAvl=false`. Check `avl.log.gz`. |
| API returns "no agencies" / RMI timeouts | `WebAgency` row missing or pointing at a wrong host/port; `transitclock.db.dbName=web` not set in `CATALINA_OPTS`. |
| Predictions table never grows | No active GTFS revision (forgot `-storeNewRevs`), or the AVL feed has no vehicles matching the GTFS routes/blocks. |
| "Could not contact RMI" between API and Core | Ports 2099 and 2098 blocked, or `hostName` passed to `CreateWebAgency` doesn't resolve from the API host. |
| Settings in your config file have no effect | Root tag is `<transitime>` (legacy). Change to `<transitclock>`; every typed `ConfigValue` is registered under `transitclock.*`. |
| `CreateAPIKey` crashes / JDBC URL ends in `/null` | Forgot `-Dtransitclock.db.dbName=web`. `ApiKeyManager` resolves its DB name at class-init from `DbSetupConfig.getDbName()`; without the override the URL becomes `…/null` and the connection fails. |
