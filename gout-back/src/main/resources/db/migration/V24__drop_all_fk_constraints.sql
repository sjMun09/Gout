-- ============================================================================
-- V24__drop_all_fk_constraints.sql
-- ----------------------------------------------------------------------------
-- 목적: DB 레벨 FOREIGN KEY 제약 15개를 일괄 제거한다. 참조 컬럼/인덱스/컬럼 타입은
--       그대로 유지하며, JOIN 절과 ORM 조회 경로에는 영향이 없다.
--
-- 배경 (docs/26-04-24_DB-PK-FK-재설계.md §2 "원칙"):
--   1) 샤딩/테넌트 분리 확장성 — 크로스 샤드 FK 는 불가. 애초에 걸지 않는 편이 낫다.
--   2) 마이크로서비스 분해 여지 — 커뮤니티/건강로그 도메인 분리 시 FK 는 강제로 끊어야 함.
--   3) 대량 삭제 lock 회피 — ON DELETE CASCADE 는 대형 테이블에서 트랜잭션을 길게 잡는다.
--      앱 레이어 명시 삭제(UserService.withdraw) + 소프트삭제/고아 정리 배치가 안정적.
--   4) 마이그레이션 유연성 — PK 타입 전환(Phase 2: VARCHAR → uuid v7) 시 FK 가 없으면
--      테이블 단위 독립 ALTER 가능.
--
-- 안전 가이드:
--   - DROP CONSTRAINT 은 ACCESS EXCLUSIVE lock 을 짧게 잡는다. 빈/작은 테이블은 ms 단위.
--   - Flyway 는 SQL 한 문을 트랜잭션으로 감싸므로 중간 실패 시 자동 롤백된다.
--   - IF EXISTS 를 사용해 dev/QA/prod 환경에서 제약이 이미 없어도(재적용/스냅샷 차이)
--     에러 없이 진행되도록 한다.
--   - 본 파일 적용 이전에 반드시 pg_dump 백업을 받아두고, UserServiceImpl.withdraw 의
--     앱 레벨 cascade 로직이 먼저 배포되어 있어야 한다(고아 방지).
--
-- 참조 무결성 대체:
--   - users 탈퇴: UserServiceImpl.withdraw() 에서 단일 @Transactional 로 자식 row 처리
--     (건강 로그/북마크/좋아요/알림 물리 삭제, hospital_reviews/reports.user_id → NULL,
--     posts/comments 는 user_id 유지 + 응답 DTO 에서 "탈퇴한 사용자" 치환).
--   - posts 삭제: PostServiceImpl.deletePost 에서 comments/post_likes/post_bookmarks 수동 정리.
--   - hospitals 삭제: Hospital.is_active=false 소프트 삭제 + 조회 시 필터.
--
-- 롤백: V99__restore_fks.sql 를 신규 작성해야 한다 (Flyway CE 는 undo 미지원).
--       §8.2 롤백 스켈레톤 참고. 롤백 전에 반드시 고아 row 정리 후 NOT VALID → VALIDATE.
-- ============================================================================

-- 1. 건강 로그 3종 (users 참조) — V3__create_health_profiles.sql
ALTER TABLE uric_acid_logs   DROP CONSTRAINT IF EXISTS uric_acid_logs_user_id_fkey;
ALTER TABLE gout_attack_logs DROP CONSTRAINT IF EXISTS gout_attack_logs_user_id_fkey;
ALTER TABLE medication_logs  DROP CONSTRAINT IF EXISTS medication_logs_user_id_fkey;

-- 2. 병원 리뷰 — V4__create_hospitals.sql
ALTER TABLE hospital_reviews DROP CONSTRAINT IF EXISTS hospital_reviews_hospital_id_fkey;
ALTER TABLE hospital_reviews DROP CONSTRAINT IF EXISTS hospital_reviews_user_id_fkey;

-- 3. 커뮤니티 (게시글 / 댓글 / 좋아요) — V8__create_posts.sql
ALTER TABLE posts      DROP CONSTRAINT IF EXISTS posts_user_id_fkey;
ALTER TABLE comments   DROP CONSTRAINT IF EXISTS comments_post_id_fkey;
ALTER TABLE comments   DROP CONSTRAINT IF EXISTS comments_user_id_fkey;
ALTER TABLE comments   DROP CONSTRAINT IF EXISTS comments_parent_id_fkey; -- self-reference
ALTER TABLE post_likes DROP CONSTRAINT IF EXISTS post_likes_post_id_fkey;
ALTER TABLE post_likes DROP CONSTRAINT IF EXISTS post_likes_user_id_fkey;

-- 4. 북마크 / 신고 / 알림 — V20, V21, V22
ALTER TABLE post_bookmarks DROP CONSTRAINT IF EXISTS post_bookmarks_user_id_fkey;
ALTER TABLE post_bookmarks DROP CONSTRAINT IF EXISTS post_bookmarks_post_id_fkey;
ALTER TABLE reports        DROP CONSTRAINT IF EXISTS reports_reporter_id_fkey;
ALTER TABLE notifications  DROP CONSTRAINT IF EXISTS notifications_user_id_fkey;

-- ----------------------------------------------------------------------------
-- NOT NULL 완화: 유저 탈퇴 시 감사/리뷰 보존을 위해 user_id 를 NULL 로 치환해야 하는 컬럼.
--   - hospital_reviews.user_id : 리뷰 콘텐츠는 남기고 작성자를 "탈퇴한 사용자" 로 표기
--   - reports.reporter_id       : 감사 목적상 신고 행위는 유지하되 신고자 익명화
-- ----------------------------------------------------------------------------
ALTER TABLE hospital_reviews ALTER COLUMN user_id     DROP NOT NULL;
ALTER TABLE reports          ALTER COLUMN reporter_id DROP NOT NULL;

-- ----------------------------------------------------------------------------
-- 참고 (삭제되지 않은 보조 인덱스 — 쿼리 성능상 반드시 유지되어야 함):
--   idx_uric_acid_logs_user_id, idx_gout_attack_logs_user_id, idx_medication_logs_user_id,
--   idx_hospital_reviews_hospital_id, idx_hospital_reviews_user_id,
--   idx_posts_user_id, idx_comments_post_id, idx_comments_parent_id,
--   idx_post_bookmarks_user_created, idx_post_bookmarks_post_id,
--   idx_notifications_user_unread, idx_reports_status_created, idx_reports_target
-- post_likes 는 PK(post_id, user_id) 가 복합인덱스 역할을 한다.
-- ============================================================================
