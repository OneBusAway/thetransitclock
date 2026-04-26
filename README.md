# TransitClock

This is a fork of TheTransitClock, a [GTFS-RT Trip Updates](https://gtfs.org/documentation/realtime/feed-entities/trip-updates/) generation engine used by public transit agencies around the world, including in Minneapolis, MN where [the software was found to outperform proprietary alternatives](http://berrebi.net/wp-content/uploads/2020/01/trbws07_FileUploads_2020-AM-Presentations_3470_pdf_13557_P20-20421_2020-01-14-09-24-54.pdf).

[TheTransitClock](https://thetransitclock.github.io) was developed by Sean Óg Crudden and Simon J. Berrebi, Ph.D., itself a fork of [Swiftly Transitime](https://transitime.github.io/core/).

## About this repo

The complete core Java software for the TransitClock real-time transit information project. The purpose of the software is to use any type of real-time GPS data to generate useful public transportation information, namely a GTFS-RT Trip Updates feed.

The system is for both letting passengers know the status of their vehicles and helping agencies more effectively manage their systems. By providing a complete open-source system, agencies can have a cost-effective system and have full ownership of it.

## Modules

| Module | Role |
|---|---|
| `transitclock` | Core engine (Java). Houses the matchers, prediction generators, Hibernate entities, RMI servers, and every `main` class under `org.transitclock.applications` (`Core`, `GtfsFileProcessor`, `SchemaGenerator`, `CreateWebAgency`, `CreateAPIKey`, `RmiQuery`, …). Builds shaded JARs into `transitclock/target/`. |
| `transitclockApi` | JAX-RS REST API WAR. Talks to a running Core over RMI; produces the GTFS-RT TripUpdates output. |
| `transitclockWebapp` | User-facing web UI WAR. Consumes the REST API. |
| `transitclockQuickStart` | Single-process launcher bundling Core + API + Webapp. Convenient for local experimentation. Production deployments should run each tier as its own process — see the setup guide. |
| `transitclockBarefootClient`, `transitclockTraccarClient` | Thin clients for Barefoot map-matching and Traccar GPS, depended on by Core. |
| `transitclockIntegration`, `transitclockPipelineTests` | Opt-in test modules (see [docs/integration-tests.md](docs/integration-tests.md) and the profiles below). |
| `coverage-report` | JaCoCo aggregate report (no sources of its own). |

## Building

From the repo root:

```bash
mvn install -DskipTests
```

This produces:

- Shaded executable JARs in `transitclock/target/` (`Core.jar`, `GtfsFileProcessor.jar`, `SchemaGenerator.jar`, `CreateWebAgency.jar`, `CreateAPIKey.jar`, `RmiQuery.jar`, `UpdateTravelTimes.jar`, `ScheduleGenerator.jar`).
- `transitclockApi/target/api.war` and `transitclockWebapp/target/web.war` for Tomcat.

There is no lint step.

## Production setup

End-to-end runbook for standing up Core against a GTFS-realtime feed, with the
API and webapp on Tomcat: **[docs/setup.md](docs/setup.md)**.

It covers:

- The external data you need (GTFS static, GTFS-RT VehiclePositions, Postgres, JDK, Tomcat) and where to source it.
- Database provisioning and DDL generation.
- Config files (a working sample is in [`docs/examples/`](docs/examples)).
- Running every shaded JAR with the right system properties.
- Deploying `api.war` and `web.war` and minting an API key.

## Running tests

- Default unit tests across all modules: `mvn verify` (not `mvn test` — see below).
- Single module: `mvn -pl transitclock test`
- Single class: `mvn -pl transitclock test -Dtest=TestAPIKeyManager`

`mvn test` on the full reactor fails because `transitclockQuickStart` binds `maven-dependency-plugin:copy` to `generate-resources` to pull the `transitclockApi` WAR into its resources, but the `test` phase never packages that WAR (MDEP-187: "Artifact has not been packaged yet"). Use `mvn verify` / `mvn package` / `mvn install` to exercise all tests, or scope to a single module with `-pl`, or skip QuickStart with `mvn test -pl '!transitclockQuickStart'`.

Two additional test suites are opt-in via Maven profiles and excluded from the default build:

- **Pipeline tests** (`transitclockPipelineTests`) — boot a real Core against an in-memory HSQL database populated with a small WMATA GTFS fixture, then exercise matcher / generator behavior end-to-end. Run with:
  ```
  mvn -pl transitclockPipelineTests -am -P include-pipeline-tests test
  ```
  First run takes ~30s while `transitclockCore` compiles; subsequent runs are ~3s. CI runs this on every PR as a separate step after the unit-test build.
- **Integration tests** (`transitclockIntegration`) — full AVL-CSV replay runs, heavier than pipeline tests. Run with:
  ```
  mvn install -P include-integration-tests
  ```

To run **everything** (unit + pipeline + integration) in one go:
```
mvn install -P run-all-tests
```

### Code coverage

JaCoCo generates coverage reports as part of the Maven `verify` phase.

- Per-module HTML reports: `<module>/target/site/jacoco/index.html`
- Aggregate report across Core + thin clients: `coverage-report/target/site/jacoco-aggregate/index.html`
- Regenerate just the aggregate (fastest): `mvn verify -pl coverage-report -am`
