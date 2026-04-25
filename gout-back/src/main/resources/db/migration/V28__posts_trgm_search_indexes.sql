-- =============================================================================
-- V28: posts 전문 검색 — pg_trgm GIN 인덱스
-- =============================================================================
-- V27 의 [TODO] 항목 후속.
-- PostRepository.searchVisible 의 LOWER(title) LIKE '%kw%', LOWER(content) LIKE '%kw%'
-- 는 leading-wildcard 라 B-tree 로는 커버 불가 → pg_trgm GIN 으로 대체.
--
-- 의도적 설계 결정:
--   1) JPQL 변경 없음. LOWER(col) LIKE LOWER(...) 를 그대로 두고 인덱스만 추가한다.
--      → similarity() 로 바꾸면 짧은 한국어(2글자) 키워드가 기본 임계값 0.3 미만으로
--        떨어져 PostSearchIntegrationTest 가 깨진다. LIKE substring 의 의미를 보존하면서
--        GIN 으로 가속만 받는 편이 안전.
--   2) functional index — GIN (LOWER(col) gin_trgm_ops).
--      JPQL 이 LOWER 로 감싸므로, 인덱스도 LOWER 표현식 위에 만들어야 plan-time 매칭이 된다.
--      참고: V4/V5 의 idx_hospitals_name_trgm / idx_foods_name_trgm 은 LOWER 없이 만들어
--            져 있어 동일 패턴 LIKE LOWER 쿼리에서 인덱스를 못 타는 문제가 있다 (별도 수정 대상).
-- 작성일: 2026-04-25
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- idx_posts_title_trgm
--   근거: PostRepository.searchVisible — LOWER(p.title) LIKE LOWER(CONCAT('%', :kw, '%'))
--   기존: 없음. status/카테고리 인덱스(V27)는 텍스트 substring 매칭과 무관.
CREATE INDEX IF NOT EXISTS idx_posts_title_trgm
    ON posts USING GIN (LOWER(title) gin_trgm_ops);

-- idx_posts_content_trgm
--   근거: PostRepository.searchVisible — LOWER(p.content) LIKE LOWER(CONCAT('%', :kw, '%'))
--   주의: content 는 본문 전체(긴 텍스트)라 인덱스 사이즈가 크다.
--         배포 후 pg_relation_size('idx_posts_content_trgm') 로 모니터링 필요.
CREATE INDEX IF NOT EXISTS idx_posts_content_trgm
    ON posts USING GIN (LOWER(content) gin_trgm_ops);
