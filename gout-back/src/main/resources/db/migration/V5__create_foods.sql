CREATE TYPE purine_level AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH');
CREATE TYPE food_recommendation AS ENUM ('GOOD', 'MODERATE', 'BAD', 'AVOID');

CREATE TABLE foods (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    name VARCHAR(200) NOT NULL,
    name_en VARCHAR(200),
    category VARCHAR(100),              -- 육류, 해산물, 채소, 음료 등
    purine_content NUMERIC(6, 1),       -- 퓨린 함량 (mg/100g)
    purine_level purine_level NOT NULL,
    recommendation food_recommendation NOT NULL,
    description TEXT,
    caution TEXT,                       -- 주의사항
    evidence_notes TEXT,                -- 근거 설명
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_foods_name_trgm ON foods USING GIN(name gin_trgm_ops);
CREATE INDEX idx_foods_purine_level ON foods(purine_level);
CREATE INDEX idx_foods_recommendation ON foods(recommendation);
CREATE INDEX idx_foods_category ON foods(category);
