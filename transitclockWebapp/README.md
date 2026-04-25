# `transitclockWebapp` — web UI WAR

User-facing UI on top of `transitclockApi`. Built and deployed the same way:

```bash
cd transitclockWebapp
mvn install -DskipTests
```

Produces `target/web.war`. Tomcat 9 only — the WAR is `javax.servlet`, not
`jakarta.servlet`, so Tomcat 10+ will refuse to load it.

## Runtime requirements

The webapp is plain JSP + JavaScript; it does **not** open a DB session, and
it does **not** call the API in-process. Each rendered page emits JavaScript
that calls the REST API over HTTP from the browser, using an API key the JSP
reads from a JVM system property.

Practical implications:

- The webapp shares Tomcat with `api.war`, so it inherits the API's
  `CATALINA_OPTS` (config file, Hibernate config, DB credentials,
  `transitclock.db.dbName=web`). It doesn't actually use the DB credentials
  itself, but they have to be set for the API tier in the same JVM.
- It additionally needs `-Dtransitclock.apikey=<key>` set in `CATALINA_OPTS`.
  `template/includes.jsp` reads it via
  `System.getProperty("transitclock.apikey")` and bakes it into the
  `apiUrlPrefix` JavaScript variable. Without it the JSP renders
  `apiKey="null"` and every API call 401s. Mint the key with
  `CreateAPIKey.jar` (see [`transitclock/README.md`](../transitclock/README.md))
  and reuse the same key here.

See [`transitclockApi/README.md`](../transitclockApi/README.md) and the full
runbook in [`docs/setup.md`](../docs/setup.md).

## Full setup runbook

See [../docs/setup.md](../docs/setup.md).
