# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

TheTransitClock (formerly Transitime) is a Java real-time transit prediction and monitoring system. It ingests AVL (Automatic Vehicle Location) data, matches vehicles to GTFS schedules/routes, and generates arrival/departure predictions. Java 17, Maven multi-module, Hibernate 5.5 over PostgreSQL (MySQL and HSQLDB also wired up).

## Build and test

Built from the repository root as a Maven multi-module project.

- Build everything without tests: `mvn install -DskipTests`
- Run all unit tests across the reactor: `mvn verify` (**not** `mvn test` — see below)
- Run a single module's tests: `mvn -pl transitclock test`
- Run a single test class: `mvn -pl transitclock test -Dtest=TestAPIKeyManager`
- Integration tests live in the `transitclockIntegration` module and are **excluded by default** via the `skip-integration-tests` profile. Enable with: `mvn install -P include-integration-tests`
- Pipeline tests live in the `transitclockPipelineTests` module and are **excluded by default** — opt-in via the `include-pipeline-tests` profile. They boot a real Core against an in-memory HSQL database populated with a small WMATA GTFS fixture and exercise matcher/generator behavior end-to-end (lighter than the full AVL-trace runs in `transitclockIntegration`). Enable with: `mvn -pl transitclockPipelineTests -am -P include-pipeline-tests test`. CI runs this as a separate step after `mvn verify`.
- Run **everything** (unit + pipeline + integration) in one invocation: `mvn install -P run-all-tests`. The `run-all-tests` profile activates both extra modules so the reactor picks them up alongside the default build.

`mvn test` on the full reactor fails: `transitclockQuickStart` binds `maven-dependency-plugin:copy` to `generate-resources` to pull the `transitclockApi` WAR into its resources, but the `test` phase never packages that WAR (MDEP-187: "Artifact has not been packaged yet"). Use `mvn verify` / `mvn package` / `mvn install` to exercise all tests, or scope to one module with `-pl`, or skip QuickStart: `mvn test -pl '!transitclockQuickStart'`.
- Shaded executable JARs are emitted into `transitclock/target/` (e.g. `Core.jar`, `GtfsFileProcessor.jar`, `SchemaGenerator.jar`, `CreateWebAgency.jar`, `CreateAPIKey.jar`, `RmiQuery.jar`, `UpdateTravelTimes.jar`, `ScheduleGenerator.jar`) — each is a maven-shade execution in `transitclock/pom.xml`.

There is no lint step configured in the build.

## Code coverage

JaCoCo 0.8.12 is wired at the root `pom.xml`. It inherits into any module that declares `<parent>`, which today means `transitclock`, `transitclockQuickStart`, `transitclockBarefootClient`, and `transitclockTraccarClient`. `transitclockApi` and `transitclockWebapp` don't declare `<parent>`, so they currently produce no coverage data.

- Per-module HTML reports land at `<module>/target/site/jacoco/index.html` after `mvn verify`.
- Aggregate report (merges Core + both thin clients) lands at `coverage-report/target/site/jacoco-aggregate/index.html`. The `coverage-report` module's only job is to run `jacoco:report-aggregate`; it has no sources of its own.
- Quickest way to regenerate just the aggregate: `mvn verify -pl coverage-report -am`.
- `transitclockQuickStart` has JaCoCo explicitly skipped — it bundles `api.war`/`web.war` into `target/classes` as resources, and JaCoCo's analyzer chokes on multi-release JARs inside those WARs ("Can't add different class with same name"). QuickStart is a launcher, not logic worth measuring.
- No coverage threshold / build-break rule is configured. `jacoco:check` with a minimum goal would be the place to add one.

## Module layout

Nine Maven modules under the root aggregator `pom.xml`:

- **transitclock** — core engine. Artifact id `transitclockCore`. Contains domain model, AVL ingestion, matching, prediction generation, Hibernate entities, config, modules, IPC servers, and all executable `main` classes under `org.transitclock.applications`.
- **transitclockApi** — JAX-RS REST API WAR. Calls into a running Core process via RMI (see `org.transitclock.ipc`); does **not** talk to the DB directly for live vehicle/prediction data.
- **transitclockWebapp** — user-facing web UI WAR. Consumes the REST API; deployed to the same Tomcat instance as `transitclockApi`. Connects to the DB via `hibernate.cfg.xml` in `src/main/resources`.
- **transitclockQuickStart** — standalone `java -jar` launcher bundling Core+API+Webapp for local experimentation.
- **transitclockTraccarClient**, **transitclockBarefootClient** — thin clients for Traccar GPS devices and the Barefoot map-matching server. Depended on by `transitclock`.
- **transitclockIntegration** — end-to-end / prediction-accuracy tests; only built under the `include-integration-tests` profile.
- **transitclockPipelineTests** — real-Core behavior tests for the prediction pipeline (`AvlProcessor`, and eventually the other matcher/generator classes). Boots a real Core against in-memory HSQL with a small WMATA GTFS fixture via `CoreHarness` (a JUnit `@ClassRule`). Only built under the `include-pipeline-tests` profile.
- **coverage-report** — `packaging=pom` aggregator whose sole purpose is producing a JaCoCo aggregate report. No sources; inherits the root jacoco plugin and runs `report-aggregate` at `verify`.

