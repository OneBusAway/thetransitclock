# `transitclock` — core engine

The core module produces the long-running `Core` JVM plus the operational tooling
around it. Every `main` class under `org.transitclock.applications` is wired as
its own shaded executable JAR via `transitclock/pom.xml`. After
`mvn install -DskipTests` from the repo root, the JARs land in
`transitclock/target/`.

For a complete production runbook (databases, configs, deployment), see
[../docs/setup.md](../docs/setup.md). This README is a per-tool reference.

## Tools

| JAR | Main class | Use |
|---|---|---|
| `Core.jar` | `org.transitclock.applications.Core` | The engine. AVL ingestion, matching, prediction generation, RMI servers. Long-running. |
| `SchemaGenerator.jar` | `org.transitclock.applications.SchemaGenerator` | Emit DDL from the Hibernate-annotated entities. **Run via `mvn exec:java`** — the shaded jar has a one-jar/Hibernate classloader collision. |
| `GtfsFileProcessor.jar` | `org.transitclock.applications.GtfsFileProcessor` | Import a GTFS static feed into the database. One-shot. |
| `CreateWebAgency.jar` | `org.transitclock.applications.CreateWebAgency` | Register an agency in the `web` database so the API can route RMI lookups. |
| `CreateAPIKey.jar` | `org.transitclock.applications.CreateAPIKey` | Mint a REST API key. |
| `RmiQuery.jar` | `org.transitclock.applications.RmiQuery` | CLI client for a running Core's RMI servers. |
| `UpdateTravelTimes.jar` | `org.transitclock.applications.UpdateTravelTimes` | Offline travel-time recompute over historical AD data. |
| `ScheduleGenerator.jar` | `org.transitclock.applications.ScheduleGenerator` | Offline schedule generation. |

## `SchemaGenerator`

Generates DDL for both schemas the deployment needs:

- `org.transitclock.db.structs` — core tables (predictions, AVL, AD, vehicles, …)
- `org.transitclock.db.webstructs` — web layer (`WebAgency`, `ApiKey`)

The `mvn exec:java` form sidesteps the shaded-jar classloader issue:

```bash
cd transitclock
mvn exec:java -Dexec.mainClass=org.transitclock.applications.SchemaGenerator \
              -Dexec.args="-o target -p org.transitclock.db.structs"
mvn exec:java -Dexec.mainClass=org.transitclock.applications.SchemaGenerator \
              -Dexec.args="-o target -p org.transitclock.db.webstructs"
```

Output files (one per supported dialect): `ddl_postgres_org_transitclock_db_structs.sql`, `ddl_mysql_…`, `ddl_oracle_…`, plus the `_webstructs` variants. Apply the dialect/schema pair appropriate for your database with `psql -U transitclock -d <dbname> -f <ddl-file>` (or the equivalent for your DB) — see [docs/setup.md §4](../docs/setup.md) for a worked example.

```
usage:
    -o,--outputDirectory <arg>        Directory to write SQL files into.
    -p,--hibernatePackagePath <arg>   Java package containing the Hibernate
                                      annotated entity classes.
```

## `GtfsFileProcessor`

Imports a GTFS static feed into the database, computes initial travel times, and
optionally promotes the resulting revision into `ActiveRevisions`.

```bash
java -Xmx2g \
    -Dtransitclock.core.agencyId=02 \
    -Dtransitclock.db.dbType=postgresql \
    -Dtransitclock.db.dbHost=localhost \
    -Dtransitclock.db.dbUserName=transitclock \
    -Dtransitclock.db.dbPassword=changeme \
    -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
    -jar target/GtfsFileProcessor.jar \
    -c /etc/transitclock/transitclockConfig.xml \
    -gtfsZipFileName /tmp/agency-gtfs.zip \
    -storeNewRevs
```

Key flags (full list with `-h`):

