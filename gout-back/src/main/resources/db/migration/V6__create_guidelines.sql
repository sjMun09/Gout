CREATE TYPE guideline_type AS ENUM ('DO', 'DONT');
CREATE TYPE guideline_category AS ENUM ('FOOD', 'EXERCISE', 'MEDICATION', 'LIFESTYLE', 'EMERGENCY');
CREATE TYPE evidence_strength AS ENUM ('STRONG', 'MODERATE', 'WEAK');
-- STRONG = 가이드라인/메타분석, MODERATE = RCT, WEAK = 관찰연구

CREATE TABLE guidelines (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    type guideline_type NOT NULL,
    category guideline_category NOT NULL,
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    evidence_strength evidence_strength NOT NULL DEFAULT 'MODERATE',
    evidence_source VARCHAR(500),       -- "2020 ACR Guidelines" 등
    evidence_doi VARCHAR(200),          -- DOI 링크
    target_age_groups TEXT[],           -- 적용 연령대: ['20s','30s','all']
    is_published BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_guidelines_type ON guidelines(type);
CREATE INDEX idx_guidelines_category ON guidelines(category);
CREATE INDEX idx_guidelines_target_age ON guidelines USING GIN(target_age_groups);
