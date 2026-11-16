
#!/usr/bin/env bash
set -euo pipefail

psql -v ON_ERROR_STOP=1 \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  -v "hello_password=$HELLO_DB_PASSWORD" \
  -v "identity_password=$IDENTITY_DB_PASSWORD" <<'SQL'

-- Create application users
CREATE ROLE hello_app LOGIN PASSWORD :'hello_password';
CREATE ROLE identity_app LOGIN PASSWORD :'identity_password';

-- Each database has its own owner
CREATE DATABASE hello_db OWNER hello_app;
CREATE DATABASE identity_db OWNER identity_app;

-- Remove default public connectivity
REVOKE CONNECT ON DATABASE hello_db FROM PUBLIC;
REVOKE CONNECT ON DATABASE identity_db FROM PUBLIC;

-- Explicitly authorize each owner
GRANT CONNECT ON DATABASE hello_db TO hello_app;
GRANT CONNECT ON DATABASE identity_db TO identity_app;

SQL
