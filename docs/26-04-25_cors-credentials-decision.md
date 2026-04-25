# LOW-003 — CORS `allowCredentials` 와 JWT 저장 아키텍처 결정

> PR #55 보안감사 LOW-003 이월 건. 감사 시점에 "CORS `allowCredentials=true` 인데 프론트는 JWT 를 localStorage 에 넣어 사용 중 → 설정 불일치" 지적.

## 현 상태 (2026-04-25)

### 백엔드
`gout-back/src/main/java/com/gout/config/SecurityConfig.java:111`
```java
config.setAllowCredentials(true);
config.setAllowedOrigins(allowedOrigins); // 구체 origin 나열
```

### 프론트엔드
- 로그인 응답의 `accessToken` 을 `localStorage` 에 저장 (`home/page.tsx:112`, `profile/page.tsx:78/159`, 등 10+ 곳)
- API 호출 시 `Authorization: Bearer {accessToken}` 헤더로 전달
- 쿠키 / `credentials: 'include'` 사용처 없음

## 문제

`allowCredentials=true` 의 실제 의미는 **브라우저가 cookie / HTTP auth 헤더 / TLS client cert 를 cross-origin 요청에 포함해도 된다**는 허용 선언이다. 현재 인증은 `Authorization` 헤더 하나만 쓰며, 이 헤더는 `allowCredentials` 와 무관하게 `Allowed-Headers` 에 들어 있기만 하면 전송된다.

즉 **`allowCredentials=true` 는 현 아키텍처에 불필요**. 제거해도 기능 회귀 없음.

반대로 남겨 두면:
- "Cookie 기반 인증을 쓰는 것처럼 보이지만 실제로는 localStorage 에 JWT 를 던지는" 기만적인 설정
- 향후 누군가 쿠키 세션을 섞어 쓰려 할 때 **암묵적 허용**이 돼 있어 실수 가능성 증가
- `Access-Control-Allow-Origin: *` 과 병용 불가 제약은 이미 구체 origin 나열로 회피돼 실질 효과 없음

## 선택지

### 옵션 A — `allowCredentials=false` 로 바꾸고 localStorage 유지 (현 상태 유지, 설정만 정리)
- 변경: `SecurityConfig` 한 줄 수정
- 영향: 없음 (쿠키를 쓰는 API 가 없으므로)
- 트레이드오프: localStorage 취약점(XSS 로 탈취) 유지. 단, 현재 프론트는 React + Next.js 기본 escaping + CSP 적용으로 XSS 표면 자체가 크진 않음.
- **이행 난이도: 낮음 (백엔드 1줄)**

### 옵션 B — httpOnly secure 쿠키로 refresh 토큰 이동 + `allowCredentials=true` 유지
- 변경: 로그인/refresh 엔드포인트 응답을 `Set-Cookie: refreshToken=...; HttpOnly; Secure; SameSite=Lax`. 프론트는 `credentials: 'include'` 로 호출. access token 은 메모리(React state) 에만.
- 영향: refresh 플로우 전면 재설계, Next.js 서버사이드 fetch 경로까지 전부 수정
- 이점: XSS 로 refresh 탈취 불가. 모범 사례.
- 단점: CSRF 토큰 별도 도입 필요 (same-site=Lax 만으론 부족한 케이스). dev / prod 도메인 분리 시 cross-site 쿠키 설정 복잡.
- **이행 난이도: 높음 (백엔드 + 프론트 + CSRF 인프라)**

### 옵션 C — access 는 메모리, refresh 는 쿠키, logout 은 서버가 쿠키 clear (옵션 B 의 단계적 버전)
- 옵션 B 와 동일한 목표를 단계적으로: 먼저 refresh 만 쿠키화, access 는 기존대로 localStorage 유지.
- 중간 단계로 몇 주 동안 공존 가능.

## 결정

**옵션 A 채택** — 당장 `allowCredentials=false` 로 내리고 localStorage 방식 유지.

### 이유
1. 보안 이득 대비 이행 비용 격차가 크다. 옵션 B 는 현재 혼자 개발하는 Gout 프로젝트에 과투자.
2. 현재 프론트 XSS 표면은 React + 자체 에디터 없음(posts 는 평문) + CSP(P1-10, 예정) 로 이미 작다. localStorage 탈취 시나리오가 실질 위협으로 올라오기 전 단계.
3. 옵션 A 는 "쓰지 않는 능력 제거" 로 **가장 정직한 설정**. 향후 옵션 B/C 로 이행하고 싶어질 때 시작점이 명확.

### 유보/트리거
다음 상태가 오면 옵션 C 를 시작:
- 사용자 제작 HTML (위키, rich-text 에디터) 기능 추가 — XSS 표면 확대
- refresh token 수명이 현재 7일(P1-8) → 30일 이상으로 연장 — 탈취 시 피해 증가
- 다중 도메인 배포 (www / app / admin 분리) — 쿠키 운영 오버헤드가 이미 발생

## 구현 체크리스트 (옵션 A)

- [ ] `SecurityConfig.corsConfigurationSource()` 에서 `config.setAllowCredentials(true)` 제거 혹은 `false` 로 교체
- [ ] 프론트 API 호출에 `credentials: 'include'` 가 남아 있지 않은지 확인 (`grep -rn "credentials" gout-front/src`)
- [ ] preflight OPTIONS 응답 수동 확인: `curl -i -X OPTIONS -H "Origin: https://app.gout.test" -H "Access-Control-Request-Method: POST" https://api.gout.test/api/auth/login` → `Access-Control-Allow-Credentials` 헤더가 응답에 없어야 함
- [ ] 로그인/refresh/like 등 주요 플로우 E2E 회귀 (브라우저)

## 비범위
- CSP 헤더 추가 (P1-10 별도 이슈)
- JWT 수명/로테이션 전략 변경 (P1-8 에서 이미 결정)
- refresh token 쿠키화 (옵션 B/C 트리거 시점에 별도 설계)
