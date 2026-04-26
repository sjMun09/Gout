# 응급 콘텐츠 리소스 관리

응급 페이지의 고정 의료 안내 문구는 `gout-front/src/content/emergency.ts`에서 관리한다.

- `metadata.version`과 `metadata.lastReviewedAt`은 문구를 검토하거나 근거를 확인한 날짜로 갱신한다.
- `sections.doNow`, `sections.dontNow`, `sections.hospitalAlert`은 페이지에서 즉시 렌더링하는 고정 안내다. 문구 변경 시 기존 표현을 임의로 재작성하지 말고 의료 근거 확인 후 필요한 항목만 수정한다.
- `sections.extraGuidelines.apiCategory`는 `/api/guidelines?category=EMERGENCY` 추가 안내를 불러오는 연결값이다. 이 API의 시드 근거는 `gout-back/src/main/resources/db/migration/V11__seed_guidelines.sql`, `V14__seed_guidelines_expanded.sql`에서 확인한다.
- 응급 연락처는 `contacts.items`에서 관리한다. 전화번호가 바뀌면 화면 표시값과 `tel:` 링크를 함께 갱신한다.

DB/API 모델 변경 없이 문구만 조정하는 경우 프론트 리소스 파일과 이 문서를 함께 갱신한다. 새로운 근거 기반 장문 안내를 추가해야 할 때는 기존 `guidelines` 시드/API에 넣는 쪽을 우선 검토한다.
