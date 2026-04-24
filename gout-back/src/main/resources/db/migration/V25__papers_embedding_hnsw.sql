-- papers.embedding: ivfflat → HNSW 전환
-- ---------------------------------------------------------------
-- 근거
--   - 점진(주간) 적재에 강하다: HNSW 는 증분 INSERT 에 대해 그래프를 그대로 확장한다.
--     반면 ivfflat 은 lists 파티션 품질이 초기 학습 데이터에 의존하여 데이터가 늘수록
--     재빌드가 필요하고 리콜이 떨어진다.
--   - 레이턴시/리콜 모두 HNSW 가 일반적으로 우위 (pgvector 공식 벤치).
--   - 도메인 규모 1K~10K 수준 → 메모리 비용 무시 가능.
--
-- 사전 요구
--   - pgvector >= 0.5.0 (HNSW 지원). 현 프로젝트는 pgvector/pgvector:pg17 이미지를 사용하며
--     해당 이미지는 pgvector 0.8.2 를 탑재한다. 아래 주석 쿼리로 버전 확인 가능:
--     SELECT extversion FROM pg_extension WHERE extname='vector';
--
-- 트랜잭션
--   - CREATE INDEX CONCURRENTLY 는 트랜잭션 블록 안에서 실행할 수 없다.
--   - Flyway 11+ 의 스크립트-레벨 지시자로 이 스크립트만 트랜잭션 밖에서 실행한다.
--   - application.yml 에도 spring.flyway.mixed=true 를 설정하여 혼합 스크립트를 허용한다.
-- ---------------------------------------------------------------
-- flyway:executeInTransaction=false

-- 전환 순서 (빌드 중/실패 시 인덱스 공백 방지):
--   1) 새 HNSW 인덱스를 별도 이름(idx_papers_embedding_hnsw) 으로 먼저 생성
--   2) 빌드 성공 후 기존 ivfflat 인덱스 제거
--   이 순서는 다음을 보장한다:
--     - 빌드 실패 시 → 기존 ivfflat 인덱스가 그대로 남아 쿼리는 계속 인덱스 스캔
--     - 빌드 중      → 두 인덱스가 공존, 옵티마이저가 더 나은 쪽 선택
--     - 빌드 성공 후 → 기존 인덱스 제거, HNSW 단독
-- CONCURRENTLY 가 실패할 경우 INVALID 인덱스가 남을 수 있으므로 DROP 은 IF EXISTS 로 멱등 보장.

-- (1) HNSW 인덱스 생성 (신규, 별도 이름)
--   m=16 (default)             : 그래프 노드당 연결 수. 높을수록 리콜↑ 메모리/빌드시간↑
--   ef_construction=64 (default): 빌드 시 탐색 폭. 높을수록 빌드 느림, 리콜↑
-- 쿼리 시 튜닝:
--   SET hnsw.ef_search = 40;   -- 기본 40. 높일수록 리콜↑ 레이턴시↑
--
-- 실운영 주의:
--   - 실데이터(수천~수만 행) 에서는 빌드에 수 초~수십 초가 소요될 수 있다.
--   - CONCURRENTLY 덕분에 DML 은 막지 않으나, 빌드 중 신규 INSERT 는 인덱스에 늦게 반영될 수 있다.
--   - 현 단계(1K~10K 점진 적재)에서는 영향 미미.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_papers_embedding_hnsw
    ON papers USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- (2) 기존 ivfflat 인덱스 제거 (빌드 성공 후에만 도달)
DROP INDEX CONCURRENTLY IF EXISTS idx_papers_embedding;
