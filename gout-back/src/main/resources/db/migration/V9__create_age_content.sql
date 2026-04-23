-- 연령대별 콘텐츠 (20대~70대+)
CREATE TYPE age_group AS ENUM ('TWENTIES', 'THIRTIES', 'FORTIES', 'FIFTIES', 'SIXTIES', 'SEVENTIES_PLUS');

CREATE TABLE age_group_contents (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    age_group age_group NOT NULL,
    title VARCHAR(500) NOT NULL,
    characteristics TEXT NOT NULL,     -- 이 연령대 통풍 특징
    main_causes TEXT NOT NULL,         -- 주요 원인
    warnings TEXT NOT NULL,            -- 주의사항
    management_tips TEXT NOT NULL,     -- 관리 방법
    evidence_source VARCHAR(500),
    evidence_doi VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_age_group_contents_age ON age_group_contents(age_group);
