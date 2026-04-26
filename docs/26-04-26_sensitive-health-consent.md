# 민감 건강정보 동의 정책

요산수치, 통풍 발작, 복약 기록은 건강에 관한 민감정보로 취급한다. 사용자가 `POST /api/me/sensitive-consent`로 민감 건강정보 수집·이용에 동의한 뒤에만 건강 기록 API를 사용할 수 있다.

## 적용 범위

- `GET /api/health/*`
- `POST /api/health/*`
- `DELETE /api/health/*`

동의가 없거나 철회된 사용자는 `403 HEALTH_SENSITIVE_CONSENT_REQUIRED` 응답을 받는다. 프론트엔드는 건강 기록 화면에서 동의 상태를 먼저 확인하고, 미동의 상태에서는 건강 기록을 조회하거나 생성하지 않는다.

## 동의 상태

- 현재 상태 조회: `GET /api/me`의 `consentSensitiveAt`
- 동의: `POST /api/me/sensitive-consent`
- 철회: `DELETE /api/me/sensitive-consent`

동의를 철회해도 기존 건강 기록은 자동 삭제하지 않는다. 기록 삭제는 회원 탈퇴 또는 개별 삭제 플로우에서 처리한다.
