# Test-Driven Java 21 + Tomcat 11 Upgrade for TransitClock

## Context

TransitClock is on **Java 17 + Tomcat 9** today (`docker/Dockerfile` line 85: `tomcat:9.0-jdk17-temurin`). `docker-compose.yml` carries the candid comment "Tomcat 10 doesn't work — the WARs are still javax.servlet, not jakarta.servlet." That comment is the whole problem: every Tomcat ≥ 10 implements Jakarta EE 9+, so the only path forward requires the `javax.*` → `jakarta.*` namespace migration. The repo has **590 `javax.*` imports and zero `jakarta.*` imports** today. Hibernate is still on **5.5.7.Final** which only knows the `javax.persistence` namespace, so a Hibernate 6.x jump rides along with the same migration.

The risk we are most exposed to is **regressions in the GTFS-realtime feed** (`GtfsRealtimeApi.getGtfsRealtimeVehiclePositionsFeed` / `getGtfsRealtimeTripFeed`). It is the highest-stakes external contract this system exposes, it is consumed by external systems (transit apps, open-data portals), and **today it has zero tests** — no unit tests, no integration tests, no golden protobuf fixtures. Upgrading library versions and rewriting JAX-RS imports while a contract has zero coverage is how we'd ship a silently-corrupted feed.

The strategy is therefore: **add the missing tests first on the current `javax` stack, lock in a green baseline, then do the upgrade in two sequenced phases** — Java 21 first (small, isolated), then the Jakarta + Tomcat 11 big-bang.

Two scope decisions confirmed by the user:
- **`transitclockQuickStart` is dropped from the upgrade scope.** It embeds Jetty 9.4.15 and bundles `api.war`/`web.war`. Once the WARs go Jakarta, Jetty 9.4 can't load them. We mark QuickStart as legacy/unsupported during the upgrade and revisit later.
- **HornetQ 2.3.25 is replaced with Apache ActiveMQ Artemis.** Direct successor lineage; supports `jakarta.jms`; keeps the optional JMS AVL feed working.

## Phase 0 — Test-First Foundation (do this on the current javax stack)

Goal: green coverage of every layer the upgrade will touch, **before** anything moves.

### 0.1 GTFS-RT golden-fixture suite (highest priority)

New module location: tests live under `transitclockApi/src/test/java/org/transitclock/api/gtfsRealtime/` (the API module currently has **no `src/test/`** directory at all — create it). New test resources under `transitclockApi/src/test/resources/gtfsrt/`.

Two layers, both required:

**Layer 1 — builder unit tests (no Jersey, no servlet container):**
- `GtfsRtVehicleFeedTest` — directly call `GtfsRtVehicleFeed.createMessage(canned Collection<IpcVehicleGtfsRealtime>)` and assert the resulting `FeedMessage` field-by-field. Cover: predictable + at-stop, predictable + in-transit, unpredictable, canceled trip, unscheduled trip (`ScheduleRelationship` matrix), frequency-based trip (`TripDescriptor.startTime` + `startDate`), heading/speed present/absent, stop-sequence propagation.
- `GtfsRtTripFeedTest` — same shape against `GtfsRtTripFeed.createMessage(List<IpcPrediction>)`. Cover: schedule-based prediction (uncertainty 300), delayed (301), late+subsequent (302), canceled trip, frequency trip, multiple stop time updates per trip, prediction grouping by trip ID.
- `DataCacheTest` — assert the 15s TTL cache (`transitclock.api.gtfsRtCacheSeconds`) returns cached `FeedMessage` within window and refreshes after.

**Layer 2 — HTTP-level tests using `jersey-test-framework-provider-grizzly2`:**
- Spin the `GtfsRealtimeApi` resource in-process (no Tomcat). Mock `VehiclesInterfaceFactory.get()` and `PredictionsInterfaceFactory.get()` (Mockito 5.12.0 is already on the classpath).
- Assert the response **`Content-Type`** (`application/octet-stream` for binary, `text/plain` for `?format=human`), the response **byte stream** parses back into `FeedMessage` via `FeedMessage.parseFrom(InputStream)`, and the `?format=human` variant matches the snapshot of `OctalDecoder.convertOctalEscapedString(message.toString())`.
- Cover error paths: missing API key, missing agency id, RMI client returns null/empty.

