# Secret Management

이 저장소에는 실제 secret 값을 커밋하지 않는다. private repo, submodule, 문서 파일도
secret 저장소로 쓰지 않는다.

## 원칙

- 실제 값은 `.env`, `.env.local`, GitHub Secrets, 운영 서버 env, 또는 secret manager에만 둔다.
- Git에는 `.env.example`처럼 변수명과 설명만 커밋한다.
- `NEXT_PUBLIC_*` 값은 브라우저 번들에 포함된다. 이 값은 "숨겨지는 값"이 아니라 도메인 제한으로 보호하는 public config다.
- REST API key, Admin key, JWT secret, DB password는 절대 `NEXT_PUBLIC_*`로 만들지 않는다.
- 운영 키를 채팅, 이슈, PR 본문, 로그에 붙여 넣었다면 재발급을 기본값으로 본다.

## 로컬 개발

루트 docker compose 실행:

```bash
cp .env.example .env
```

프론트만 직접 실행:

```bash
cp gout-front/.env.example gout-front/.env.local
```

백엔드만 직접 실행:

```bash
cp gout-back/.env.example gout-back/.env
```

## Kakao Keys

| 변수 | 위치 | 노출 범위 | 용도 |
|------|------|-----------|------|
| `NEXT_PUBLIC_KAKAO_MAP_KEY` | front build env | 브라우저 노출 | Kakao Maps JavaScript SDK |
| `KAKAO_REST_API_KEY` | backend env | 서버 전용 | Kakao Local REST API |
| `KAKAO_ADMIN_KEY` | backend env | 서버 전용 | Admin API 필요 시에만 |
| `KAKAO_NATIVE_APP_KEY` | backend env | 서버 전용 또는 native client 전용 | 현재 웹 앱에서는 직접 사용하지 않음 |

JavaScript 키는 Kakao Developers 콘솔에서 Web 플랫폼 도메인을 제한해야 한다.
로컬 개발은 `http://localhost:3000`, 운영은 실제 서비스 도메인을 등록한다.

## GitHub Actions

GitHub에는 값 자체를 커밋하지 않고 `Settings > Secrets and variables > Actions`에 등록한다.
운영 배포는 가능하면 GitHub Environment를 `production`으로 분리하고 approval rule을 둔다.

필수 후보:

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `EC2_HOST`
- `EC2_SSH_KEY`
- `NEXT_PUBLIC_API_URL`
- `NEXT_PUBLIC_KAKAO_MAP_KEY`

운영 서버 `.env`에도 필요한 값:

- `JWT_SECRET`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- `KAKAO_REST_API_KEY`
- `KAKAO_ADMIN_KEY`
- `KAKAO_NATIVE_APP_KEY`
- `ANTHROPIC_API_KEY`
- `OPENAI_API_KEY`

## 운영 서버

Docker Compose 운영 배포에서는 서버에만 `.env`를 둔다. 예:

```bash
sudo install -m 600 /dev/null /app/gout/.env
```

권장 권한:

```bash
chmod 600 /app/gout/.env
```

장기적으로는 AWS Secrets Manager 또는 SSM Parameter Store로 옮긴다.

## Rotation

다음 경우 즉시 키를 재발급한다.

- 키가 Git history에 들어간 경우
- 이슈/PR/채팅/로그에 붙여 넣은 경우
- 외부 협업자에게 노출됐을 가능성이 있는 경우
- 운영 권한자가 바뀐 경우

재발급 후에는 로컬 `.env`, GitHub Secrets, 운영 서버 env, Kakao Developers 콘솔 도메인 설정을 함께 갱신한다.
