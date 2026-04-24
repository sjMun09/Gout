-- =============================================================================
-- V27: 쿼리 패턴 기반 인덱스 최적화
-- =============================================================================
-- 분석 대상: gout-back/src/main/java/com/gout/dao/ 전체 Repository
-- 작성일: 2026-04-25
-- =============================================================================

-- -----------------------------------------------------------------------------
-- [추가] posts — 피드/트렌딩 쿼리 커버 복합 인덱스
-- -----------------------------------------------------------------------------

-- idx_posts_status_created_at
--   근거: PostRepository.findVisible
--           WHERE status = 'VISIBLE' [AND category = ?] ORDER BY createdAt DESC
--         PostRepository.searchVisible
--           WHERE status = 'VISIBLE' [AND category = ?] AND (LIKE ...) + Pageable 정렬
--         PostRepository.incrementViewCount
--           WHERE id = :id AND status = 'VISIBLE'  (id=PK, status 조건만 추가)
--   기존: idx_posts_created_at (created_at DESC 단독) + idx_posts_category (category 단독)
--         → status 조건을 선행 컬럼으로 묶은 복합 인덱스가 없어 status 필터링 후 정렬에 seq scan 발생
CREATE INDEX IF NOT EXISTS idx_posts_status_created_at
    ON posts (status, created_at DESC);

-- idx_posts_status_like_count
--   근거: PostRepository.findTrending
--           WHERE status = 'VISIBLE' AND createdAt >= :since
--           ORDER BY likeCount DESC, createdAt DESC
--         PostRepository.searchVisible (Pageable sort = likeCount DESC)
--   기존: 없음
--   note: createdAt DESC 는 like_count DESC 와 함께 tie-break 로 쓰이므로
--         (status, like_count DESC, created_at DESC) 3-column으로 구성하여
--         ORDER BY like_count DESC, created_at DESC 를 인덱스 정렬로 커버
CREATE INDEX IF NOT EXISTS idx_posts_status_like_count
    ON posts (status, like_count DESC, created_at DESC);

-- idx_posts_status_view_count
--   근거: PostRepository.searchVisible (Pageable sort = viewCount DESC)
--   기존: 없음
CREATE INDEX IF NOT EXISTS idx_posts_status_view_count
    ON posts (status, view_count DESC);

-- idx_posts_category_status
--   근거: findVisible / searchVisible 에서 category 필터 + status = 'VISIBLE' 조합
--         idx_posts_category (category 단독) 는 status 조건을 커버 못 함
--   기존: idx_posts_category (category 단독) — 부분 대체
CREATE INDEX IF NOT EXISTS idx_posts_category_status
    ON posts (category, status);

-- -----------------------------------------------------------------------------
-- [추가] comments — 댓글 목록 / 집계 복합 인덱스
-- -----------------------------------------------------------------------------

-- idx_comments_post_status_created
--   근거: CommentRepository.findByPostIdAndStatusOrderByCreatedAtAsc
--           WHERE post_id = ? AND status = ? ORDER BY created_at ASC
--         CommentRepository.countByPostIdAndStatus
--           WHERE post_id = ? AND status = ?
--         CommentRepository.countByPostIdInAndStatusGroupByPostId
--           WHERE post_id IN (...) AND status = ? GROUP BY post_id
--   기존: idx_comments_post_id (post_id 단독) — status/created_at 미포함
CREATE INDEX IF NOT EXISTS idx_comments_post_status_created
    ON comments (post_id, status, created_at ASC);

-- -----------------------------------------------------------------------------
-- [추가] post_likes — userId 기준 삭제 쿼리 최적화
-- -----------------------------------------------------------------------------

-- idx_post_likes_user_id
--   근거: PostLikeRepository.deleteByUserId
--           DELETE FROM PostLike WHERE id.userId = :userId
--         PK는 (post_id, user_id) → user_id 선행 탐색 인덱스 없음 → seq scan
--   기존: PK (post_id, user_id) 만 존재
CREATE INDEX IF NOT EXISTS idx_post_likes_user_id
    ON post_likes (user_id);

-- -----------------------------------------------------------------------------
-- [추가] health logs — user_id + 날짜 복합 인덱스 (조회 패턴 최적화)
-- -----------------------------------------------------------------------------

