-- 게시글/댓글 신고 테이블
-- 스펙: 같은 사용자가 같은 대상을 중복 신고할 수 없도록 UNIQUE 제약
-- target_type/reason 값은 애플리케이션 레벨에서 검증 (유연성 유지)
-- NOTE: 기존 users/posts/comments 테이블이 VARCHAR(36) id 를 사용하므로 동일 타입으로 맞춘다.
--       (task 샘플 SQL 의 UUID 컬럼 타입은 기존 FK 와 호환되지 않아 VARCHAR(36) 으로 조정)

CREATE TABLE reports (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    target_type VARCHAR(20) NOT NULL,                                   -- POST or COMMENT
    target_id VARCHAR(36) NOT NULL,
    reporter_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(50) NOT NULL,                                        -- SPAM, ABUSE, SEXUAL, MISINFO, ETC
    detail TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',                      -- PENDING, RESOLVED, DISMISSED
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    resolved_at TIMESTAMP,
    UNIQUE (target_type, target_id, reporter_id)
);

CREATE INDEX idx_reports_status_created ON reports(status, created_at DESC);
CREATE INDEX idx_reports_target ON reports(target_type, target_id);
