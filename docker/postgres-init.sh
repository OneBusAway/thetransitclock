-- Postgres init script. Runs once when the data volume is first created.
-- POSTGRES_USER / POSTGRES_PASSWORD / POSTGRES_DB are honoured by the
-- official postgres image to set up the superuser and create the first
-- DB. We use that to create `wmata` (the per-agency DB) and add the
-- `web` DB and the `transitclock` role here.

CREATE USER transitclock WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE wmata TO transitclock;
ALTER DATABASE wmata OWNER TO transitclock;

CREATE DATABASE web OWNER transitclock;
