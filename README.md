# Gout Care — 통풍 케어 커뮤니티

통풍 환자를 위한 응급처치, 식단, 요산 기록, 병원 찾기, 관리 팁 공유 커뮤니티 플랫폼.
통풍을 겪는 사용자가 흩어진 정보와 부족한 지식 공유 때문에 불편을 겪지 않도록,
근거 기반 정보와 환자 경험을 한 곳에서 찾고 나눌 수 있게 하는 것을 목표로 한다.

---

## Quick Start (로컬 실행)

```bash
git clone https://github.com/sjMun09/Gout.git
cd Gout
cp .env.example .env          # JWT_SECRET 등 필요한 값 수정
docker compose up -d --build  # 첫 빌드 5~10분
# http://localhost:3000
```

상세 가이드: [docs/LOCAL_SETUP.md](docs/LOCAL_SETUP.md)

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Frontend | Next.js 16.2.4, React 19, TypeScript, Tailwind CSS, shadcn/ui, PWA |
| Backend | Spring Boot 4.0.5, Java 21, Gradle 9.4.1 |
| ORM | Spring Data JPA (기본), QueryDSL 5.1.0 (복잡한 동적 쿼리) |
| 인증 | Spring Security, JWT (jjwt 0.13.0) |
| Database | PostgreSQL 17 + PostGIS + pgvector |
| 마이그레이션 | Flyway |
| 인프라 | AWS, Docker, GitHub Actions CI/CD |
| 지도 | 카카오맵 API |

---

## 프로젝트 구조

```
Gout/
├── gout-front/                  # Next.js 프론트엔드
│   └── src/
│       ├── app/
│       │   ├── (main)/          # 메인 레이아웃 (하단 탭 포함)
│       │   │   ├── home/        # 홈 대시보드
│       │   │   ├── food/        # 음식 퓨린 검색
│       │   │   ├── record/      # 요산 수치 · 발작 · 복약 기록
│       │   │   ├── hospital/    # 주변 병원 찾기
│       │   │   └── more/        # 통풍 백과 · 커뮤니티 등
│       │   └── (auth)/          # 로그인 · 회원가입
│       ├── components/
│       │   ├── layout/          # BottomNav 등 공통 레이아웃
│       │   └── common/          # 공통 UI 컴포넌트
│       ├── service/             # API 호출 레이어
│       ├── hooks/               # 커스텀 훅
│       ├── types/               # TypeScript 타입 정의
│       └── constants/           # 프론트 상수
│
├── gout-back/                   # Spring Boot 백엔드
│   └── src/main/java/com/gout/
│       ├── config/              # SecurityConfig, QueryDslConfig
│       ├── constant/            # AppConstants (전역 상수)
│       ├── global/
│       │   ├── entity/          # BaseEntity
│       │   ├── exception/       # ErrorCode, BusinessException, GlobalExceptionHandler
│       │   └── response/        # ApiResponse
│       ├── security/            # JwtTokenProvider, JwtAuthenticationFilter
│       ├── controller/          # 컨트롤러 전체
│       ├── service/             # 서비스 인터페이스 전체
│       │   └── impl/            # 서비스 구현체 전체
│       ├── dao/                 # Repository (DAO) 전체
│       ├── entity/              # JPA 엔티티 전체
│       └── dto/
│           ├── request/         # 요청 DTO
│           └── response/        # 응답 DTO
│
├── docker-compose.yml           # 로컬 개발 환경 (PostgreSQL + Redis)
├── docker-compose.prod.yml      # 프로덕션 전체 스택
└── Makefile                     # 개발 편의 명령어
```

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 음식 퓨린 검색 | 음식별 퓨린 함량 신호등(빨강/노랑/초록) 표시 |
| 병원 찾기 | 위치 기반 주변 류마티스내과 검색 (카카오맵) |
| 커뮤니티 | 병원 경험 · 식단 · 발작 경험 자유 게시판 |
| 근거 기반 가이드 | ACR 2020 · EULAR 2016 · 대한류마티스학회 가이드라인 기반 |
| 연령별 정보 | 20대~70대 이상 연령대별 통풍 특징 및 주의사항 |
| 운동 가이드 | 논문 근거 기반 권장/주의 운동 정보 |
| 논문 요약 | PubMed 기반 통풍 관련 논문 메타데이터 및 AI 요약 |
| 응급 가이드 | 발작 시 즉시 대처법 |

---

## 로컬 개발 환경 실행

### 사전 요구사항
- Java 21
- Node.js 22 LTS
- Docker

### DB 실행 (PostgreSQL + Redis)
```bash
make dev
# 또는
docker compose up -d
```

### 백엔드 실행
```bash
cd gout-back
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### 프론트엔드 실행
```bash
cd gout-front
npm install
npm run dev
```

### 환경 변수 설정
```bash
cp .env.example .env
# .env 파일에서 필요한 값 입력
```

실제 API key, JWT secret, DB password는 Git에 커밋하지 않습니다.
Kakao 키와 운영 secret 관리 방식은 [docs/SECRET_MANAGEMENT.md](docs/SECRET_MANAGEMENT.md)를 참고하세요.

---

## DB 마이그레이션

Flyway 자동 적용 (애플리케이션 시작 시)

```
V1  — PostgreSQL 확장 (PostGIS, pgvector, pg_trgm)
V2  — users
V3  — uric_acid_logs, gout_attack_logs, medication_logs
V4  — hospitals, hospital_reviews
V5  — foods
V6  — guidelines
V7  — papers
V8  — posts, comments, post_likes
V9  — age_group_contents
V10 — 음식 시드 데이터 (논문 근거 40종)
V11 — 가이드라인 시드 데이터 (22건, DOI 포함)
```

---

## 브랜치 전략

```
main      — 운영 (PR merge만)
develop   — 개발 통합
feature/* — 기능 개발
fix/*     — 버그 수정
docs/*    — 문서
```

---

## GitHub Actions CI

- PR 생성 시 백엔드 컴파일 + 프론트엔드 빌드 자동 검증
- main 머지 시 AWS ECR 빌드 후 EC2 배포