```
-c, --config <configFile>            Config file. Required for DB connection.
-gtfsZipFileName <zipFileName>       Local GTFS zip. Will be unzipped + processed.
-gtfsUrl <url>                       GTFS zip URL. Will be downloaded + processed.
-gtfsDirectoryName <dirName>         Pre-unzipped GTFS directory.
-storeNewRevs                        Promote the new rev in ActiveRevisions.
-supplementDir <dirName>             Supplemental GTFS files merged in.
-maxTravelTimeSegmentLength <m>      Default 200m.
-maxSpeedKph <kph>                   Default 97.
-maxStopToPathDistance <m>           If a stop is farther than this from its
                                     path it triggers a warning + path edit.
-trimPathBeforeFirstStopOfTrip       Trim shape head before first stop.
-defaultWaitTimeAtStopMsec <ms>      Default 10000.
-n, --notes <notes>                  Free-text description of the import.
-regexReplaceFile <fileName>         Spelling/case fix-ups for GTFS strings.
```

## `CreateWebAgency`

Inserts a `WebAgency` row into the **`web`** database (the database name is
hardcoded in the application's `main`). Positional args, in this exact order:

```
agencyId  hostName  dbName  dbType  dbHost  dbUserName  dbPassword
```

`hostName` is where Core is reachable over RMI from the API host.

```bash
java -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
     -Dtransitclock.db.dbType=postgresql \
     -Dtransitclock.db.dbHost=localhost \
     -Dtransitclock.db.dbUserName=transitclock \
     -Dtransitclock.db.dbPassword=changeme \
     -jar target/CreateWebAgency.jar \
     02 localhost 02 postgresql localhost transitclock changeme
```

## `CreateAPIKey`

```bash
java -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
     -Dtransitclock.db.dbType=postgresql \
     -Dtransitclock.db.dbHost=localhost \
     -Dtransitclock.db.dbName=web \
     -Dtransitclock.db.dbUserName=transitclock \
     -Dtransitclock.db.dbPassword=changeme \
     -jar target/CreateAPIKey.jar \
     -c /etc/transitclock/transitclockConfig.xml \
     -n "<application name>" \
     -u "<application URL>" \
     -e "<contact email>" \
     -p "<contact phone>" \
     -d "<description>"
```

The minted key is printed to stdout. All `-n -u -e -p -d` flags are required.

`transitclock.db.dbName=web` must be set somewhere: `ApiKeyManager`
resolves its DB name from `DbSetupConfig.getDbName()` when its singleton
class-inits, and without it the JDBC URL becomes `…/null` and the command
crashes. The `-D` flag above is the easy form; equivalently you can put
`<db><dbName>web</dbName></db>` in the config file passed via `-c`.

## `Core`

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
    -jar target/Core.jar
```

System properties Core honours:

| Property | Default | Notes |
|---|---|---|
| `transitclock.core.agencyId` | — | Required. Used as default DB name and log subdirectory. |
| `transitclock.configFiles` | — | Required in practice. Semicolon-separated list of XML/properties config files. |
| `transitclock.hibernate.configFile` | `hsql_hibernate.cfg.xml` | Filesystem path or classpath name. |
| `transitclock.db.dbType` | `mysql` | **Set to `postgresql` if you're on Postgres.** |
| `transitclock.db.dbHost` | (cfg.xml) | Override DB host. |
| `transitclock.db.dbName` | agencyId | Override DB name. |
| `transitclock.db.dbUserName` | (cfg.xml) | Override DB user. |
| `transitclock.db.dbPassword` | (cfg.xml) | Override DB password. |
| `transitclock.rmi.rmiPort` | `2099` | Primary RMI port. |
| `transitclock.rmi.secondaryRmiPort` | `2098` | Secondary RMI port (data streams). |
| `transitclock.logging.dir` | `/Logs` | Base directory for `logback.xml`. |
| `transitclock.core.pidDirectory` | `/usr/local/transitclock/` | PID file location. |

Logs are written to `${transitclock.logging.dir}/${agencyId}/core/YYYY/MM/DD/*.log.gz`.

## `RmiQuery`

Smoke-test a running Core:

```bash
# List all vehicles.
java -jar target/RmiQuery.jar -a 02 -c vehicles

# Predictions for a stop, or for a lat/lon (1500 m radius).
java -jar target/RmiQuery.jar -a 02 -c preds -s <stopId>
java -jar target/RmiQuery.jar -a 02 -c preds -lat 47.6 -lon -122.3
```

Valid `-c` values: `vehicles`, `preds`, `routeConfig`, `config`,
`activeBlocks`, `resetVehicle`. `preds` requires either `-s` or **both**
`-lat` and `-lon`; called without one it prints
`Error: must specify stop(s) to get predictions.` to stderr and exits.
