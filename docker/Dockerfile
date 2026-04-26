# Multi-stage build for TheTransitClock.
#
# Stages:
#   builder       — Maven + JDK 21, runs `mvn install -DskipTests` against the
#                   full reactor and produces shaded JARs in
#                   transitclock/target/ plus api.war / web.war.
#   tools         — Same image as builder; kept mounted at /workspace so
#                   `docker compose run --rm tools <cmd>` can invoke the
#                   shaded JARs (or `mvn exec:java` for SchemaGenerator) with
#                   the build artifacts already present.
#   core-runtime  — JDK 21 (not JRE — Core needs the `tools.jar`/`rmiregistry`
#                   path on some flows). Carries Core.jar and the other
#                   shaded JARs the `tools` stage uses for admin work.
#   tomcat-runtime — Tomcat 9 + JDK 17 with api.war and web.war preinstalled
#                   (Tomcat 9 has no -jdk21 image; the WAR is still on
#                   javax.servlet so we cannot move to Tomcat 10/11 yet).
#                   Phase B replaces this with tomcat:11.0-jdk21-temurin.
#
# The two runtime stages copy from `builder`; nothing in the runtime images
# touches the local filesystem, which is what the user asked for.

# ----------------------------------------------------------------------------
# Stage 1: builder
# ----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# We don't pre-warm the dependency cache by copying poms first because
# `mvn dependency:go-offline` doesn't work cleanly in this reactor (some
# plugins resolve based on real sources). Without that, copying poms
# separately just wastes a cache slot — every source change still
# invalidates the COPY below. Keep it simple.
COPY . .

# No --mount=type=cache here on purpose: a BuildKit cache mount lives outside
# the image and would leave /root/.m2 empty in the `tools` stage downstream.
# We want the resolved deps to land in image layers so `docker compose run
# --rm tools mvn ...` can run admin commands offline.
RUN mvn -B -DskipTests install

# ----------------------------------------------------------------------------
# Stage 2: tools (admin one-shots: SchemaGenerator, GtfsFileProcessor,
# CreateWebAgency, CreateAPIKey, RmiQuery). Reuses the builder image so that
# `mvn exec:java` continues to work for SchemaGenerator (the shaded JAR has a
# known classloader issue — see transitclock/README.md).
# ----------------------------------------------------------------------------
FROM builder AS tools
WORKDIR /workspace
RUN apt-get update \
 && apt-get install -y --no-install-recommends postgresql-client \
 && rm -rf /var/lib/apt/lists/*
ENTRYPOINT []
CMD ["bash"]

# ----------------------------------------------------------------------------
# Stage 3: core-runtime
# ----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS core-runtime

WORKDIR /opt/transitclock

# Carry only what Core actually invokes. Logback config is bundled inside
# Core.jar; the runbook exposes /var/log/transitclock and /var/run/transitclock
# via volumes, so we create them up-front.
COPY --from=builder /workspace/transitclock/target/Core.jar ./Core.jar

# Run as a non-root user so a Core compromise can't trivially write outside
# its own directories. UID 10001 is high enough not to collide with anything
# the base image creates and stable enough that bind-mounted host volumes
# (e.g. .deploy/logs) can be chowned to it deterministically.
RUN useradd --system --uid 10001 --no-create-home --shell /usr/sbin/nologin transitclock \
 && mkdir -p /var/log/transitclock /var/run/transitclock \
 && chown -R 10001:10001 /opt/transitclock /var/log/transitclock /var/run/transitclock

USER 10001

EXPOSE 2099 2098

# The actual command line lives in docker-compose.yml so it can pull
# environment-driven properties. Keep this entrypoint trivial.
ENTRYPOINT ["java"]
CMD ["-jar", "/opt/transitclock/Core.jar"]

# ----------------------------------------------------------------------------
# Stage 4: tomcat-runtime
# ----------------------------------------------------------------------------
FROM tomcat:9.0-jdk17-temurin AS tomcat-runtime

# Drop the default ROOT app — we don't need it, and leaving it confuses people
# who hit http://localhost:8080/ expecting the transitclock UI.
RUN rm -rf /usr/local/tomcat/webapps/ROOT

COPY --from=builder /workspace/transitclockApi/target/api.war        /usr/local/tomcat/webapps/api.war
COPY --from=builder /workspace/transitclockWebapp/target/web.war     /usr/local/tomcat/webapps/web.war

# Same hardening as core-runtime: non-root service account. Tomcat needs to
# write into /usr/local/tomcat/{logs,temp,work,webapps} (catalina.sh creates
# log files, deploys explode there), so chown the whole directory.
RUN useradd --system --uid 10002 --no-create-home --shell /usr/sbin/nologin tomcat-runtime \
 && chown -R 10002:10002 /usr/local/tomcat

USER 10002

EXPOSE 8080
