CREATE TYPE post_category AS ENUM (
    'HOSPITAL_REVIEW',    -- 병원 경험
    'FOOD_EXPERIENCE',    -- 음식/식단 경험
    'EXERCISE',           -- 운동 경험
    'MEDICATION',         -- 약물 경험
    'QUESTION',           -- 질문
    'SUCCESS_STORY',      -- 관리 성공담
    'FREE'                -- 자유
);

CREATE TABLE posts (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category post_category NOT NULL DEFAULT 'FREE',
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    view_count INTEGER NOT NULL DEFAULT 0,
    like_count INTEGER NOT NULL DEFAULT 0,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',  -- VISIBLE, HIDDEN, DELETED
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_posts_category ON posts(category);
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);

CREATE TABLE comments (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    post_id VARCHAR(36) NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id VARCHAR(36) REFERENCES comments(id) ON DELETE CASCADE,  -- 대댓글
    content TEXT NOT NULL,
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_id);

-- 좋아요
CREATE TABLE post_likes (
    post_id VARCHAR(36) NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (post_id, user_id)
);