**Golden fixtures:**
- Capture binary protobufs once from the current javax stack, store as `vehicle_positions_baseline.pb` and `trip_updates_baseline.pb` under `transitclockApi/src/test/resources/gtfsrt/`.
- A test asserts the byte stream produced from the same canned `IpcVehicle*`/`IpcPrediction` inputs equals the fixture (both binary-equal **and** structurally-equal via `FeedMessage.parseFrom`). Binary equality is too strict for production wall-clock timestamps — freeze the clock by injecting a `Clock`/`Supplier<Long>` at the producer boundary, or strip `header.timestamp` before comparison.
- These fixtures become the regression backstop for both the Java 21 jump **and** the Jakarta jump.

### 0.2 JAX-RS endpoint smoke suite

New tests under `transitclockApi/src/test/java/org/transitclock/api/rootResources/`. One smoke per resource (`TransitimeApi`, `GtfsRealtimeApi`, `SiriApi`, `CommandsApi`, `CacheApi`, `TransitimeNonAgencyApi`) — for each, hit one representative endpoint per HTTP method via Jersey Test Framework with mocked RMI client factories. Assert HTTP 200 + non-empty body + correct `Content-Type`. Goal isn't deep coverage; it's "the wiring still works after we change Jersey 2.35 → 3.1.x and javax.ws.rs → jakarta.ws.rs."

Critical: also test the JAXB serialization. `transitclockApi/src/main/java/org/transitclock/api/data/` has 50+ DTOs annotated with `@XmlRootElement`/`@XmlElement`/`@XmlAttribute` from `javax.xml.bind.annotation.*` (note the dependency in `transitclock/pom.xml` is already on `jakarta.xml.bind-api:3.0.1` — the source imports lag the dep). Add at least one `Accept: application/xml` and one `Accept: application/json` test per content-negotiated endpoint.

### 0.3 Hibernate entity round-trip suite

New module location: `transitclockPipelineTests/src/test/java/org/transitclock/pipelinetests/persistence/`. Reuse `CoreHarness` (boots HSQL 2.2.4 in-memory) — it's already wired up.

For each of the 36 `@Entity` classes under `transitclock/src/main/java/org/transitclock/db/structs/` and `db/webstructs/`, add a round-trip test: build an instance, save via `DbQueue` (the path `DataDbLogger` actually uses), flush, read back via a fresh `Session`, assert deep equality. This catches:
- Hibernate 6's stricter HQL parser rejecting things 5.5 silently allowed.
- Generated DDL changes between Hibernate 5.5 and 6.x (column nullability, default values, sequence vs identity strategy on PostgreSQL, blob/clob handling).
- Boolean / enum / temporal type-mapping differences between the two.

Then add explicit tests for the **8+ legacy Criteria API call sites** (`StopArrivalDepartureCacheInterface`, `VehicleDataCache`, `dataCache/ehcache/StopArrivalDepartureCache`, `dataCache/jcs/scheduled/TripDataHistoryCache`, `misc/HibernateTest`, etc.). The legacy Criteria API was **removed in Hibernate 6.0** — these will not compile after the jump. Tests pinned to current behavior become the spec for the JPA Criteria rewrite.

### 0.4 JSP / servlet smoke suite

New tests under `transitclockWebapp/src/test/java/`. Use Tomcat's embedded test container (`tomcat-embed-core` 9.x for now, 11.x after the jump) or `jetty-server` 9.4 to render the 5–10 most-trafficked JSPs (those under `transitclockWebapp/src/main/webapp/maps/`, `reports/`, `welcome/`, plus the `/template/includes.jsp` shared header) and assert HTTP 200 + non-error markup. The JSTL URI change from `http://java.sun.com/jsp/jstl/core` → `jakarta.tags.core` would otherwise blow up at runtime in the browser — these tests catch it in CI.

