import { getAccessToken } from '@/lib/auth/storage'

export const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? ''

type JwtClaims = {
  sub?: string
  roles?: string[]
}

function parseAccessTokenClaims(): JwtClaims | null {
  const token = getAccessToken()
  if (!token) return null
  const parts = token.split('.')
  if (parts.length < 2) return null
  try {
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = payload + '==='.slice((payload.length + 3) % 4)
    const json = atob(padded)
    return JSON.parse(json) as JwtClaims
  } catch {
    return null
  }
}

/**
 * localStorage 의 accessToken 에서 현재 로그인 사용자 ID (JWT sub 클레임)를 추출한다.
 * JWT 검증은 수행하지 않으며, 토큰 부재/형식 오류 시 null 을 반환한다.
 * UI 권한 표시용 (예: 본인 댓글에 수정 버튼)으로만 사용. 실제 인가는 서버에서 검증.
 */
export function getCurrentUserId(): string | null {
  return parseAccessTokenClaims()?.sub ?? null
}

/**
 * accessToken 의 roles 클레임을 추출한다. JwtTokenProvider 는 access 토큰에만 roles 를 포함시킨다.
 * UI 라우팅 분기용 (admin 레이아웃 등) 으로만 사용. 실제 인가는 서버에서 검증.
 */
export function getCurrentUserRoles(): string[] {
  const roles = parseAccessTokenClaims()?.roles
  return Array.isArray(roles) ? roles : []
}

export function hasAdminRole(): boolean {
  return getCurrentUserRoles().includes('ADMIN')
}

export type FieldError = { field: string; code: string | null; message: string }

/**
 * API 호출 시 던져지는 에러. 응답 body 가 있으면 파싱된 server message 를 그대로 싣는다.
 * 호출측에서 status 별 분기(예: 409 중복) 가 필요할 때 사용.
 *
 * - `code`: 백엔드 ErrorCode (예: AUTH_EXPIRED_TOKEN). 레거시 응답이면 null.
 * - `fieldErrors`: 422 검증 실패 시 필드별 에러 목록.
 * - `retryAfter`: 429 응답의 Retry-After 헤더 (초 단위).
 */
export class ApiError extends Error {
  status: number
  code: string | null
  fieldErrors: FieldError[] | null
  retryAfter: number | null
  constructor(
    status: number,
    message: string,
    opts?: {
      code?: string | null
      fieldErrors?: FieldError[] | null
      retryAfter?: number | null
    },
  ) {
    super(message)
    this.status = status
    this.name = 'ApiError'
    this.code = opts?.code ?? null
    this.fieldErrors = opts?.fieldErrors ?? null
    this.retryAfter = opts?.retryAfter ?? null
  }
}

/**
 * 서버가 내려주는 fieldErrors 배열을 방어적으로 파싱한다.
 * 배열이 아니면 null, 항목 형태가 어긋나면 String 으로 강제 변환한다.
 */
export function parseFieldErrors(raw: unknown): FieldError[] | null {
  if (!Array.isArray(raw)) return null
  return raw.map((item) => {
    const obj = (item ?? {}) as { field?: unknown; code?: unknown; message?: unknown }
    const field = typeof obj.field === 'string' ? obj.field : String(obj.field ?? '')
    const code =
      typeof obj.code === 'string'
        ? obj.code
        : obj.code == null
          ? null
          : String(obj.code)
    const message =
      typeof obj.message === 'string' ? obj.message : String(obj.message ?? '')
    return { field, code, message }
  })
}

/**
 * Retry-After 헤더를 초 단위 숫자로 파싱한다. 음수/NaN 은 null.
 * (HTTP-date 형식은 현재 미지원 — 필요 시 별도 처리)
 */
export function parseRetryAfter(headerValue: string | null): number | null {
  if (headerValue == null) return null
  const n = parseInt(headerValue, 10)
  if (!Number.isFinite(n) || n < 0) return null
  return n
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = getAccessToken()

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })

  // 응답 body 는 항상 읽어본다 — 서버가 ApiResponse.error 로 message 를 내려주기 때문에
  // !res.ok 여도 본문의 message 를 에러에 실어야 호출측에서 사용자 안내가 가능하다.
  const text = await res.text()
  let json: unknown = null
  if (text) {
    try {
      json = JSON.parse(text)
    } catch {
      // 텍스트 응답 (드문 케이스) — 그대로 메시지로 사용
    }
  }

  if (!res.ok) {
    const message =
      (json as { message?: string } | null)?.message ??
      (typeof json === 'string' ? json : null) ??
      `API ${res.status}: ${res.statusText}`
    const code = (json as { code?: string | null } | null)?.code ?? null
    const fieldErrors = parseFieldErrors((json as { fieldErrors?: unknown } | null)?.fieldErrors)
    const retryAfter = parseRetryAfter(res.headers.get('Retry-After'))
    throw new ApiError(res.status, message, { code, fieldErrors, retryAfter })
  }

  if (json && typeof json === 'object' && (json as { success?: boolean }).success === false) {
    const code = (json as { code?: string | null }).code ?? null
    const fieldErrors = parseFieldErrors((json as { fieldErrors?: unknown }).fieldErrors)
    throw new ApiError(
      res.status,
      (json as { message?: string }).message ?? 'API 요청 실패',
      { code, fieldErrors },
    )
  }
  return (json as { data: T }).data
}
