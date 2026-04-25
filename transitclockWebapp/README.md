# `transitclockWebapp` — web UI WAR

User-facing UI on top of `transitclockApi`. Built and deployed the same way:

```bash
cd transitclockWebapp
mvn install -DskipTests
```

Produces `target/web.war`.

## Runtime requirements

The webapp shares Tomcat with `api.war` and consumes the REST API in-process.
It needs the same `CATALINA_OPTS` as the API (config file, Hibernate config,
DB credentials, `transitclock.db.dbName=web`) — see
[`transitclockApi/README.md`](../transitclockApi/README.md) and the full
runbook in [`docs/setup.md`](../docs/setup.md).

## Full setup runbook

See [../docs/setup.md](../docs/setup.md).
