CREATE TABLE papers (
    id VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid()::text,
    pmid VARCHAR(20) UNIQUE,            -- PubMed ID
    doi VARCHAR(300) UNIQUE,
    title TEXT NOT NULL,
    abstract_en TEXT,
    abstract_ko TEXT,                   -- 한국어 번역 (선택)
    ai_summary_ko TEXT,                 -- AI 생성 요약 (Claude)
    authors TEXT[],
    journal_name VARCHAR(500),
    published_at DATE,
    source_url VARCHAR(500),            -- 원문 URL (저장 X, 링크만)
    embedding vector(1536),             -- pgvector (OpenAI/Claude 임베딩)
    category VARCHAR(100),              -- 'food', 'exercise', 'medication' 등
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_papers_doi ON papers(doi);
CREATE INDEX idx_papers_pmid ON papers(pmid);
CREATE INDEX idx_papers_embedding ON papers USING ivfflat(embedding vector_cosine_ops) WITH (lists = 100);
CREATE INDEX idx_papers_category ON papers(category);
