# 설계 vs 실구현 Gap 감사 (2026-04-24)

> Agent-J 가 `chore/design-audit-and-polish` 브랜치에서 수행.
> 기준: `README.md` (주요 기능 · 프로젝트 구조 · Flyway V1~V17),
>       `docs/NEXT_STEPS.md` (5개 병렬 에이전트 #15~#19 병합 이후 이슈),
>       실제 `gout-back` / `gout-front` 소스.
> 목적: README 가 약속하는 기능을 소스가 얼마나 구현했는지 정직한 표로 정리하고,
>       코드 품질/보안 관점에서 발견된 모든 이슈를 우선순위로 제시.

---

## 1. README 기능 vs 실제 구현 상태

| README 기능 | 실제 구현 상태 | Gap |
|------------|---------------|-----|
| **음식 퓨린 검색** (빨강/노랑/초록 신호등) | `GET /api/foods`, `GET /api/foods/categories`, `GET /api/foods/{id}` + 40종+ 시드 (V10, V13) 존재. `purine_level` enum (LOW/MEDIUM/HIGH/VERY_HIGH) + `recommendation` 보유. | **P0 버그**: `FoodRepository.search` JPQL 의 `LOWER(:keyword)` 바인딩이 null 일 때 PG 드라이버가 `bytea` 로 추론 → 500. `@Disabled FoodIntegrationTest.search_foods`. `keyword` 없이 호출 시 전수 장애. |
| **병원 찾기** (위치 기반 류마티스내과) | `GET /api/hospitals`(+lat/lng/radius), `/{id}`, `/{id}/reviews`, `POST /{id}/reviews`. `hospitals.location geography(Point, 4326)` + PostGIS `ST_DWithin` native query 구현. V15 로 35건 시드. | README 는 "카카오맵 API" 로 암시하지만 **백엔드는 PostGIS 고정**. 프론트(`useKakaoMap`)만 카카오맵 SDK 사용. 배치 크롤러 부재 (`docs/NEXT_STEPS.md §2`). `NEXT_PUBLIC_KAKAO_MAP_KEY` 미설정 시 지도 렌더 실패 + UX fallback 없음. |
| **커뮤니티** (병원 경험·식단·발작 자유게시판) | `/api/posts` CRUD + 좋아요 토글, `/api/posts/{id}/comments` CRUD(대댓글 `parent_id`). 7개 카테고리 enum(HOSPITAL_REVIEW, FOOD_EXPERIENCE, EXERCISE, MEDICATION, QUESTION, SUCCESS_STORY, FREE). | **P1 성능**: 게시글 목록 조회 시 게시글별 `commentRepository.countByPostIdAndStatus()` 루프 → **N+1** (`PostServiceImpl.java:54`). 신고/숨김 흐름 미구현 (컬럼만 존재). |
| **근거 기반 가이드** (ACR/EULAR/KCR) | `GET /api/guidelines?category=&type=`. V11·V14 시드 22건+ (DOI 포함). 5개 카테고리 × 2 type(DO/DONT) × 근거 강도(STRONG/MODERATE/WEAK). | 가이드라인 작성 UI/관리자 플로우 없음 — 시드만 가능. `target_age_groups` 는 컬럼 존재하지만 검색 조건엔 미노출. |
| **연령별 정보** (20대~70대 이상) | `GET /api/content/age-group`, `/{group}`. V16 로 6개 시드 전부 존재. | 프론트 `/age-info` 라우트 존재. Gap 없음. |
| **운동 가이드** | `/api/guidelines?category=EXERCISE` 로 간접 제공 + 프론트 `/exercise` 페이지. | 운동별 전용 엔티티/엔드포인트는 없음. Guideline 에 묻어서 노출. README 문구 대비 설계상 축소된 상태이나 기능상 커버됨. |
| **논문 요약** (PubMed + AI) | `GET /api/papers`, `/{id}`, `/{id}/similar` + `PaperCrawlerJob` (`@Scheduled 03:07`) + `PaperAiService`/`PaperEmbeddingService` + V17 시드. | **키 없으면 NOOP** (`docs/NEXT_STEPS.md §2` 명시). `ANTHROPIC_API_KEY`/`OPENAI_API_KEY` 미설정 시 크롤링은 되지만 `abstract_ko`/`ai_summary_ko`/`embedding` 채워지지 않음. `/similar` 엔드포인트는 embedding 없는 논문에 대해 빈 배열 반환하므로 API 자체는 안전. |
| **응급 가이드** | `/api/guidelines?category=EMERGENCY` + 프론트 `/emergency` 페이지. | 전용 엔티티 없음. Guideline 로 포괄. 기능은 제공됨. |

### 기타 엔드포인트 (README 미언급, 실구현 존재)

| 엔드포인트 | 설명 |
|-----------|------|
| `POST /api/auth/register\|login\|refresh\|logout` | JWT access(15분)/refresh(7일) 발급. **현재 `register` 는 `gender_type` enum 버그로 500**. |
| `GET/POST/DELETE /api/health/{uric-acid\|gout-attack\|medication}-logs` | 개인 건강기록. JWT 필요. |
| `POST /api/admin/papers/crawl\|{id}/embed\|{id}/summarize` | 관리자 트리거. **권한 검사 누락** — 아래 §3 참조. |

### 프론트엔드 라우트 (`gout-front/src/app`)

| 경로 | 상태 |
|------|------|
| `(auth)/login`, `(auth)/register` | 존재. `register` 는 백엔드 500 이슈 때문에 실 동작 불가. |
| `(main)/home`, `/food`, `/record`, `/hospital`, `/more`, `/profile` | README 의 5개 탭 + 프로필 구현. |
| `(main)/community`, `community/[id]`, `community/write` | 구현. |
| `(main)/age-info`, `/encyclopedia`, `/emergency`, `/exercise`, `/research` | 보조 콘텐츠 페이지 전부 존재. |

**프론트가 호출하는데 백엔드에 없는 API**: `GET/PUT /api/users/me` (`gout-front/src/lib/api.ts:264`, `profile/page.tsx:92`).
프론트는 404/401 시 localStorage 폴백으로 안전하게 동작하지만 **서버 저장은 동작하지 않음**.

---

## 2. 코드 품질 이슈

### 2.1 N+1 / 쿼리 효율

- **`PostServiceImpl.getPosts` 루프 내 `countByPostIdAndStatus`** — `gout-back/src/main/java/com/gout/service/impl/PostServiceImpl.java:54`.
  페이지 크기 20 이면 1(post list) + 1(user batch) + **20(count)** = 22 쿼리.
  해결: `Comment` 에 `@Query` 로 `post_id IN (...)` group count 배치 추가 후 Map 조회.
- **`PaperServiceImpl.findSimilar` 수동 정렬** — `gout-back/.../PaperServiceImpl.java:85-91`.
  `findAllById` 결과를 `List.stream().filter().findFirst()` 로 재정렬 → O(n²). 논문 수가 적어 현재는 무해하나
  limit 이 50 까지 허용되므로 Map 기반 정렬로 재작성 권장.
- **`HospitalServiceImpl.search`** — 위치 검색 경로에서 `searchByLocation` + `countByLocation` 을 개별 쿼리로 2번 실행. PG 에서 같은 predicate 를 두번 돌리므로 `COUNT(*) OVER()` 윈도우 집계로 합치면 1회로 줄일 수 있음.

### 2.2 예외 처리 / 에러 매핑

- **`GlobalExceptionHandler` 의 `Exception.class` 캐치-올** — `gout-back/.../GlobalExceptionHandler.java:36-42` —
  Spring Security `AccessDeniedException` / `AuthenticationException` 도 여기로 떨어져 **500** 으로 포장됨.
  `PostController.getCurrentUserId()` 가 던지는 `AccessDeniedException` 이 401/403 이 아닌 500 반환 가능성.
  `@ExceptionHandler(AccessDeniedException.class)` / `AuthenticationException.class` 별도 매핑 필요.
- `JwtAuthenticationFilter.doFilterInternal` 에 토큰이 유효하지만 해당 user id 가 DB 에 없을 때 `loadUserByUsername` 이 `UsernameNotFoundException` 던짐 → 필터체인에서 그대로 500 응답. 필터 내 try/catch 로 401 변환 필요.

### 2.3 하드코딩 / 매직값

- `PostServiceImpl` / `CommentServiceImpl` 전반에서 **status 값을 문자열 리터럴 `"VISIBLE"` / `"DELETED"` 로 비교** (`PostServiceImpl.java:54, 66, 73, 119, 126, 163`, `CommentServiceImpl.java:37, 42, 61, 71`).
  Post 엔티티에 enum 으로 만들거나 public static 상수화 필요.
- `HospitalServiceImpl.createReview` 의 `status("VISIBLE")` 도 동일 (`HospitalServiceImpl.java:97`).
- `loadNicknames` 실패 시 반환하는 기본 문자열 `"알 수 없음"` 이 서비스 3곳에 반복 (`PostServiceImpl`, `CommentServiceImpl`). 상수화 권장.
- `application.yml:23` 의 JWT secret 기본값이 **커밋된 평문** — 개발환경이지만 README 에 "production 에서 반드시 override" 강조 문구 필요.
- `docker-compose.yml:11` 의 `POSTGRES_PASSWORD: gout1234` 도 하드코딩. 로컬용이긴 해도 `.env` 로 빼는 게 좋음.

### 2.4 미사용/불완전 코드

- `docker-compose.yml` 에 `redis` 서비스가 떠 있으나 **백엔드에 `spring-boot-starter-data-redis` 의존성 없고 코드도 Redis 미사용** (`docker-compose.prod.yml:37` 에서 REDIS_HOST 주입만 함). 현재는 완전히 dead container.
- `RestTemplateConfig` 존재하지만 `PaperCrawlerJob` 외 사용처 없음. 전역 타임아웃 설정 없음 → PubMed 응답이 늦으면 스레드 무한 블록 가능.
- `AdminPaperController.summarize` 는 결과를 `paperRepository.save(paper)` 로 수동 저장 — 현재 클래스에 `@Transactional` 없어 dirty checking 미동작. 저장 자체는 되지만 flush 타이밍이 컨테이너 밖. 명시적 `save` 덕에 동작은 정상.

### 2.5 테스트 커버리지

- **통합테스트 8개 중 8개가 `@Disabled`** (`AuthFlow`, `Food`, `Post`, `Health` 시리즈). 모두 §3.1 의 `gender_type` / §3.2 의 JPQL null 바인딩 버그 때문.
- **서비스/DAO 단위 테스트 0개.** `HospitalService.mapNativeRow` 처럼 분기 많은 메서드도 단위 테스트 없음.
- `@Disabled` 가 걸린 채 merge 된 상태라 CI 는 전부 초록으로 보이지만 실질 검증 범위는 매우 좁음.

### 2.6 기타

- `CommentServiceImpl.createComment` — parent comment 가 같은 post 에 속한지 확인하는 로직은 있으나, **대댓글 깊이 제한이 없음** (`parent_id → parent_id → …`). 무한 depth 허용.
- `FoodSearchRequest`, `HospitalSearchRequest` 의 `page`/`size` 는 validation 없음 (음수/초대형 허용). 서비스단에서 `Math.max` / `Math.min` 으로 방어는 해둠.
- `PostController` 와 `CommentController` 에 **`getCurrentUserId` 유틸이 복붙** — 공통 `SecurityUtil` 로 빼면 `HealthController.currentUserId`, `HospitalController.currentUserId` 까지 4곳 중복 제거.

---

## 3. 보안 리뷰

### 3.1 인증/인가 범위 — **P0**

- `SecurityConfig.java:40` 은 `/api/admin/**` 을 **`authenticated()` 만 요구** — 즉 **일반 USER 계정이 `POST /api/admin/papers/crawl` 을 그대로 호출 가능**.
  `hasRole("ADMIN")` 또는 `hasAuthority("ROLE_ADMIN")` 으로 좁혀야 함.
  (`CustomUserDetailsService.java:28` 이 `ROLE_USER`/`ROLE_ADMIN` 을 부여하므로 방어는 한 줄 변경으로 가능.)
- Public 화이트리스트가 넓음: `/api/foods/**`, `/api/hospitals/**`, `/api/papers/**`, `/api/guidelines/**`, `/api/content/**`, 그리고 `/api/posts/**` 의 **GET 전부**.
  게시글 상세 내용까지 비로그인 노출 — 의도된 거라면 문제 없으나, 커뮤니티가 로그인 전용이라면 과다 공개.

### 3.2 CSRF / CORS

- `SecurityConfig.java:35` 가 **CSRF 전체 disable**. JWT + SPA 조합에선 관례적이지만, `POST /api/auth/logout` 처럼 쿠키 기반 세션이 없으니 실익은 크지 않고 위험도도 낮음. **의견**: 현재 설계에선 OK.
- CORS — `setAllowedOriginPatterns(List.of("*"))` + `setAllowCredentials(true)` 조합 (`SecurityConfig.java:69-72`).
  Spring 은 이 조합을 허용하지만 **실제로는 모든 출처에서 쿠키 전송이 가능해지는 꼴**. 운영에서는 `NEXT_PUBLIC_API_URL` origin 만 허용하도록 화이트리스트화 필요 (`prod` profile 에서 override).

### 3.3 JWT 서명 키 / 토큰 관리

- `JwtTokenProvider.java:24` — `secret.getBytes()` 로 HMAC key 생성. 기본 dev secret 은 32자 이상이지만 **prod 에서 override 누락 시 동일 dev secret 사용** → 누구나 토큰 위조 가능.
  **권장**: `prod` profile 에서 `JWT_SECRET` 필수화 + 기동 시 min length (32) 검증 assert.
- refresh token 이 **stateless JWT 로만 발급되고 저장소 없음** (`AuthServiceImpl.java:72-74`). 탈취 시 만료(7일) 전까지 재발급 가능. 블랙리스트(Redis) 도입 필요.
- `logout` 엔드포인트가 토큰 무효화 로직 없이 메시지만 반환 (`AuthController.java:36-39`). 클라이언트가 localStorage 비우는 것만으로는 서버 관점 로그아웃 아님.

### 3.4 비밀번호 정책

- `RegisterRequest` — `@Size(min = 8)` 만 적용. 숫자/특수문자 혼합 규칙 없음. 서비스 타겟이 일반인 소비자 앱인 것 감안하면 현재 수준도 허용 범위지만, 민감 건강정보(요산/발작 기록)를 다루므로 **PII 등급** 상향 고려.
- BCrypt 사용 (`SecurityConfig.java:63`) — 기본 strength(10). OK.

### 3.5 레이트 리미트 / 기타

- **레이트 리미트 전무.** `POST /api/auth/login` 은 brute-force 가능. `POST /api/posts/{id}/like` 도 단순 토글이라 봇으로 like_count 부풀리기 가능.
- PubMed `RestTemplate` 에 timeout 미설정 (`RestTemplateConfig` 확인 필요). 외부 API 지연 시 스레드 고갈 가능.
- `log.warn("Invalid JWT token: {}", e.getMessage())` (`JwtTokenProvider.java:65`) — 토큰 본문 일부가 로그에 섞여나갈 수 있음. message 만 찍히긴 하지만 보수적으로 `e.getClass().getSimpleName()` 로 바꿀 것 권장.

### 3.6 OK (실제 확인 후 문제 없음)

- BCrypt 사용, `@Transactional` 경계 적절, `user_id == currentUserId` 확인 후 CRUD (`HealthServiceImpl.deleteUricAcidLog`, `PostServiceImpl.updatePost/deletePost`, `CommentServiceImpl.deleteComment`).
- SQL injection — JPQL + `@Param` + PostGIS native query 의 모든 변수 바인딩 확인. 문자열 concat 없음.
- 민감정보 마스킹 — 로그에 `user.email`/`password` 직접 찍는 곳 없음.

---

## 4. 우선순위 정리

### P0 — 블로커 (merge 전/직후 수정 필수)

1. **`users.gender` Hibernate ↔ `gender_type` enum 미스매치** — `register` 엔드포인트 500. `docs/NEXT_STEPS.md §1.1`. 통합테스트 8개 `@Disabled` 중 대부분 이 이슈 의존.
2. **`FoodRepository.search` 의 `LOWER(:keyword)` null 바인딩** — `GET /api/foods` 검색 시 500. `docs/NEXT_STEPS.md §1.2`.
3. **`/api/admin/**` 에 ADMIN 역할 검증 누락** — `SecurityConfig.java:40`. 일반 USER 가 관리자 크롤러/AI 요약 API 호출 가능.
4. **prod 에서 `JWT_SECRET` 필수화 안 됨** — 기본 dev secret 그대로 쓰이면 토큰 위조 가능. `application-prod.yml` 에 필수 체크 추가.

### P1 — 중요 (다음 sprint)

5. **CORS `*` + `allowCredentials=true`** — prod profile 에서 origin 화이트리스트화.
6. **`GlobalExceptionHandler` 의 `AccessDeniedException`/`AuthenticationException` 매핑 누락** — 401/403 이 500 으로 포장될 수 있음.
7. **`PostServiceImpl.getPosts` N+1** — 페이지마다 commentCount 쿼리 20 회.
8. **Refresh token 저장소 부재** — 탈취 대응 불가. Redis 컨테이너는 이미 떠 있으니 연결만 하면 됨.
9. **레이트 리미트** — 최소 `POST /api/auth/login`, `POST /api/posts/{id}/like` 에 Bucket4j 등 도입.
10. **통합테스트 `@Disabled` 해제** — P0 #1, #2 해결 후.
11. **프론트 `/api/users/me` 백엔드 미구현** — 서버 프로필 영속화 불가. UserController 추가.

### P2 — 보강 (가용 여유 생기면)

12. **PostStatus enum 화** — 문자열 리터럴 제거.
13. **`PostController`/`CommentController`/`HealthController`/`HospitalController` 의 `currentUserId()` 중복** — `SecurityUtil` 공통화.
14. **`PaperServiceImpl.findSimilar` O(n²) 정렬** — Map 기반으로 O(n) 재작성.
15. **`HospitalServiceImpl.search` 위치 검색 카운트 쿼리 통합** — `COUNT(*) OVER()` 윈도우.
16. **Redis 컨테이너 제거 또는 사용** — 현재 dead container.
17. **Comment 대댓글 깊이 제한** — 현재 무한 허용.
18. **`RestTemplate` 타임아웃** — PubMed 지연 대비.
19. **JaCoCo / trivy / hadolint 를 CI 에 통합** — 이번 PR 로 JaCoCo 플러그인 세팅만 완료, 실 CI 단계 연결은 별도.
20. **비밀번호 정책 강화** — 8자 이상 + 문자 조합.
21. **서비스/DAO 단위테스트 추가** — 커버리지 목표 설정.

---

## 5. 결론

- 로컬 풀스택 기동(`docker compose up`)은 정상이며 대부분 엔드포인트가 응답한다.
- 그러나 **`register` 장애 + 음식 검색 500 + 관리자 엔드포인트 인가 구멍 + 기본 JWT secret 노출** 이 네 가지가 동시에 풀리지 않으면 운영 품질로는 부적합.
- P0 4건은 전부 단일 파일 수정 수준이라 **하루 이내 해결 가능**. P1 이후는 본격 개선 sprint 로 묶어 진행 권장.
