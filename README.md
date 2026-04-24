TheTransitClock
====

This is a fork of TheTransitClock, a [GTFS-RT Trip Updates](https://gtfs.org/documentation/realtime/feed-entities/trip-updates/) generation engine used by public transit agencies around the world, including in Minneapolis, MN where [the software was found to outperform proprietary alternatives](http://berrebi.net/wp-content/uploads/2020/01/trbws07_FileUploads_2020-AM-Presentations_3470_pdf_13557_P20-20421_2020-01-14-09-24-54.pdf).

[TheTransitClock](https://thetransitclock.github.io) was developed by Sean Óg Crudden and Simon J. Berrebi, Ph.D., itself a fork of [Swiftly Transitime](https://transitime.github.io/core/).

## About this Repo

The complete core Java software for the Transitime real-time transit information project. The purpose of the software is to use any type of real-time GPS data to generate useful public transportation information, namely a GTFS-RT Trip Updates feed.

The system is for both letting passengers know the status of their vehicles and helping agencies more effectively manage their systems. By providing a complete open-source system, agencies can have a cost effective system and have full ownership of it. 

## Build

The software is made up of three modules which can each be built with maven. See [BUILD.md](./BUILD.md).

The core functionality is in the transitime project. The REST api is in transitimeApi and the user Web applicaton is in transitimeWebapp.

## Setup

The main module is transitTime. This has several standalone programs in the org.transitime.applications package.

SchemaGenerator.java will generate the SQL to create the database structures you need to run on.<br/>
DBTest.java can be used to test that the database can be connected to.<br/>
GTFSFileProcessor.java will read a GTFS file into this database structure.<br/>
Core.java is as the name implies is the workhorse of the system. <br/>
RmiQuery.java allows you make queries to the server run in core from the command line.<br/>
CreateAPIKey.java a test app to allow you create test/demo key to access REST api webapp.<br/>
CreateWebAgency.java is used to create and agency that will work in transitimeWebapp.<br/>

Details on how to run each of these and their respective parameters are in the README for the transitime module.

Once this is set up the next step is to set up the transitimeApi which is a RESTful API. This API makes RMI calls to the RMI Server started by Core.java to provide results. This is a war file which can be deployed into Tomcat.  

The transitimeWebapp in turn is a web application which uses the transitTimeAPI to provided a user interface. This is a war file which can be deployed into Tomcat. This connects to the database and the connection information is configured in hibernate.cfg.xml in the src/main/resources directory. Currently this needs to be deployed on the same server as the API.

The transitimeQuickStart can be built with mvn install and ran using java -jar transitimeQuickStart it is currently a work in progress but the gui elements can be seen.

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
