# papers.embedding pgvector 인덱스 ivfflat → HNSW 전환 기록

작성일: 2026-04-24
관련 마이그레이션: `gout-back/src/main/resources/db/migration/V25__papers_embedding_hnsw.sql`

## 왜 전환했는가 (ivfflat vs HNSW)

| 항목 | ivfflat | HNSW |
|---|---|---|
| 인덱스 구조 | k-means 기반 파티션(list) + 평탄 탐색 | 멀티레이어 그래프 |
| 빌드 시 학습 데이터 | 필요 (lists 품질이 학습 샘플에 의존) | 불필요 |
| 증분 INSERT | 학습 시점 이후 데이터가 많아지면 리콜 저하 → 재빌드 필요 | 그래프에 바로 확장, 품질 유지 |
| 레이턴시 / 리콜 | 파라미터 튜닝(probes) 에 민감 | 일반적으로 같은 리콜 대비 더 낮은 레이턴시 |
| 메모리 | 적음 | 더 큼 (그래프 링크 저장) |

Gout 의 `papers` 테이블은 주간 크롤러로 점진 적재되고 규모는 1K~10K 수준이다.
이 조합에서 HNSW 의 "학습 없이 증분에 강함" 특성이 명백히 유리하며, 메모리 비용도 수 MB 수준으로 무시 가능하다.

## 파라미터와 기본값

V25 에서 쓰는 값:

```sql
CREATE INDEX CONCURRENTLY idx_papers_embedding
    ON papers USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);
```

- `m = 16` (pgvector 기본) — 노드당 연결 수. 높일수록 리콜↑ 메모리↑ 빌드↑.
- `ef_construction = 64` (pgvector 기본) — 빌드 탐색 폭. 높일수록 리콜↑ 빌드 시간↑.
- 쿼리 시 `SET LOCAL hnsw.ef_search = 40` (기본값 40) — 탐색 폭. 리콜 vs 레이턴시 트레이드오프.

### 튜닝 순서 제안

1. 기본값으로 배포 후 `PapersEmbeddingPerfTest` 로 baseline 측정.
2. 리콜이 부족하면: `SET hnsw.ef_search` 80 → 120 순으로 단계 상향. 인덱스 재빌드 없이 조정 가능.
3. 그래도 부족하면: 인덱스 재빌드 (`m = 24, ef_construction = 128`).
4. 레이턴시가 과하면: `ef_search` 낮춤 또는 `m` 유지한 채 `ef_construction` 올린 인덱스로 리빌드.

## pgvector 버전 확인 결과

- Docker 이미지: `pgvector/pgvector:pg17` (프로덕션/로컬/테스트 공통, `docker/postgres.Dockerfile`).
- 해당 이미지 내 `vector.control` 의 `default_version` = **0.8.2** → HNSW 요구사항(0.5.0+) 충족.
- 운영 DB 에서 직접 확인하려면:
  ```sql
  SELECT extversion FROM pg_extension WHERE extname = 'vector';
  ```

## 트랜잭션 처리 방식

`CREATE INDEX CONCURRENTLY` 는 트랜잭션 블록 안에서 실행할 수 없다.
Flyway 11 (이 프로젝트 runtime) 의 스크립트 레벨 지시자로 V25 만 트랜잭션 밖에서 실행한다:

- V25 상단 주석: `-- flyway:executeInTransaction=false`
- `application.yml` (dev/test/prod 전부): `spring.flyway.mixed: true`

이 조합으로 트랜잭션/논트랜잭션 스크립트가 한 실행에 섞여 있어도 Flyway 가 거부하지 않는다.

## 배포 체크리스트

1. 배포 전
   - [ ] 운영 DB 의 pgvector 버전 확인 (0.5.0+).
   - [ ] `papers` 행 수 확인 (`SELECT count(*) FROM papers`). 수만~수십만 행이면 빌드 시간 여유 두기.
2. 배포 시
   - [ ] Flyway 가 V25 실행 → `DROP INDEX CONCURRENTLY` → `CREATE INDEX CONCURRENTLY`.
   - [ ] DROP ~ CREATE 사이 짧은 구간에 `papers.embedding <=>` 쿼리의 순차 스캔 발생 가능. 1K~10K 규모에서는 무시 수준.
   - [ ] 빌드 중 신규 INSERT 는 인덱스에 늦게 반영될 수 있음 (CONCURRENTLY 특성).
3. 배포 후
   - [ ] `SELECT indexname, indexdef FROM pg_indexes WHERE tablename = 'papers';` 로 HNSW 확인.
   - [ ] `pg_size_pretty(pg_relation_size('idx_papers_embedding'))` 로 인덱스 사이즈 기록.
   - [ ] `EXPLAIN ANALYZE SELECT ... ORDER BY embedding <=> ... LIMIT 10` 로 Index Scan 사용 확인.

## 롤백

되돌리려면 `V26__papers_embedding_revert_to_ivfflat.sql` 로 역마이그레이션:

```sql
-- flyway:executeInTransaction=false
DROP INDEX CONCURRENTLY IF EXISTS idx_papers_embedding;
CREATE INDEX CONCURRENTLY idx_papers_embedding
    ON papers USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
```

(현 시점에는 생성하지 않고, 실제 롤백이 필요해졌을 때 추가.)

## 운영 관측 메트릭

- `idx_papers_embedding` 인덱스 사이즈 (`pg_relation_size`)
- `papers` 행 수 대비 인덱스 사이즈 (선형에 가깝게 증가해야 정상)
- 유사 논문 추천 엔드포인트 (`PaperServiceImpl.findSimilar`) p95 레이턴시
- 실패율 / 타임아웃 (HNSW 는 매우 안정적이어야 함)
