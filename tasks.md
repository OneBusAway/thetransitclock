# Java 21 + Tomcat 11 / Jakarta Migration — Task List

Companion to the migration plan. Tasks are grouped by phase and ordered by ID. The dependency graph is captured in the **Blocked by** field — a task is only ready to start once all its blockers are completed.

Phase 0 sub-tasks (0.2–0.5) are independent of each other and can be parallelized once 0.1 lands the API-module test scaffolding.

---

## Phase 0 — Test-First Foundation

Add green coverage of every layer the upgrade will touch on the current `javax` stack. Exit gate: all new tests green on `Java 17 + Tomcat 9 + Hibernate 5.5 + Jersey 2.35`.

### Task 1 — Phase 0.1: GTFS-RT golden-fixture test suite

- **Status:** completed
- **Blocked by:** —
- **Description:** Create `transitclockApi/src/test/` scaffolding (the API module currently has no test sources at all). Add builder unit tests (`GtfsRtVehicleFeedTest`, `GtfsRtTripFeedTest`, `DataCacheTest`), Jersey Test Framework HTTP-level tests for `GtfsRealtimeApi`, and golden `.pb` fixtures under `transitclockApi/src/test/resources/gtfsrt/` for both `vehiclePositions` and `tripUpdates` feeds. Highest priority — this is the regression backstop for both upgrade phases.

### Task 2 — Phase 0.2: JAX-RS endpoint smoke suite

- **Status:** completed
- **Blocked by:** Task 1 (shares the API module test scaffolding from 0.1)
- **Description:** One smoke test per root resource (`TransitimeApi`, `GtfsRealtimeApi`, `SiriApi`, `CommandsApi`, `CacheApi`, `TransitimeNonAgencyApi`). For each, hit one representative endpoint per HTTP method via Jersey Test Framework with mocked RMI client factories. Cover both `application/json` and `application/xml` Accept variants for the JAXB-serialized DTOs in `transitclockApi/src/main/java/org/transitclock/api/data/`.

### Task 3 — Phase 0.3: Hibernate entity round-trip + Criteria pinning tests

- **Status:** in progress
- **Blocked by:** —
- **Description:** Round-trip test per `@Entity` in `db/structs/` and `db/webstructs/` via `DbQueue` (the actual write path used by `DataDbLogger`), under `transitclockPipelineTests/src/test/java/org/transitclock/pipelinetests/persistence/`. Reuse `CoreHarness`. Plus pinning tests for the 8+ legacy Criteria API call sites under `core/dataCache/` (`StopArrivalDepartureCacheInterface`, `VehicleDataCache`, `dataCache/ehcache/StopArrivalDepartureCache`, `dataCache/jcs/scheduled/TripDataHistoryCache`, `misc/HibernateTest`, etc.). The legacy Criteria API was **removed in Hibernate 6.0**; these pinning tests become the spec for the JPA Criteria rewrite.

### Task 4 — Phase 0.4: JSP / servlet smoke suite

- **Status:** pending
- **Blocked by:** —
- **Description:** Render the 5–10 most-trafficked JSPs (those under `transitclockWebapp/src/main/webapp/maps/`, `reports/`, `welcome/`, plus the `template/includes.jsp` shared header) via embedded Tomcat or Jetty in tests; assert HTTP 200 + non-error markup. New tests under `transitclockWebapp/src/test/java/`. Catches the JSTL URI rewrite regression in CI — the change from `http://java.sun.com/jsp/jstl/core` → `jakarta.tags.core` would otherwise blow up silently in the browser.

### Task 5 — Phase 0.5: RMI smoke test

- **Status:** pending
- **Blocked by:** —
- **Description:** One test in `transitclockPipelineTests` that boots the full RMI surface via `CoreHarness` (registry + `VehiclesServer`, `PredictionsServer`, `ConfigServer`, `CommandsServer`) and exercises a client → server round-trip per interface using the existing `*InterfaceFactory.get()` clients. Pure `java.rmi.*` should sail through Java 21, but a smoke test makes that a fact.

### Task 6 — Phase 0.6: Wire new tests into CI

- **Status:** pending
- **Blocked by:** Tasks 1, 2, 3, 4, 5
- **Description:** Add the new test sources to `.github/workflows/ci.yml` so they run on every PR. Run them under both `mvn verify` and `mvn install -P run-all-tests` to confirm both profiles pick them up. Consider a separate `gtfs-rt-tests` job that runs in parallel with `pipeline-tests` to keep wall time down.

---

## Phase A — Java 21 (no Jakarta yet)

Small, isolated, low-risk. Single PR. Exit gate: green on `Java 21 + Tomcat 9 + Hibernate 5.5 + Jersey 2.35`.

### Task 7 — Phase A: Java 21 toolchain bump

- **Status:** pending
- **Blocked by:** Task 6
- **Description:**
  - All poms: `<source>17</source>`/`<target>17</target>` → `<release>21</release>` on `maven-compiler-plugin`; bump plugin to `3.13.0`.
  - Surefire standardized on `3.5.4` everywhere (currently `transitclockPipelineTests` is on `2.19.1`).
  - Docker base images: `maven:3.9-eclipse-temurin-21`, `eclipse-temurin:21-jdk`. Keep Tomcat 9 at the runtime layer for now.
  - `.github/workflows/ci.yml`: `java-version: 17` → `21`.
  - Mandatory dep bumps (Java-21 blockers, **stay on javax**): Logback `1.1.x` → `1.5.x`; slf4j `1.7.2` → `1.7.36`; `mysql-connector-java:5.1.35` → `mysql-connector-j:8.4.0`; HSQLDB `2.2.4` → `2.7.4`. Leave `byte-buddy` pinned at `1.14.15` until Phase B.
  - Replace `SystemMemoryMonitor`'s `setAccessible` reflection with public `OperatingSystemMXBean.getCommittedVirtualMemorySize()` / `getTotalMemorySize()`.

