# CI 정책 — PR 게이트와 통합 테스트 분리

작성일: 2026-04-25
관련 이슈: #91 (CI quality gate 강화)

## 1. 목적

GitHub Actions 비용을 통제하면서도 PR 단계에서 회귀를 빠르게 차단한다.
통합 테스트(Testcontainers + docker build)는 비용이 크므로 PR 마다 돌리지
않고 main push 시점에만 돌린다.

## 2. 잡 매트릭스

| 잡 이름                       | 트리거             | 단계                                                                   | 비고                              |
| ----------------------------- | ------------------ | ---------------------------------------------------------------------- | --------------------------------- |
| Frontend Build Check          | PR + push (main)   | install → lint → typecheck → test (vitest) → build (next)              | 수 분 이내                        |
| Backend Build & Test          | PR + push (main)   | gradle build (`-x test`) + `unitTest` 태스크                           | Spring 부팅 없음, 1~2 분          |
| Backend Integration Tests     | push (main) 만     | docker build (PostGIS+pgvector) → `gradle test` (Testcontainers 전체) | 10 분 내외, 25 분 timeout 강제    |

`Backend Integration Tests` 잡은 `needs: [backend, frontend]` 로 묶여 있어
upstream 잡이 깨지면 자동 skip — 추가 비용 발생을 막는다.

## 3. `unitTest` 태스크 정의

`gout-back/build.gradle.kts` 의 `tasks.register<Test>("unitTest")` 가 다음
패키지만 포함하도록 필터링한다.

- `com/gout/util/**`
- `com/gout/security/**`
- `com/gout/constant/**`
- `com/gout/dto/**`
- `com/gout/global/response/**`

이 패키지의 테스트는 Spring 컨텍스트나 Testcontainers 를 띄우지 않는
순수 단위 테스트여야 한다. 새 단위 테스트를 추가할 때는 위 패키지 중
하나에 두고, `IntegrationTestBase` 를 상속하지 않는다.

## 4. 사유 (Rationale)

- Testcontainers 기반 통합 테스트는 Postgres/Redis 컨테이너 부팅 + Flyway
  마이그레이션 + Spring 풀 컨텍스트 로드 가 필요해 한 번 도는 데 수십 초~몇
  분 걸린다.
- 추가로 PR 마다 `docker build` 로 PostGIS+pgvector 이미지를 새로 만든다
  (~5분).
- 작은 PR (오타/문서/리팩토링) 수에 통합 테스트를 매번 돌리면 GitHub Actions
  spending limit 에 빠르게 도달한다 (#65/#66 사례: 컨테이너 행업으로 1.5 시간
  정체된 적 있음 → `timeout-minutes: 25` 강제 종료 추가됨).
- 단위 테스트(JWT/PasswordPolicy/LogMasks/AppConstants/DTO 검증/ErrorResponse
  스키마) 는 Spring 부팅 없이 끝난다. 현재 5 클래스 / 41 테스트 ≈ 45 초로,
  대부분은 `JwtTokenProviderTest` clock skew 검증의 `Thread.sleep(31_500L)`
  대기 시간이다. 이 sleep 만 빼면 실 실행은 1~2 초 내외다.

## 5. 통합 테스트를 수동으로 돌리는 방법

1. PR 을 main 으로 머지한다 → push 이벤트 발생 → 자동 실행.
2. 강제 재실행: GitHub Actions UI 의 해당 워크플로우 run 에서 "Re-run jobs".
3. 로컬 검증:
   ```sh
   docker build -t gout-test-postgis-pgvector:local -f docker/postgres.Dockerfile docker/
   cd gout-back && ./gradlew test
   ```

## 6. 실패 복구 (Failure recovery)

- 일시적 실패 (네트워크/컨테이너 시작 실패): GitHub Actions UI 에서 "Re-run
  failed jobs".
- main 이 깨진 경우: 원인 커밋을 즉시 revert 한 PR 을 머지해 main 을
  green 상태로 복구한 뒤 정식 수정 PR 을 다시 올린다. 깨진 main 은
  다른 PR 의 `needs:` 도 다 막아 팀 전체가 블로킹된다.
- `unitTest` 가 PR 단계에서 실패하면 일단 머지 차단 — 단위 테스트는 빠르므로
  로컬 (`./gradlew unitTest`) 에서 즉시 재현 가능.

## 7. 변경 이력

- 2026-04-25 (#91): `unitTest` 태스크 신설, 프론트 lint 단계 추가,
  통합 테스트 정책 문서화.
