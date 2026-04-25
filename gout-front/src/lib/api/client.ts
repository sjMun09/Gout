export const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? ''

type JwtClaims = {
  sub?: string
  roles?: string[]
}

function parseAccessTokenClaims(): JwtClaims | null {
  if (typeof window === 'undefined') return null
  const token = localStorage.getItem('accessToken')
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

/**
 * API 호출 시 던져지는 에러. 응답 body 가 있으면 파싱된 server message 를 그대로 싣는다.
 * 호출측에서 status 별 분기(예: 409 중복) 가 필요할 때 사용.
 */
export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'ApiError'
  }
}

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token =
    typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null

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
    throw new ApiError(res.status, message)
  }

  if (json && typeof json === 'object' && (json as { success?: boolean }).success === false) {
    throw new ApiError(
      res.status,
      (json as { message?: string }).message ?? 'API 요청 실패',
    )
  }
  return (json as { data: T }).data
}
