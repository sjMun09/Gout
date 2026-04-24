-- 회원 탈퇴 Soft Delete 를 위한 status 컬럼 추가.
-- ACTIVE (기본값) / DELETED — CustomUserDetailsService 에서 DELETED 사용자는 인증 거부.

ALTER TABLE users
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

CREATE INDEX idx_users_status ON users(status);
