#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
umask 077
mkdir -p secrets
if [ ! -f secrets/private.pem ]; then
 openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out secrets/private.pem
 openssl pkey -in secrets/private.pem -pubout -out secrets/public.pem
fi
if [ ! -f .env ]; then
 printf 'POSTGRES_PASSWORD=%s
DB_PASSWORD=%s
MIGRATION_PASSWORD=%s
ADMIN_EMAIL=admin@npcomputers.local
ADMIN_PASSWORD=%s
APP_ORIGIN=http://localhost:8080
' "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" "$(openssl rand -hex 24)" > .env
fi
printf 'Setup ready. Read local admin credentials from the ignored .env file.
'
