import { toast } from 'sonner'
import { ApiError } from './client'

/**
 * 인증 토큰을 모두 제거하고 로그인 페이지로 이동시킨다.
 * 401(만료/무효 토큰) 응답의 기본 처리.
 */
export function clearAuthAndRedirect(): void {
  if (typeof window === 'undefined') return
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
  window.location.href = '/login'
}

const AUTH_CLEAR_CODES = new Set([
  'AUTH_EXPIRED_TOKEN',
  'AUTH_INVALID_TOKEN',
  'AUTH_UNAUTHORIZED',
])

/**
 * 글로벌 API 에러 핸들러.
 * - 401(인증 코드/레거시): 토큰 삭제 후 /login 이동 — 폼 검증성 에러는 422로 분리되므로 여기 안 옴
 * - 422 + fieldErrors: 폼이 인라인으로 렌더하므로 토스트 안 띄움
 * - 429: Retry-After 가 있으면 메시지에 포함
 * - 그 외: 서버 메시지 그대로 토스트
 *
 * 호출측에서 401 처리를 커스터마이즈하려면 `opts.onUnauthorized` 전달.
 */
export function handleApiError(
  error: unknown,
  opts?: { onUnauthorized?: () => void },
): void {
  if (!(error instanceof ApiError)) {
    toast.error('오류가 발생했습니다.')
    return
  }

  if (
    error.status === 401 &&
    (error.code === null || AUTH_CLEAR_CODES.has(error.code))
  ) {
    ;(opts?.onUnauthorized ?? clearAuthAndRedirect)()
    return
  }

  if (
    error.status === 422 &&
    error.fieldErrors &&
    error.fieldErrors.length > 0
  ) {
    return
  }

  if (error.status === 429) {
    const retry = error.retryAfter
    const base = error.message || '요청이 너무 많습니다.'
    toast.error(`${base}${retry != null ? ` (${retry}초 후 재시도)` : ''}`)
    return
  }

  if (error.status === 403) {
    toast.error(error.message || '접근 권한이 없습니다.')
    return
  }

  toast.error(error.message || '오류가 발생했습니다.')
}
