-- 요산 수치 로그 (민감정보, 별도 동의 후 저장)
CREATE TABLE uric_acid_logs (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    value NUMERIC(4, 1) NOT NULL,  -- 단위: mg/dL
    measured_at DATE NOT NULL,
    memo VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_uric_acid_logs_user_id ON uric_acid_logs(user_id);
CREATE INDEX idx_uric_acid_logs_measured_at ON uric_acid_logs(measured_at);

-- 통풍 발작 일지
CREATE TABLE gout_attack_logs (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    attacked_at DATE NOT NULL,
    pain_level SMALLINT CHECK (pain_level BETWEEN 1 AND 10),
    location VARCHAR(100),  -- 발작 부위 (엄지발가락, 발목 등)
    duration_days SMALLINT,
    suspected_cause VARCHAR(500),  -- 추정 원인 (음주, 식이 등)
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gout_attack_logs_user_id ON gout_attack_logs(user_id);

-- 복약 기록
CREATE TABLE medication_logs (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    medication_name VARCHAR(200) NOT NULL,
    dosage VARCHAR(100),
    taken_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_medication_logs_user_id ON medication_logs(user_id);
