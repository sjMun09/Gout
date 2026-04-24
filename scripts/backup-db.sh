#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# scripts/backup-db.sh
#
# Gout-Care PostgreSQL(+PostGIS+pgvector) 컨테이너의 논리 백업 스크립트.
#
# 사용법:
#   ./scripts/backup-db.sh                 # 기본값으로 backups/ 에 저장
#   ./scripts/backup-db.sh /path/to/dir    # 출력 디렉토리 지정
#
# 환경 변수:
#   COMPOSE_FILE   docker compose 파일 (default: docker-compose.yml)
#   PG_SERVICE     service 이름        (default: postgres)
#   PG_USER        postgres user       (default: gout)
#   PG_DB          DB 이름             (default: gout_dev)
#
# cron 예시 (매일 03:30):
#   30 3 * * * cd /opt/gout && ./scripts/backup-db.sh /var/backups/gout >> /var/log/gout-backup.log 2>&1
#
# 복구는 `docker compose exec -T postgres psql -U $PG_USER -d $PG_DB < backup.sql`.
# ---------------------------------------------------------------------------
set -euo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"
PG_SERVICE="${PG_SERVICE:-postgres}"
PG_USER="${PG_USER:-gout}"
PG_DB="${PG_DB:-gout_dev}"

OUT_DIR="${1:-./backups}"
mkdir -p "$OUT_DIR"

TS="$(date +%Y%m%d-%H%M%S)"
OUT_FILE="$OUT_DIR/backup-${PG_DB}-${TS}.sql"

if ! command -v docker >/dev/null 2>&1; then
  echo "[backup-db] docker 명령을 찾을 수 없습니다." >&2
  exit 1
fi

# docker compose 내 postgres 컨테이너가 살아있는지 빠르게 체크
if ! docker compose -f "$COMPOSE_FILE" ps --status running --services | grep -qx "$PG_SERVICE"; then
  echo "[backup-db] $PG_SERVICE 서비스가 실행 중이지 않습니다. docker compose up -d 후 다시 시도하세요." >&2
  exit 2
fi

echo "[backup-db] $(date -u +%FT%TZ) dumping $PG_DB → $OUT_FILE"

# pg_dump: plain SQL + CREATE EXTENSION 포함. 다른 서버로 옮길 때 관리 쉬움.
docker compose -f "$COMPOSE_FILE" exec -T "$PG_SERVICE" \
  pg_dump -U "$PG_USER" -d "$PG_DB" --clean --if-exists --no-owner --no-privileges \
  > "$OUT_FILE"

BYTES="$(wc -c < "$OUT_FILE")"
echo "[backup-db] done: ${BYTES} bytes → $OUT_FILE"

# 30일 초과 백업은 정리 (옵션. 매일 돌릴 때 유용)
find "$OUT_DIR" -maxdepth 1 -type f -name "backup-${PG_DB}-*.sql" -mtime +30 -print -delete 2>/dev/null || true
