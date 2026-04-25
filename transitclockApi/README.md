# `transitclockApi` — REST API WAR

JAX-RS API backing TheTransitClock's user-facing webapp and any third-party
integrations. Produces a GTFS-RT TripUpdates feed from the live predictions
that a running `Core` JVM exposes over RMI.

```bash
cd transitclockApi
mvn install -DskipTests
```

This produces `target/api.war`, suitable for deployment into Tomcat 9.

## Runtime requirements

The API talks to two things:

1. A running `Core` JVM, over RMI on port **2099** (primary) and **2098** (secondary).
2. The **`web`** database, where it reads the `WebAgency` registry (to find each agency's RMI host) and the `ApiKey` table (to authenticate REST callers).

So Tomcat needs the same DB credentials and config file Core uses. Set
`CATALINA_OPTS` before starting Tomcat:

```bash
CATALINA_OPTS="\
  -Dtransitclock.configFiles=/etc/transitclock/transitclockConfig.xml \
  -Dtransitclock.hibernate.configFile=/etc/transitclock/postgres_hibernate.cfg.xml \
  -Dtransitclock.db.dbType=postgresql \
  -Dtransitclock.db.dbHost=localhost \
  -Dtransitclock.db.dbName=web \
  -Dtransitclock.db.dbUserName=transitclock \
  -Dtransitclock.db.dbPassword=changeme"
```

`-Dtransitclock.db.dbName=web` is what makes the API read the `WebAgency`
registry from the right database. Without it, lookups fall back to using the
agency id as the database name.

## API keys

REST calls authenticate via a key supplied as a URL segment. Mint one with
`CreateAPIKey.jar` (see `transitclock/README.md`); the row lives in the `web`
database.

## Sample request

```bash
curl "http://<host>:<port>/api/v1/key/<API_KEY>/agency/<agencyId>/command/gtfs-rt/tripUpdates?format=human"
```

Drop `?format=human` for the binary protobuf feed. Routes such as
`/v1/transitime/key/...` from older deployments are still wired up — see the
JAX-RS root resources under `org.transitclock.api.rootResources`.

## Full setup runbook

See [../docs/setup.md](../docs/setup.md) for the end-to-end deployment flow
(database provisioning, GTFS import, Core launch, WAR deployment).
