#!/bin/bash
# PostgreSQL 초기화 스크립트
# docker-entrypoint-initdb.d 에 마운트되어 첫 구동 시 1회 자동 실행됨.
# 이후 Flyway V1__init_extensions.sql 가 멱등적으로 같은 확장을 재확인한다.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
  CREATE EXTENSION IF NOT EXISTS postgis;
  CREATE EXTENSION IF NOT EXISTS vector;
  CREATE EXTENSION IF NOT EXISTS pg_trgm;
  CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
EOSQL
