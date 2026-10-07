#!/bin/sh
set -eu
psql --username "$POSTGRES_USER" --dbname npcomputers -v ON_ERROR_STOP=1 -v app_password="$DB_PASSWORD" -v migration_password="$MIGRATION_PASSWORD" -f /opt/np/roles.sql
