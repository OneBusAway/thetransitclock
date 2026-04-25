# `transitclockApi` — REST API WAR

JAX-RS API backing TheTransitClock's user-facing webapp and any third-party
integrations. Produces a GTFS-RT TripUpdates feed from the live predictions
that a running `Core` JVM exposes over RMI.

```bash
cd transitclockApi
mvn install -DskipTests
```

This produces `target/api.war`, suitable for deployment into Tomcat 9.
Tomcat 10+ will refuse the WAR — it's still `javax.servlet`, not the
Jakarta `jakarta.servlet` namespace Tomcat 10 requires.

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
registry from the right database. Without it, the registry lookup runs with
`dbName=null`, the JDBC URL ends in `/null`, and the connection fails — the
API will serve "no agencies" until you set it.

## API keys

REST calls authenticate via a key supplied as a URL segment. Mint one with
`CreateAPIKey.jar` (see `transitclock/README.md`); the row lives in the `web`
database.

## Sample request

```bash
curl "http://<host>:<port>/api/v1/key/<API_KEY>/agency/<agencyId>/command/gtfs-rt/tripUpdates?format=human"
```

Drop `?format=human` for the binary protobuf feed. The full set of resources
(predictions, vehicles, route config, GTFS-RT TripUpdates, SIRI, commands,
cache queries) is defined by the JAX-RS classes under
`org.transitclock.api.rootResources`; the user-facing resources are rooted
at `/api/v1/key/{key}/agency/{agencyId}/...`.

## Full setup runbook

See [../docs/setup.md](../docs/setup.md) for the end-to-end deployment flow
(database provisioning, GTFS import, Core launch, WAR deployment).