## Runtime architecture

The system is a **pipeline of long-running processes**, not a single server. Understanding this pipeline is the fastest way to orient in the codebase.

1. **Core** (`org.transitclock.applications.Core`) is the workhorse long-running JVM per agency. It:
   - Loads GTFS config from the DB into `org.transitclock.gtfs.DbConfig` (an in-memory snapshot).
   - Starts `Module`s (subclasses of `org.transitclock.modules.Module`) declared in config — each is a thread. AVL feed modules under `org.transitclock.avl/` (GTFS-RT, NextBus, JMS, CSV playback, Traccar, Barefoot, etc.) pull or receive vehicle locations and push them into `AvlExecutor` → `AvlProcessor`.
   - `AvlProcessor` calls `SpatialMatcher` + `TemporalMatcher` to snap a vehicle to a `SpatialMatch` on a route's stopPath, then `MatchProcessor` derives `ArrivalDeparture`s and triggers `PredictionGenerator` (see `core/predictiongenerator/`) to produce predictions.
   - Exposes data over RMI via servers in `org.transitclock.ipc.servers` (`PredictionsServer`, `VehiclesServer`, `ConfigServer`, `CommandsServer`, `CacheQueryServer`, etc.). RMI interfaces are under `ipc/interfaces/`, client stubs under `ipc/clients/`.
   - Writes predictions, arrivals/departures, and vehicle state back to the DB through `db/hibernate/DataDbLogger` (an async batched writer — do not open short-lived Hibernate sessions for high-volume writes; go through the logger).

2. **transitclockApi** (REST) runs in Tomcat. Resources under `org.transitclock.api.rootResources` convert REST requests into RMI calls against a Core JVM. No live Core → no live predictions.

3. **transitclockWebapp** (UI) runs in Tomcat alongside the API and proxies through it.

### Bootstrapping / ops-style commands
These are all `main` classes in `org.transitclock.applications` and are wired as shaded JARs:

- `SchemaGenerator` — emits DDL from Hibernate annotated classes. Targets both `org.transitclock.db.structs` (core tables) and `org.transitclock.db.webstructs` (webapp tables — web agency registry, API keys). There is a known classloader issue running the shaded JAR; use `mvn exec:java -Dexec.mainClass=...` instead (see `transitclock/README.md`).
- `GtfsFileProcessor` — imports a GTFS feed into the DB. Use `-storeNewRevs` to activate the imported revision.
- `DbTest` — connectivity smoke test.
- `CreateWebAgency`, `CreateAPIKey` — seed webapp-side records.
- `RmiQuery` — CLI to hit a running Core's RMI servers.
- `UpdateTravelTimes`, `ScheduleGenerator` — offline batch jobs over historical data.

### Config
- Agency/runtime config comes from an XML config file (passed with `-c` and via the JVM property `-Dtransitime.configFiles=...`) plus system properties. The config framework is `org.transitclock.config.*` (typed `ConfigValue` subclasses registered statically on class load).
- Agency identity is a JVM-wide setting: `-Dtransitime.core.agencyId=<id>` is required on most `main` classes.
- DB connection info lives in `hibernate.cfg.xml` (on classpath) **and** can be overridden by config. The webapp has its own `hibernate.cfg.xml` in `transitclockWebapp/src/main/resources`.

### Two revision concepts
GTFS data in the DB is versioned by `configRev` and travel-time data by `travelTimesRev`. `ActiveRevisions` points at the currently-live pair. When changing ingestion logic or entity shapes, consider that multiple revs coexist in the same tables.

## Conventions to be aware of

- Package name is `org.transitclock` (not `org.transitime`); the project was renamed but README files still reference the old name in places.
- Hibernate entities in `db/structs/` are the source of truth for the schema — `SchemaGenerator` derives DDL from them. Add `@Entity` classes there, not hand-written SQL.
- New data sources belong in `org.transitclock.avl/` as a `Module` subclass.
- New prediction algorithms plug in via `PredictionGeneratorFactory` (configured by class name in the config file) — don't edit `PredictionGeneratorDefaultImpl` in place for an experiment.
- Lombok is on the classpath (`@Slf4j`, `@Data`, etc.); your IDE needs the annotation processor enabled.
