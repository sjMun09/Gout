# API 문서 (Swagger UI) 사용법 — 2026-04-24

Springdoc OpenAPI 3.0.3 (Spring Boot 4.0.5 대응) 적용. 본 문서는 로컬/스테이징에서
Swagger UI 를 열어 엔드포인트를 호출하는 절차와 인증 스키마를 설명한다.

---

## 1. 실행

```bash
# 1) 로컬 풀스택
docker compose up -d

# 2) 혹은 백엔드만
cd gout-back
./gradlew bootRun --args='--spring.profiles.active=dev'
```

백엔드가 8080 에서 뜨면 아래 URL 에서 바로 접근 가능.

| 용도 | URL |
|------|-----|
| Swagger UI | <http://localhost:8080/swagger-ui.html> (리다이렉트 → `/swagger-ui/index.html`) |
| OpenAPI JSON | <http://localhost:8080/v3/api-docs> |
| OpenAPI YAML | <http://localhost:8080/v3/api-docs.yaml> |

> 운영에서 차단하고 싶으면 `application-prod.yml` 에
> `springdoc.swagger-ui.enabled: false`, `springdoc.api-docs.enabled: false` 를 추가.

---

## 2. Security 경로 허용 방식

`SecurityConfig` 는 Agent-I 가 동시에 수정 중이라 충돌 회피를 위해 본 PR 에선 건드리지 않았다.
대신 **`OpenApiWebSecurityCustomizer`** 라는 별도 `@Configuration` 클래스에서
`WebSecurityCustomizer` 빈으로 아래 경로를 Security 필터체인 자체에서 제외시킨다.

- `/v3/api-docs`, `/v3/api-docs/**`, `/v3/api-docs.yaml`
- `/swagger-ui.html`, `/swagger-ui/**`
- `/webjars/**` (swagger-ui 가 참조하는 리소스)

`ignoring()` 은 `permitAll()` 과 다르게 **`JwtAuthenticationFilter` 자체가 실행되지 않으므로**
토큰이 없어도 Swagger UI 정적 리소스가 로드된다.

---

## 3. 인증 (Authorize 버튼)

- 본 API 는 **JWT Bearer** 토큰을 사용.
- Swagger UI 우상단의 🔓 **Authorize** 버튼을 누르면 BearerAuth 입력창이 뜬다.
- 값은 `ey...` 형태의 **access token 본문만** 입력 (`Bearer ` prefix 없이).

### 토큰 발급 흐름

1. `POST /api/auth/register` — 가입. 응답 body 의 `data.accessToken` 사용.
   - 주의: 현재 `gender_type` enum 버그로 500 반환 가능 (NEXT_STEPS §1.1).
2. `POST /api/auth/login` — 기존 계정 로그인.
3. `POST /api/auth/refresh` — refresh 토큰으로 access 재발급.

access token 만료는 15분(`jwt.access-token-expiry`), refresh 는 7일.

---

## 4. 보안 스키마 요약

OpenAPI 문서(`/v3/api-docs`)에 정의된 single scheme:

```json
{
  "BearerAuth": {
    "type": "http",
    "scheme": "bearer",
    "bearerFormat": "JWT"
  }
}
```

- 모든 엔드포인트에 기본 `security: [{BearerAuth: []}]` 가 걸려 있지만,
  실제 Spring Security 인가는 `SecurityConfig` 의 `authorizeHttpRequests(...)` 가 판단.
- `SecurityConfig.java:39-51` 기준 public 엔드포인트:
  - `/api/auth/**`, `/api/foods/**`, `/api/guidelines/**`, `/api/hospitals/**`,
    `/api/papers/**`, `/api/content/**`, `/actuator/health`, 그리고 `/api/posts/**`·`/api/comments/**` 의 **GET**.
- 그 외는 토큰 필요.

---

## 5. 알려진 제약

- `@Operation`, `@Parameter`, `@Schema` 등의 세밀한 어노테이션은 아직 적용 안 함.
  Springdoc 가 리플렉션으로 자동 문서화한 기본 정보만 노출된다.
- 관리자 엔드포인트(`/api/admin/**`)도 현재 BearerAuth 만으로 통과됨.
  **실제로는 USER/ADMIN 구분이 안 되고 있음 — `docs/26-04-24_design-vs-implementation-audit.md` §3.1 P0 참조.**
- Request/Response DTO 의 `@Valid` 제약은 `@Schema` 로 명시하지 않아도 Springdoc 가 대체로 인식한다.
- 현재 통합테스트 다수가 `@Disabled` 상태이므로, Swagger UI 에서 보이는 일부 엔드포인트
  (예: `POST /api/auth/register`, `GET /api/foods?keyword=...`) 는 500 을 반환할 수 있다.