-- idx_uric_acid_logs_user_measured
--   근거: UricAcidLogRepository.findByUserIdOrderByMeasuredAtDesc
--           WHERE user_id = ? ORDER BY measured_at DESC
--   기존: idx_uric_acid_logs_user_id (user_id 단독) +
--          idx_uric_acid_logs_measured_at (measured_at 단독) — 별개 인덱스로 조합 미지원
CREATE INDEX IF NOT EXISTS idx_uric_acid_logs_user_measured
    ON uric_acid_logs (user_id, measured_at DESC);

-- idx_gout_attack_logs_user_attacked
--   근거: GoutAttackLogRepository.findByUserIdOrderByAttackedAtDesc
--           WHERE user_id = ? ORDER BY attacked_at DESC
--   기존: idx_gout_attack_logs_user_id (user_id 단독) — ORDER BY 미커버
CREATE INDEX IF NOT EXISTS idx_gout_attack_logs_user_attacked
    ON gout_attack_logs (user_id, attacked_at DESC);

-- idx_medication_logs_user_taken
--   근거: MedicationLogRepository.findByUserIdOrderByTakenAtDesc
--           WHERE user_id = ? ORDER BY taken_at DESC
--   기존: idx_medication_logs_user_id (user_id 단독) — ORDER BY 미커버
CREATE INDEX IF NOT EXISTS idx_medication_logs_user_taken
    ON medication_logs (user_id, taken_at DESC);

-- -----------------------------------------------------------------------------
-- [추가] hospital_reviews — 조회 복합 인덱스
-- -----------------------------------------------------------------------------

-- idx_hospital_reviews_hospital_status
--   근거: HospitalReviewRepository.findByHospitalIdAndStatus
--           WHERE hospital_id = ? AND status = ? + Pageable
--   기존: idx_hospital_reviews_hospital_id (hospital_id 단독) — status 미포함
CREATE INDEX IF NOT EXISTS idx_hospital_reviews_hospital_status
    ON hospital_reviews (hospital_id, status);

-- -----------------------------------------------------------------------------
-- [추가] reports — reporter_id 기준 익명화 쿼리 최적화
-- -----------------------------------------------------------------------------

-- idx_reports_reporter_id
--   근거: ReportRepository.anonymizeByReporterId
--           UPDATE reports SET reporter_id = NULL WHERE reporter_id = :userId
--         UNIQUE (target_type, target_id, reporter_id) 가 존재하나 선행 컬럼이 reporter_id 아님
--   기존: UNIQUE (target_type, target_id, reporter_id) — reporter_id 가 3번째 컬럼이라 단독 탐색 비효율
CREATE INDEX IF NOT EXISTS idx_reports_reporter_id
    ON reports (reporter_id);

-- -----------------------------------------------------------------------------
-- [제거] users — UNIQUE 컬럼 중복 인덱스
-- -----------------------------------------------------------------------------

-- V2__create_users.sql 에서 아래 두 인덱스가 명시적으로 생성되었으나,
-- users.email 과 users.kakao_id 는 이미 UNIQUE 제약이 걸려 있어
-- PostgreSQL이 자동으로 B-tree 인덱스를 생성한다.
-- → 중복 인덱스를 제거해 쓰기 오버헤드와 스토리지 낭비를 줄인다.
DROP INDEX IF EXISTS idx_users_email;
DROP INDEX IF EXISTS idx_users_kakao_id;

-- -----------------------------------------------------------------------------
-- [TODO] 전문 검색 — B-tree 인덱스로 커버 불가 항목
-- -----------------------------------------------------------------------------
-- PostRepository.searchVisible: LOWER(title) LIKE '%keyword%', LOWER(content) LIKE '%keyword%'
--   → leading-wildcard LIKE 는 B-tree 인덱스로 커버 불가.
--   → pg_trgm GIN 인덱스 또는 PostgreSQL full-text search (tsvector) 로 대체 필요.
--   → 이번 마이그레이션에서는 제외. 별도 V28 에서 tsvector 컬럼 + GIN 인덱스 추가 예정.
--
-- FoodRepository.search: LOWER(name) LIKE '%keyword%', LOWER(nameEn) LIKE '%keyword%'
--   → foods.name 에 idx_foods_name_trgm (GIN, pg_trgm) 이미 존재 (V5).
--   → name_en 컬럼에 대한 trgm 인덱스는 별도 검토 필요.