---

## Phase B — Jakarta + Tomcat 11 (the big jump)

Unavoidable big-bang. Don't ship in pieces — partial Jakarta is undefined behavior.

### Task 8 — Phase B: Jakarta + Tomcat 11 big-bang migration

- **Status:** pending
- **Blocked by:** Task 7
- **Description:**
  - **Namespace migration:** Eclipse Transformer in-place across `transitclock/`, `transitclockApi/`, `transitclockWebapp/`, `transitclockTraccarClient/`, `transitclockBarefootClient/`, `transitclockIntegration/`, `transitclockPipelineTests/`. Skip `transitclockQuickStart/` (out of scope per user decision).
  - **Hibernate 6.5:** `org.hibernate:hibernate-core:5.5.7.Final` → `org.hibernate.orm:hibernate-core:6.5.3.Final` via the `hibernate-platform` BOM. Hand-rewrite the 8+ legacy Criteria call sites to `jakarta.persistence.criteria`. Audit HQL parser strictness, DDL diff, bootstrap (`HibernateUtils.getSessionFactory()`).
  - **Jersey 3.1.7:** `jersey-container-servlet` / `jersey-media-moxy` / `jersey-hk2` 2.35 → 3.1.7. JAX-RS API 2.0 → `jakarta.ws.rs-api:3.1.0`. Servlet API 3.0.1 → `jakarta.servlet-api:6.1.0`.
  - **JSTL 3.0:** Add `jakarta.servlet.jsp.jstl-api:3.0.0` + `org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1`. Drop `tomcat7-maven-plugin:2.2`.
  - **JAXB 4.0:** `jakarta.xml.bind-api` 3.0.1 → 4.0.2; `com.sun.xml.bind:jaxb-impl:3.0.2` → `org.glassfish.jaxb:jaxb-runtime:4.0.5`.
  - **Swagger:** `swagger-jaxrs2:2.0.2` → `swagger-jaxrs2-jakarta:2.2.22`; `swagger-ui:3.17.1` → `5.17.14`.
  - **ehcache:** `3.4.0` → `3.10.8`. Drop the explicit `byte-buddy` pin.
  - **HornetQ → Artemis:** Drop `org.hornetq:*`; add `org.apache.activemq:artemis-jms-client:2.33.0`. Rewrite the JMS AVL feed module under `org.transitclock.avl/` to standard JMS 3.x lookup. Add an in-process Artemis broker test that publishes a canned AVL record and asserts it lands in `AvlExecutor`.
  - **web.xml:** Both descriptors to schema 6.0 (`https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd`). Jersey init-param `javax.ws.rs.Application` → `jakarta.ws.rs.Application`.
  - **JSP taglib URIs:** Scripted rewrite across all 104 JSPs: `http://java.sun.com/jsp/jstl/core` → `jakarta.tags.core`; `http://java.sun.com/jsp/jstl/fmt` → `jakarta.tags.fmt`.
  - **Container:** `tomcat:9.0-jdk17-temurin` → `tomcat:11.0-jdk21-temurin`; drop the "Tomcat 10 doesn't work" comment in `docker-compose.yml`.

---

## Verification (per phase, in order)

After **each phase**, in order:

1. Run the `/simplify` and `/pr-review-toolkit` skills, incorporating changes from `pr-review-toolkit`.
2. `mvn install -P run-all-tests -DskipTests=false` — full reactor, all profiles, no skips. Must pass.
3. `mvn -pl coverage-report -am verify` — confirm JaCoCo aggregate still generates and that the new `transitclockApi` test coverage is included.
4. `docker compose down -v && docker compose up --build` — clean boot of the full stack (Postgres, Core, Tomcat). Wait for `Core started` log line.
5. From a separate JVM on the host, run a small client that:
   - Calls `RmiQuery` → asserts `VehiclesInterface.getGtfsRealtime()` returns at least one vehicle.
   - HTTP GETs `/api/v1/key/{key}/agency/{agency}/command/gtfs-rt/vehiclePositions` → parses with `FeedMessage.parseFrom(...)` → asserts entity count > 0 and matches the entity count from the RMI call.
   - HTTP GETs `/api/v1/key/{key}/agency/{agency}/command/gtfs-rt/tripUpdates?format=human` → asserts response is non-empty and contains expected `trip_id` substring.
6. Open the webapp map view in a browser — confirm vehicles render (the JSTL/JSP path Phase B is most likely to silently break).
7. `mvn -pl transitclockPipelineTests -am -P include-pipeline-tests test` — pipeline tests against the upgraded stack must still pass.
8. `mvn -pl transitclockIntegration -am -P include-integration-tests test` — integration tests (excluding the `@Ignore`d `PredictionAccuracyIntegrationTest` per CLAUDE.md) must pass.
9. Compact the conversation. Each task's PR is self-contained — once it's merged into upgrades, the per-task tool output, intermediate diffs, and exploratory reads are no longer load-bearing. The next task starts from a clean slate: tasks.md, the migration plan, and the now-current upgrades branch.

If any step fails, fix forward — do not split the Phase B PR.
