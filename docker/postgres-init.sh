#!/bin/bash
# Postgres init script. Runs once when the data volume is first created.
# POSTGRES_USER / POSTGRES_PASSWORD / POSTGRES_DB are honoured by the
# official postgres image to set up the superuser and create the first
# DB. We use that to create `wmata` (the per-agency DB) and add the
# `web` DB and the `transitclock` role here.
#
# The transitclock role's password is read from TRANSITCLOCK_DB_PASSWORD
# (passed through from the top-level .env via docker-compose.yml), so
# rotating it doesn't require editing this file.
#
# ON_ERROR_STOP makes any statement failure abort the script, instead of
# letting psql march through the rest with a non-zero log line and a
# zero exit code. Idempotent forms below ensure that re-running on a
# partially-initialized data volume doesn't trip on "role already exists"
# / "database already exists".

set -euo pipefail

: "${TRANSITCLOCK_DB_PASSWORD:?postgres-init.sh: TRANSITCLOCK_DB_PASSWORD must be set (see .env.example)}"

psql -v ON_ERROR_STOP=1 \
     --username "$POSTGRES_USER" \
     --dbname "$POSTGRES_DB" \
     --set "transitclock_password=$TRANSITCLOCK_DB_PASSWORD" \
     <<-'SQL'
DO $do$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'transitclock') THEN
    EXECUTE format('CREATE ROLE transitclock LOGIN PASSWORD %L', :'transitclock_password');
  ELSE
    EXECUTE format('ALTER ROLE transitclock WITH PASSWORD %L', :'transitclock_password');
  END IF;
END
$do$;

GRANT ALL PRIVILEGES ON DATABASE wmata TO transitclock;
ALTER DATABASE wmata OWNER TO transitclock;

SELECT 'CREATE DATABASE web OWNER transitclock'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'web')
\gexec
SQL
