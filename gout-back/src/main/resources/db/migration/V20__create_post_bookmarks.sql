-- 게시글 북마크(스크랩)
-- ID 타입은 기존 posts/users 테이블 규약(VARCHAR(36))을 따른다.
-- 같은 사용자-게시글 조합은 한 번만 가능하며, 내 북마크 목록은 최신순으로 조회된다.

CREATE TABLE post_bookmarks (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    post_id VARCHAR(36) NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, post_id)
);

CREATE INDEX idx_post_bookmarks_user_created
    ON post_bookmarks(user_id, created_at DESC);

CREATE INDEX idx_post_bookmarks_post_id
    ON post_bookmarks(post_id);
