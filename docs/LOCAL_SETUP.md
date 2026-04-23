# 로컬 풀스택 실행 가이드 (docker compose)

`docker compose up -d --build` 한 방으로 **PostgreSQL(+PostGIS+pgvector) + Spring Boot 백엔드 + Next.js 프론트**를 전부 띄우는 절차.

---

## 전제

| 항목 | 요구 |
|------|------|
| Docker Desktop | 설치 & 실행 중 (4.x 이상) |
| 플랫폼 | macOS (Apple Silicon/Intel), Linux, Windows WSL2 — `pgvector/pgvector:pg17` 이미지가 멀티아크 지원 |
| 포트 | 3000, 8080, 5432, 6379 가 비어 있어야 함 |
| 디스크 | 최초 빌드 시 약 3GB (Gradle, npm 캐시 + 이미지) |

로컬에 Java/Node 를 따로 설치할 필요는 없다. 전부 컨테이너 안에서 빌드/실행한다.

---

## Step 1 — 환경변수 준비

```bash
cp .env.example .env
```

`.env` 에서 **최소 `JWT_SECRET` 만** 32자 이상 임의값으로 교체하면 된다.

```bash
# 간단 생성 예시
echo "JWT_SECRET=$(openssl rand -base64 48)" >> .env
```

| 변수 | 용도 | 비워둬도 되는가 |
|------|------|-----------------|
| `JWT_SECRET` | HS256 서명 키 (32자+) | 기본값 있음 — 운영 절대 금지 |
| `NEXT_PUBLIC_KAKAO_MAP_KEY` | 카카오맵 JS 키 | 비워두면 `/hospital` 페이지 지도만 비활성 |
| `ANTHROPIC_API_KEY` / `OPENAI_API_KEY` | 논문 요약 등 LLM 기능 | 현재 미사용이면 공란 |
| `PAPER_CRAWLER_ENABLED` | PubMed 크롤러 on/off | 기본 false |

---

## Step 2 — 빌드 & 기동

```bash
docker compose up -d --build
```

- 첫 실행은 **5~10분** 소요 (Gradle + QueryDSL + Next build).
- 두 번째부터는 이미지 캐시로 1분 내.
- 백엔드는 `depends_on: postgres(service_healthy)` 로 묶여 있어 DB 준비 완료 전에는 기동하지 않는다.

진행 중 로그를 따로 보고 싶다면:

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

---

## Step 3 — 컨테이너 상태 확인

```bash
docker compose ps
```

기대 출력 (요약):

```
NAME            STATE     STATUS
gout-postgres   running   healthy
gout-redis      running   healthy
gout-backend    running   Up
gout-frontend   running   Up
```

- `gout-postgres` 가 `healthy` 로 올라오기 전에는 backend 가 시작되지 않는다.
- 4개 전부 `running` 이면 OK.

---

## Step 4 — 헬스체크

### 백엔드

```bash
curl -fsS http://localhost:8080/actuator/health
# 기대: {"status":"UP"}
```

### 프론트

```bash
curl -I http://localhost:3000
# 기대: HTTP/1.1 200 OK
```

### 샘플 API (DB 연결 + Flyway 시드 데이터 확인)

```bash
curl -fsS http://localhost:8080/api/foods/categories
# 기대: ["MEAT", "SEAFOOD", ...]  (V10 seed 데이터 기반)
```

브라우저 확인: <http://localhost:3000>

---

## Step 5 — 회원가입 → 로그인 동작 확인

회원가입:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "test@gout.local",
    "password": "Test1234!",
    "nickname": "tester"
  }'
```

로그인 (access token 반환):

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"test@gout.local","password":"Test1234!"}'
```

정확한 필드명은 `gout-back/src/main/java/com/gout/dto/request/` 하위 `RegisterRequest` / `LoginRequest` 참고.

---

## 트러블슈팅

### 포트 충돌 (`bind: address already in use`)

| 포트 | 충돌 원인 후보 |
|------|----------------|
| 5432 | 로컬에 postgres 가 이미 떠 있음 |
| 8080 | 다른 Spring/Tomcat 앱 |
| 3000 | 다른 Next/React 개발 서버 |
| 6379 | 로컬 redis |

해결:

```bash
lsof -iTCP:5432 -sTCP:LISTEN    # 점유 중인 프로세스 확인
# compose 포트를 바꾸고 싶다면 docker-compose.yml 의 ports: "호스트:컨테이너" 에서 호스트쪽만 바꾼다.
```

### Flyway 실패

```bash
docker compose logs backend | grep -i flyway
```

- `ERROR: extension "postgis" does not exist` → pgvector 이미지가 아닌 순정 postgres 를 썼을 때. `docker compose down -v` 후 재실행해서 초기화 스크립트를 다시 태우면 된다.
- `Validate failed` → 기존 볼륨에 이전 버전 스키마가 남아 있음. 같은 방법으로 볼륨 삭제.

### 카카오맵 키 없음

`.env` 의 `NEXT_PUBLIC_KAKAO_MAP_KEY` 를 비운 채 빌드하면 placeholder 로 주입된다. `/hospital` 페이지는 뜨고 목록·검색은 동작하지만 지도 스크립트는 로드 실패한다. 나머지 페이지는 정상.

키 발급 후 반영:

```bash
# .env 수정 → 프론트 이미지만 다시 빌드
docker compose up -d --build frontend
```

### 백엔드 로그 전체 확인

```bash
docker compose logs --tail=200 backend
```

기대 라인:
- `Successfully applied 12 migrations to schema "public"` (V1~V12)
- `Started GoutApplication in ...`

### backend 가 postgres 에 연결 못 함

`Connection refused` / `Connection to postgres:5432 refused` 이면 DB 가 아직 준비되지 않은 것.

```bash
docker compose logs postgres | tail -20   # "database system is ready to accept connections" 확인
docker compose restart backend
```

---

## 정리

```bash
# 컨테이너만 내림 (DB 데이터 유지)
docker compose down

# 데이터까지 싹 밀고 처음부터
docker compose down -v
```

---

## 아키텍처 요약

```
┌────────────┐    http:3000     ┌──────────────┐
│   browser  │ ───────────────→ │  gout-front  │   Next 16 (standalone)
└────────────┘                  └──────┬───────┘
                                       │ NEXT_PUBLIC_API_URL = http://localhost:8080
                                       ▼ (브라우저에서 직접 호출)
┌────────────┐    jdbc:5432     ┌──────────────┐
│ postgres   │ ←─────────────── │ gout-backend │   Spring Boot 4 + JPA + Flyway
│ (pgvector) │                  └──────────────┘   JWT / QueryDSL
└────────────┘
```

- 프론트 → 백엔드는 브라우저에서 직접 호출하므로 `NEXT_PUBLIC_API_URL` 이 **호스트 주소**(`localhost`) 여야 한다. 컨테이너 내부 DNS(`backend:8080`) 를 쓰면 브라우저에서 해석 불가.
- 백엔드 → DB 는 컨테이너 네트워크 내부이므로 `postgres:5432` 사용.