### 0.5 RMI smoke

Add one `transitclockPipelineTests` test that boots the full RMI surface (registry + `VehiclesServer`, `PredictionsServer`, `ConfigServer`, `CommandsServer`) via `CoreHarness`, then exercises a client → server round-trip per interface using the existing `*InterfaceFactory.get()` clients. RMI uses plain `java.rmi.*` (zero `java.rmi.activation.*` imports — confirmed), so it should sail through Java 21, but a smoke test makes that a fact, not a hope.

### 0.6 CI step

After Phase 0 lands, add a GitHub Actions step that runs the new tests on every PR. Run them under both `mvn verify` and `mvn install -P run-all-tests` to confirm both profiles pick them up.

**Phase 0 exit gate:** all new tests green on `Java 17 + Tomcat 9 + Hibernate 5.5 + Jersey 2.35`. This is the baseline every later phase must keep green.

## Phase A — Java 21 (no Jakarta yet)

Small, isolated, low risk. Should be achievable in a single PR.

### A.1 Toolchain bumps

- Root `pom.xml` and per-module poms: change `<source>17</source>`/`<target>17</target>` → `<release>21</release>` on `maven-compiler-plugin`. Bump the plugin to `3.13.0`.
- `docker/Dockerfile`: `maven:3.9-eclipse-temurin-17` → `maven:3.9-eclipse-temurin-21`; `eclipse-temurin:17-jdk` → `eclipse-temurin:21-jdk`. Leave `tomcat:9.0-jdk17-temurin` alone for now (Tomcat 9 doesn't ship a `-jdk21` tag — use a multi-stage image with `eclipse-temurin:21-jdk` + a manual Tomcat 9 install, or accept JDK 17 at the Tomcat layer until Phase B).
- `.github/workflows/ci.yml` line 18–22: `java-version: 17` → `21`.
- Surefire: standardize all modules on `3.5.4` (currently `transitclockPipelineTests` is on `2.19.1`). 2.19 is a known issue under Java 17+ and a guaranteed problem under 21.

### A.2 Mandatory dependency bumps (Java-21 blockers, **stay on javax**)

These libraries break or warn loudly under Java 21. None of them require Jakarta:
- **Logback** `1.1.x` → `1.5.x` — current version is from 2014 and breaks under modern JDKs.
- **slf4j** `1.7.2` → `1.7.36` (last 1.x; stays binary-compatible with logback 1.5).
- **MySQL connector** `mysql:mysql-connector-java:5.1.35` → `com.mysql:mysql-connector-j:8.4.0`. The 5.1 line was retired and has known JDK-17+ failures.
- **HSQLDB** `2.2.4` → `2.7.4` (test scope only; needed for clean module behavior under Java 21).
- **byte-buddy** stays pinned at `1.14.15` (already explicitly pinned in `transitclock/pom.xml` line 272–275 to override Hibernate 5.5's old version — leave the pin until Phase B).

### A.3 `--add-opens` audit

`SystemMemoryMonitor` calls `method.setAccessible(true)` on a JDK internal. On Java 21, log a warning today; in some future JDK it will be denied. Either:
- Replace with `OperatingSystemMXBean.getCommittedVirtualMemorySize()` / `getTotalMemorySize()` (clean, public API), or
- Add `--add-opens java.base/jdk.internal.misc=ALL-UNNAMED` to `MAVEN_OPTS` and document.

### A.4 Verification — Phase A

- `mvn install -P run-all-tests` clean.
- All Phase 0 tests green.
- Docker stack boots; `docker compose up` produces a Core that ingests AVL and serves predictions over RMI; the GTFS-RT endpoints return parseable `FeedMessage` payloads with non-zero entity counts (compare against the golden fixtures).

**Phase A exit gate:** green on Java 21 + Tomcat 9 + Hibernate 5.5 + Jersey 2.35.

## Phase B — Jakarta + Tomcat 11 (the big jump)

This is the unavoidable big-bang. Everything that imports `javax.*` for one of the migrated namespaces moves in lockstep with its dependency. Don't try to ship this in pieces — partial Jakarta is undefined behavior.

### B.1 Migrate the namespaces with Eclipse Transformer

Use **Eclipse Transformer** (the same tool the Jakarta team ships for migration). It's lossless for the namespaces in scope. Configuration:

- In-place transform the source under `transitclock/`, `transitclockApi/`, `transitclockWebapp/`, `transitclockTraccarClient/`, `transitclockBarefootClient/`, `transitclockIntegration/`, `transitclockPipelineTests/`. Skip `transitclockQuickStart/` (out of scope per user decision).
- Run with the default JakartaEE rules; that handles `javax.servlet`, `javax.ws.rs`, `javax.persistence`, `javax.xml.bind`, `javax.annotation`, `javax.inject`, `javax.transaction`, `javax.validation`, `javax.jms` automatically.
- Manually review changes to: `org.transitclock.web.ReadConfigListener`, `transitclockApi/src/main/webapp/WEB-INF/web.xml`, `transitclockWebapp/src/main/webapp/WEB-INF/web.xml`, all 104 `*.jsp` files (taglib URI changes — see B.4).

### B.2 Dependency updates (Maven)

In `transitclock/pom.xml` (Core):
- `org.hibernate:hibernate-core:5.5.7.Final` → `org.hibernate.orm:hibernate-core:6.5.3.Final` (note groupId change to `org.hibernate.orm`). Adopt the **`hibernate-platform` BOM** (`org.hibernate.orm:hibernate-platform:6.5.3.Final`, `<type>pom</type>`, `<scope>import</scope>`) per the Hibernate docs to keep transitive versions aligned.
- Add `jakarta.persistence:jakarta.persistence-api:3.1.0`, `jakarta.transaction:jakarta.transaction-api:2.0.1`.
- `org.ehcache:ehcache:3.4.0` → `3.10.8` (3.10 is the first to integrate cleanly with Hibernate 6's new region-factory).
- Drop the `byte-buddy` 1.14.15 explicit pin — Hibernate 6 brings a new-enough version.
- Drop `org.hornetq:*` → add `org.apache.activemq:artemis-jms-client:2.33.0` (jakarta classifier). Audit the JMS AVL module under `org.transitclock.avl/` for HornetQ-specific connection-factory APIs and rewrite to the standard JMS 3.x lookup.
- `jakarta.xml.bind-api:3.0.1` is already there; bump to `4.0.2` and `com.sun.xml.bind:jaxb-impl:3.0.2` → `org.glassfish.jaxb:jaxb-runtime:4.0.5` to align with Jakarta XML Bind 4.0.

In `transitclockApi/pom.xml`:
- `javax.ws.rs:javax.ws.rs-api:2.0` → `jakarta.ws.rs:jakarta.ws.rs-api:3.1.0`.
- `javax.servlet:javax.servlet-api:3.0.1` (provided) → `jakarta.servlet:jakarta.servlet-api:6.1.0` (provided).
- `org.glassfish.jersey.containers:jersey-container-servlet:2.35` → `3.1.7`. Same bump for `jersey-media-moxy`, `jersey-hk2`. Per Jersey docs, 3.1.x is the first line implementing **Jakarta REST 3.1 / Jakarta EE 10**; minimum JDK is 11; runs cleanly on 21.
- `io.swagger.core.v3:swagger-jaxrs2:2.0.2` → `io.swagger.core.v3:swagger-jaxrs2-jakarta:2.2.22` (note `-jakarta` artifact id).
- `org.webjars:swagger-ui:3.17.1` → `5.17.14`.

In `transitclockWebapp/pom.xml`:
- `javax.servlet:javax.servlet-api:3.0.1` (provided) → `jakarta.servlet:jakarta.servlet-api:6.1.0` (provided).
- Add `jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.0` (api) and `org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1` (impl).
- Drop `tomcat7-maven-plugin:2.2` — defunct and the wrong namespace.

### B.3 web.xml rewrites

Both `transitclockApi/src/main/webapp/WEB-INF/web.xml` and `transitclockWebapp/src/main/webapp/WEB-INF/web.xml` declare Servlet 3.0 against `http://java.sun.com/xml/ns/javaee`. Rewrite to Servlet 6.0:

```xml
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">
```

The Jersey `<init-param>` name `javax.ws.rs.Application` becomes `jakarta.ws.rs.Application`. Listener and filter classes keep their FQNs but the imports inside those classes change.

### B.4 JSP / JSTL URI rewrite

All 104 JSPs declare:
```jsp
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
```

Jakarta Tags 3.0 (which ships with Tomcat 11) requires:
```jsp
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
```

A scripted find-replace across `transitclockWebapp/src/main/webapp/**/*.jsp` is fine; the JSP smoke suite from §0.4 catches mistakes.

### B.5 Hibernate 6 hand-fixes

The transformer rewrites imports but cannot fix:
- **Removed legacy Criteria API** — the 8+ call sites identified above (`StopArrivalDepartureCacheInterface.getArrivalsDeparturesFromDb`, `VehicleDataCache`, `ehcache/StopArrivalDepartureCache`, `jcs/scheduled/TripDataHistoryCache`, `misc/HibernateTest`, etc.) must be rewritten to `jakarta.persistence.criteria.CriteriaBuilder` / `CriteriaQuery`. The §0.3 entity round-trip + Criteria pinning tests are the spec.
- **HQL parser strictness** — Hibernate 6 uses a new SQM-based parser. Implicit joins, untyped parameters, and some `select new ...` forms behave differently. Run the full pipeline-test suite and chase any HQLException to the offending query.
- **DDL diff** — generate the 6.x DDL via `SchemaGenerator` and diff against the 5.5 DDL. Most likely diffs: sequence allocation, `boolean` column type on PostgreSQL, `enum` column length. Add a Flyway-style upgrade script if anything moves.
- **Bootstrap** — `HibernateUtils.getSessionFactory()` may need rework; Hibernate 6 prefers `StandardServiceRegistryBuilder` + `MetadataSources`. The pattern still works but options/properties names changed (e.g. `hibernate.connection.*` is now standardized via `jakarta.persistence.jdbc.*`).

### B.6 HornetQ → ActiveMQ Artemis

In `transitclock/src/main/java/org/transitclock/avl/` find the JMS feed module(s) (the survey turned up 25 `javax.jms` imports concentrated there). Rewrite the connection factory lookup to use Artemis's `ActiveMQConnectionFactory` (or the standard `jakarta.jms.ConnectionFactory` via JNDI). Add an integration test that publishes a canned AVL record to an in-process Artemis broker and asserts it lands in `AvlExecutor`.

### B.7 Container & runtime

- `docker/Dockerfile` line 85: `tomcat:9.0-jdk17-temurin` → `tomcat:11.0-jdk21-temurin`.
- `docker-compose.yml`: drop the "Tomcat 10 doesn't work" comment.
- Confirm Tomcat 11's catalina.policy and conf changes don't break the deploy (default config is fine for both WARs; the only deploy-time concern is making sure both WARs land in `/usr/local/tomcat/webapps/`).
- RMI hostname / port (Core JVM, not Tomcat) is unaffected — pure `java.rmi.*` keeps working on Java 21.

### B.8 Verification — Phase B

- `mvn install -P run-all-tests` clean on Java 21.
- All Phase 0 tests green (especially the GTFS-RT golden fixtures — byte-equal to the pre-upgrade baseline once timestamps are stripped).
- `docker compose up` boots; both WARs deploy to Tomcat 11; the API responds; the webapp renders.
- Manual: hit `/api/v1/key/{key}/agency/{agency}/command/gtfs-rt/vehiclePositions` and `tripUpdates`, parse the binary response in a separate JVM with `gtfs-realtime-bindings`, eyeball field shape against a Tomcat-9 capture from the same agency snapshot.
- Manual: smoke the webapp's map view in a browser (the JSTL URI rewrite is the most-likely silent breakage).

## Risk register — things most likely to break the upgrade

1. **Legacy Hibernate Criteria API removal** (Hibernate 6.0). 8+ call sites in `core/dataCache/`. Highest-impact code change after the namespace move. Mitigated by §0.3 pinning tests.
2. **HQL parser strictness** in Hibernate 6 (new SQM parser). Subtle: queries that "worked" silently on 5.5 will throw at runtime. Only caught by exercising real query paths under load — the pipeline-test suite gets us most of the way.
3. **JSP / JSTL URI mismatch** — silent (page renders blank/garbled, no exception). §0.4 smoke suite is the only real defense.
4. **JAXB serialization regression** — the `javax.xml.bind` source imports already disagree with the `jakarta.xml.bind-api` pom dep. The transformer aligns these, but `MOXyJsonProvider` (used by Jersey for JSON output) has different default settings between Jersey 2.x and 3.x — JSON shape may change. §0.2 with both `application/json` and `application/xml` accept variants is the spec.
5. **ehcache 3.4 → 3.10 region-factory wiring**. Cache config under `core/dataCache/HoldingTimeCache` may need an `EhcacheXmlConfiguration` rewrite. Pipeline tests touch this.
6. **HornetQ replacement** — the only path with no automated test coverage today. §B.6 in-process Artemis test is non-optional.
7. **Tomcat 9 with JDK 21** during Phase A — Tomcat 9 is supported on JDK 21 (since 9.0.85) but we should pin a recent 9.0.x. Earlier 9.0.x had reflection-on-JDK-internals warnings and `WebappClassLoader` issues on 21.
8. **gtfs-realtime-bindings 0.0.4 is from ~2016**. Worth bumping to current (≥ 0.0.8 / `1.0+`); the golden fixtures protect us through the bump.
9. **Coverage report aggregation** (`coverage-report` module). After Phase B the new `transitclockApi` test sources will produce JaCoCo data — need to add `<dependency>` on `transitclockApi` to `coverage-report/pom.xml` so `report-aggregate` picks them up.
10. **CI run time** — Phase 0 tests add 2–5 minutes; the pipeline-test suite already adds significant time. Consider a separate `gtfs-rt-tests` GitHub job that runs in parallel with `pipeline-tests`.

## Critical files to be modified

**Phase 0 (new tests, new directories):**
- `transitclockApi/src/test/` — create the directory; add `gtfsRealtime/GtfsRtVehicleFeedTest.java`, `gtfsRealtime/GtfsRtTripFeedTest.java`, `gtfsRealtime/DataCacheTest.java`, `rootResources/*SmokeTest.java`, `resources/gtfsrt/*.pb` fixtures.
- `transitclockPipelineTests/src/test/java/org/transitclock/pipelinetests/persistence/` — entity round-trip + Criteria-pinning tests.
- `transitclockWebapp/src/test/java/` — JSP smoke tests (new directory).
- `.github/workflows/ci.yml` — new test step.

**Phase A (Java 21):**
- All `pom.xml` (root + modules): `<release>21</release>`, surefire 3.5.4, dep bumps (logback, mysql-connector-j, hsqldb).
- `docker/Dockerfile`, `.github/workflows/ci.yml` — JDK 21.
- `transitclock/src/main/java/org/transitclock/monitoring/SystemMemoryMonitor.java` — replace `setAccessible` reflection.

**Phase B (Jakarta + Tomcat 11):**
- All Java sources under `transitclock/`, `transitclockApi/`, `transitclockWebapp/`, `transitclockTraccarClient/`, `transitclockBarefootClient/`, `transitclockIntegration/`, `transitclockPipelineTests/` — namespace migration via Eclipse Transformer.
- `transitclockApi/src/main/webapp/WEB-INF/web.xml`, `transitclockWebapp/src/main/webapp/WEB-INF/web.xml` — schema 6.0 + Jersey init-param rename.
- `transitclockWebapp/src/main/webapp/**/*.jsp` (104 files) — JSTL URI rewrite to `jakarta.tags.core` / `jakarta.tags.fmt`.
- `transitclock/pom.xml`, `transitclockApi/pom.xml`, `transitclockWebapp/pom.xml` — Hibernate 6.5, Jersey 3.1, JSTL 3.0, JAXB 4.0, Artemis, Swagger jakarta artifact.
- `transitclock/src/main/java/org/transitclock/db/hibernate/HibernateUtils.java`, `DataDbLogger.java`, `DbQueue.java` — bootstrap & session API check.
- 8+ files under `transitclock/src/main/java/org/transitclock/core/dataCache/` — legacy Criteria → JPA Criteria.
- JMS AVL module under `transitclock/src/main/java/org/transitclock/avl/` — HornetQ → Artemis.
- `docker/Dockerfile`, `docker-compose.yml` — Tomcat 11.

**Out of scope (per user decision):**
- `transitclockQuickStart/` — marked legacy; not migrated.

## Existing utilities to reuse

- `CoreHarness` (`transitclockPipelineTests/src/test/java/org/transitclock/pipelinetests/CoreHarness.java`) — JUnit 4 `ExternalResource` that boots a real Core against in-memory HSQL with a WMATA 5A GTFS fixture. Extend with `@RmiOnly` / `@PersistenceOnly` modes to avoid full Core boot for the new persistence and RMI smoke tests.
- `DataDbLogger` / `DbQueue` (`transitclock/src/main/java/org/transitclock/db/hibernate/`) — the canonical write path. Persistence tests should go through this, not `Session.save()` directly.
- `VehiclesInterfaceFactory.get()` / `PredictionsInterfaceFactory.get()` (`transitclock/src/main/java/org/transitclock/ipc/clients/`) — mock at this seam in §0.1, §0.2.
- `OctalDecoder.convertOctalEscapedString` (`transitclockApi/src/main/java/org/transitclock/api/gtfsRealtime/`) — already used by the human-format endpoint; reuse in tests for snapshot comparisons.
- Jackson + JAX-RS `Response` (`jakarta.ws.rs.core.Response`) — for negotiated content tests.
- Mockito 5.12.0 (already on classpath) — for RMI client mocks.

## End-to-end verification

After **each phase**, in order:

1. Run the /simplify and /pr-review-toolkit skills, incorporating changes from pr-review-toolkit
2. `mvn install -P run-all-tests -DskipTests=false` — full reactor, all profiles, no skips. Must pass.
3. `mvn -pl coverage-report -am verify` — confirm JaCoCo aggregate still generates and that the new `transitclockApi` test coverage is included.
4. `docker compose down -v && docker compose up --build` — clean boot of the full stack (Postgres, Core, Tomcat). Wait for `Core started` log line.
5. From a separate JVM on the host, run a small client that:
   - Calls `RmiQuery` → asserts `VehiclesInterface.getGtfsRealtime()` returns at least one vehicle (assuming the AVL feed is configured).
   - HTTP GETs `/api/v1/key/{key}/agency/{agency}/command/gtfs-rt/vehiclePositions` → parses with `FeedMessage.parseFrom(...)` → asserts entity count > 0 and matches the entity count from the RMI call.
   - HTTP GETs `/api/v1/key/{key}/agency/{agency}/command/gtfs-rt/tripUpdates?format=human` → asserts response is non-empty and contains expected `trip_id` substring.
6. Open the webapp map view in a browser — confirm vehicles render (this is the JSTL/JSP path Phase B is most likely to silently break).
7. `mvn -pl transitclockPipelineTests -am -P include-pipeline-tests test` — pipeline tests against the upgraded stack must still pass.
8. `mvn -pl transitclockIntegration -am -P include-integration-tests test` — integration tests (excluding the `@Ignore`d `PredictionAccuracyIntegrationTest` per CLAUDE.md) must pass.

If any step fails, fix forward — do not split the Phase B PR.
