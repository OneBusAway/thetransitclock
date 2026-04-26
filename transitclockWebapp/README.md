# `transitclockWebapp` — web UI WAR

User-facing UI on top of `transitclockApi`. Built and deployed the same way:

```bash
cd transitclockWebapp
mvn install -DskipTests
```

Produces `target/web.war`. Tomcat 9 only — the WAR is `javax.servlet`, not
`jakarta.servlet`, so Tomcat 10+ will refuse to load it.

## Runtime requirements

The webapp has two distinct request paths:

1. **Page rendering.** The JSPs themselves don't open DB sessions or call the
   API in-process. Each page emits JavaScript that calls the REST API over
   HTTP from the browser, using an API key the JSP reads from a JVM system
   property.
2. **Reports.** The classes under `org.transitclock.reports`
   (`RoutePerformanceQuery`, `ScheduleAdherenceController`,
   `PredictionAccuracyQuery`, …) hit the database directly via
   `HibernateUtils.getSession()` / `GenericQuery.getConnection(...)`. So the
   webapp tier really does need working DB credentials — any request that
   reaches a report page will 500 without them.

Practical implications:

- The webapp shares Tomcat with `api.war`, so it inherits the API's
  `CATALINA_OPTS` (config file, Hibernate config, DB credentials,
  `transitclock.db.dbName=web`). The reports tier uses those credentials
  directly; don't skimp on them just because the page-rendering JSPs don't.
- It additionally needs `-Dtransitclock.apikey=<key>` set in `CATALINA_OPTS`.
  `template/includes.jsp` reads it via
  `System.getProperty("transitclock.apikey")` and bakes it into a JavaScript
  `apiKey` variable, which `apiUrlPrefix` then concatenates into the
  `/api/v1/key/<key>/agency/<id>/...` URL segment. Without the property the
  JSP renders `apiKey="null"` and every API call 401s. Mint the key with
  `CreateAPIKey.jar` (see [`transitclock/README.md`](../transitclock/README.md))
  and reuse the same key here.

See [`transitclockApi/README.md`](../transitclockApi/README.md) and the full
runbook in [`docs/setup.md`](../docs/setup.md).

## Full setup runbook

See [../docs/setup.md](../docs/setup.md).
