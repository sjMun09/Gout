# 다음 스텝 — 남은 개선·보강 과제

## 완료된 항목 (Agent-J, 2026-04-24)

본 PR (`chore/design-audit-and-polish`) 로 처리된 항목 — 아래 본문에서 해당 불릿은
실 merge 후에 제거 / 재작성할 것.

- [x] **설계 vs 실구현 Gap 감사 문서** 작성 (`docs/26-04-24_design-vs-implementation-audit.md`)
      — 코드 품질/보안 이슈 전수 추출 + P0/P1/P2 랭킹. §5 DevOps 의 "JaCoCo 추가 고려" 도 일부 착수.
- [x] **ERD 문서** 작성 (`docs/26-04-24_erd.md`) — Flyway V1~V9, V12 + 엔티티 전수 반영. Mermaid erDiagram.
- [x] **Springdoc OpenAPI** 통합 — `build.gradle.kts` 에 `springdoc-openapi-starter-webmvc-ui:3.0.3`
      추가 (Spring Boot 4.0.5 호환). `OpenApiWebSecurityCustomizer` 빈으로 `/swagger-ui/**`, `/v3/api-docs/**` 허용.
      사용법은 `docs/26-04-24_api-docs-guide.md`.
- [x] **JaCoCo 커버리지 리포트** — `./gradlew test` 후 `build/reports/jacoco/test/html/index.html`.
      임계치는 강제하지 않음 (통합테스트 다수 `@Disabled` 상태).
      → §5 "DevOps — JaCoCo" 플러그인 세팅 부분 완료, **CI 업로드 연결은 별도 PR 필요**.
- [x] **DB 백업 스크립트** — `scripts/backup-db.sh` (`pg_dump --clean --if-exists`, 30일 이상 자동 정리).
      → §5 "scripts/backup.sh 작성 필요" 해소.

### 다른 병렬 에이전트 PR 에서 처리될 것으로 예상되는 항목

(Agent-J 가 직접 확인한 범위 외. 실제 머지 후 교정 바람.)

- gender_type enum 버그 (§1.1), Food 검색 JPQL null 바인딩 (§1.2) — Agent-A/B 영역.
- SecurityConfig (§3 ADMIN 가드, CORS) — Agent-I 담당.
- PWA 아이콘 교체 (§4) — Agent-J 가 시도했으나 로컬에 ImageMagick 없음 → **SKIP**.
  별도로 디자이너 PNG 받아 `gout-front/public/icon-{192,512}.png`, `icon-maskable.png` 교체 필요.

> 아래는 **병합 전 원본 NEXT_STEPS 본문**. 실제 해결되면 체크 표기 후 정리할 것.

---

## 원본 — 남은 개선·보강 과제

