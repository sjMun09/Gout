# 운영 관측성 메모

## Request ID

- 백엔드는 모든 요청에 `X-Request-Id` 응답 헤더를 붙인다.
- 클라이언트가 보낸 `X-Request-Id` 는 `[A-Za-z0-9._:-]` 문자만 포함하고 8~128자일 때만 재사용한다. 그 외 값은 UUID로 대체한다.
- 같은 값은 로그 MDC의 `requestId` 로 들어가며 기본 로그 레벨 패턴에 `requestId=...` 형태로 출력된다.
- 브라우저에서 확인할 수 있도록 CORS `Access-Control-Expose-Headers` 에도 `X-Request-Id` 를 추가했다.

## ErrorResponse 결정

표준 에러 본문에는 이번 변경에서 `requestId` 필드를 추가하지 않았다. 현재 프론트와 테스트는 `success/code/message/status/path/timestamp/fieldErrors/data` shape 를 표준 계약으로 보고 있어, 관측성 식별자는 우선 헤더와 로그의 상관관계로만 제공한다.

## 프론트 클라이언트 에러 수집

- `NEXT_PUBLIC_CLIENT_ERROR_ENDPOINT` 를 설정하면 전역 에러 바운더리와 서비스워커 등록 실패가 해당 엔드포인트로 최소 JSON 페이로드를 보낸다.
- 페이로드에는 `source`, 에러 `name/message/digest`, query string 없는 `path`, `occurredAt` 만 포함한다.
- 쿠키, 토큰, localStorage, 요청 본문, URL query/hash 는 읽거나 전송하지 않는다.
- 운영 대시보드는 `requestId` 포함 백엔드 5xx/4xx 급증, 프론트 `source` 별 에러율, 서비스워커 등록 실패율을 기본 알림 지표로 잡는다.
