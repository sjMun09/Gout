-- 인앱 알림 테이블
-- 기존 users.id / posts.id 가 VARCHAR(36) 이므로 user_id 도 VARCHAR(36) 으로 맞춘다.
-- (태스크 명세의 UUID 컬럼은 스키마 컨벤션에 맞춰 조정)

CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL
        CHECK (type IN ('COMMENT_ON_POST', 'REPLY_ON_COMMENT', 'POST_LIKE')),
    title VARCHAR(200) NOT NULL,
    body TEXT,
    link VARCHAR(300),
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 미읽음 우선 + 최신순 조회 최적화
CREATE INDEX idx_notifications_user_unread
    ON notifications(user_id, read_at NULLS FIRST, created_at DESC);
