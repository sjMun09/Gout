CREATE TABLE hospitals (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    hira_code VARCHAR(20) UNIQUE,       -- 건강보험심사평가원 요양기관번호
    name VARCHAR(200) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(30),
    location geography(Point, 4326),    -- PostGIS 위치 (경도, 위도)
    departments TEXT[],                 -- 진료과목 배열
    operating_hours JSONB,              -- 영업시간
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 공간 인덱스 (반경 검색용)
CREATE INDEX idx_hospitals_location ON hospitals USING GIST(location);
CREATE INDEX idx_hospitals_hira_code ON hospitals(hira_code);
CREATE INDEX idx_hospitals_name_trgm ON hospitals USING GIN(name gin_trgm_ops);

-- 병원 리뷰 (편의성 후기, 치료효과 제외)
CREATE TYPE review_category AS ENUM ('WAITING', 'KINDNESS', 'EXPLANATION', 'FACILITY', 'PARKING', 'GENERAL');

CREATE TABLE hospital_reviews (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    hospital_id VARCHAR(36) NOT NULL REFERENCES hospitals(id) ON DELETE CASCADE,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating SMALLINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    category review_category NOT NULL DEFAULT 'GENERAL',
    content TEXT,
    visit_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',  -- VISIBLE, HIDDEN, REPORTED
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, hospital_id, visit_date)       -- 같은 날 중복 후기 방지
);

CREATE INDEX idx_hospital_reviews_hospital_id ON hospital_reviews(hospital_id);
CREATE INDEX idx_hospital_reviews_user_id ON hospital_reviews(user_id);
