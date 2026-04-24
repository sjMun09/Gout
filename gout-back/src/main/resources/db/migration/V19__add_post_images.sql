-- 게시글 이미지 URL 목록 컬럼 추가.
-- 여러 장 첨부를 단순히 지원하기 위해 TEXT[] (Postgres 배열) 사용.
-- 저장되는 값은 백엔드가 반환하는 상대 URL (예: /api/uploads/posts/abc123.jpg).
-- NULL 대신 빈 배열을 기본값으로 두어 엔티티 매핑 시 null 체크 부담을 덜어준다.

ALTER TABLE posts
    ADD COLUMN image_urls TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[];