> 2026-04-24 5개 병렬 에이전트 (PR #15~#19) 머지 후 식별된 잔여 이슈 목록.
> 현재 `docker compose up` 은 정상 기동하고 엔드포인트도 응답하지만,
> 일부 기능은 아래 이슈 때문에 통합 테스트가 `@Disabled` 처리되어 있다.

## 1. 치명 — 앱 기능 버그

### 1.1 `users.gender` 컬럼 ↔ Hibernate 타입 불일치

- **증상**: `POST /api/auth/register` 가 500 반환
  ```
  org.hibernate.exception.DataException:
    column "gender" is of type gender_type but expression is of type character varying
  ```
- **원인**:
  - Flyway V1 은 `gender` 컬럼을 PostgreSQL `gender_type` enum 으로 선언.
  - 엔티티(`User.gender`) 는 Java enum 이지만 Hibernate 7.2 기본 매핑은 VARCHAR 로 보냄.
  - Hibernate 는 PostgreSQL 커스텀 enum 을 자동 인식하지 못해 서버가 `VARCHAR` 로 BIND → PG 가 거부.
- **영향 테스트**: `AuthFlowIntegrationTest.register_*`, `registerAndLogin()` 의존 테스트 전부 (`Health/Post`).
- **해결 옵션**:
  1. (권장) 엔티티에 `@JdbcTypeCode(SqlTypes.NAMED_ENUM)` + `columnDefinition = "gender_type"` 지정.
  2. Flyway 마이그레이션으로 enum → VARCHAR + CHECK 제약으로 변경.
  3. 커스텀 `UserType` 작성(가장 노동 큼).
- **완료 판정**: 비활성화된 8개 테스트 중 register 계열 4개 + 의존 테스트 4개 `@Disabled` 제거 후 초록.

### 1.2 `FoodRepository.search` JPQL `LOWER(:keyword)` null 타입 추론 실패

- **증상**: `GET /api/foods?keyword=` 요청 시 500
  ```
  org.postgresql.util.PSQLException:
    ERROR: function lower(bytea) does not exist
  ```
- **원인**:
  - JPQL: `:keyword IS NULL OR LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))`.
  - JPA 가 null 바인딩을 내려보낼 때 PG 드라이버가 타입을 `bytea` 로 추론.
  - `LOWER(bytea)` 는 PG 에 존재하지 않음.
- **해결 옵션**:
  1. (권장) Repository 시그니처를 `String keyword` → 빈 문자열 기본값 + JPQL 에서 `LENGTH(:keyword) = 0 OR ...` 사용.
  2. QueryDSL / Specification 으로 조건부 where 로 재작성.
  3. `@Param` 대신 native query + `CAST(:keyword AS text)` 로 강제 캐스트.
- **영향 테스트**: `FoodIntegrationTest.search_foods`.

## 2. 기능적으로 비어있는 영역

| 항목 | 현재 상태 | 필요 작업 |
| --- | --- | --- |
| 실 병원 데이터 | 시드 35건 (카카오 API 가능 시 확장) | Kakao Local API 키 세팅 + 배치 크롤러. `.env` 에 `KAKAO_API_KEY` 추가 필요 |
| 알림 | 엔터티만 존재, 실제 발송 없음 | FCM 또는 이메일 어댑터 + 스케줄러 |
| 논문 크롤러 (#16) | 기동은 되지만 AI 요약/임베딩이 키 없으면 NOOP | `.env` 에 `ANTHROPIC_API_KEY`, `OPENAI_API_KEY` 세팅 + 프로덕션 cron 재검토 |
| 지도 | 카카오 맵 SDK key fallback 없음 | `NEXT_PUBLIC_KAKAO_MAP_KEY` 없을 때 UX fallback / 안내 문구 |

## 3. 테스트 인프라

- `ddl-auto: validate` 복원 — 현재 `none` 은 엔티티 메타데이터 전반 재검증을 회피한 상태.
  1.1 해결 후 `validate` 로 다시 돌려 통과시켜야 한다.
- Flyway baseline 정책 정리 — 로컬에서 V15 까지 한 번에 적용되면 시간이 꽤 걸림. 실데이터 량을 줄이거나 `test` 프로필에 경량 시드 세트를 분리할 수 있음.
- CI 에서 `docker build -t gout-test-postgis-pgvector:local` 이 매번 실행됨. GHCR 에 푸시 후 `pull` 하는 구조로 전환하면 러너 시간 단축.

## 4. 프론트엔드 잔여

- PWA manifest 아이콘은 실제 PNG 세트가 아닌 placeholder. 디자이너 아이콘 세트 확보 후 교체.
- `/profile` 페이지는 읽기 전용 — 비밀번호 변경/탈퇴 흐름 미구현.
- i18n 미적용 — 일부 텍스트가 한국어 하드코딩. `next-intl` 등 도입 고려.

## 5. DevOps

- `docker-compose.yml` 의 포트 매핑이 호스트 3000/8080 고정 → 동시에 다른 프로젝트 돌리기 불편. `.env` 로 offset 가능하게.
- Production compose (`docker-compose.prod.yml`) 의 DB 볼륨 백업 스크립트 부재. `scripts/backup.sh` 작성 필요.
- CI 에 코드 커버리지(JaCoCo) 와 스캔(trivy / hadolint) 추가 고려.

---

위 항목은 전부 후속 작업이며, 현재 커밋된 코드 자체는 **로컬 풀스택 기동**을 보장한다
(`docker compose up -d` → `/actuator/health` UP, `/api/foods/categories` 17건 반환).
