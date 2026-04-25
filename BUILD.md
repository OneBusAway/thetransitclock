# Build

TheTransitClock is a Maven multi-module project. From the repo root:

```bash
mvn install -DskipTests
```

This builds every module and produces:

- Shaded executable JARs in `transitclock/target/` — one per `main` class under `org.transitclock.applications`. The most relevant for production are `Core.jar`, `GtfsFileProcessor.jar`, `SchemaGenerator.jar`, `CreateWebAgency.jar`, and `CreateAPIKey.jar`.
- `transitclockApi/target/api.war` — the REST API.
- `transitclockWebapp/target/web.war` — the user-facing web UI.

Java 17 and Maven 3.6+ are required. There is no lint step.

For the full production runbook (database provisioning, DDL generation,
config files, running each JAR, deploying the WARs to Tomcat), see
[docs/setup.md](docs/setup.md).

For test invocation and coverage, see the README.
