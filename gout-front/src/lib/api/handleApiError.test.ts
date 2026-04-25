import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { toast } from 'sonner'
import { ApiError } from './client'
import { handleApiError } from './handleApiError'

vi.mock('sonner', () => ({
  toast: { error: vi.fn() },
}))

const toastError = vi.mocked(toast.error)

describe('handleApiError', () => {
  const originalLocation = window.location

  beforeEach(() => {
    toastError.mockClear()
    localStorage.clear()
    // jsdom 의 window.location 을 spy 가능한 객체로 교체
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: { href: '' } as Location,
    })
  })

  afterEach(() => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: originalLocation,
    })
  })

  it('401 with AUTH_EXPIRED_TOKEN: clears tokens, redirects to /login, no toast', () => {
    localStorage.setItem('accessToken', 'a')
    localStorage.setItem('refreshToken', 'r')

    handleApiError(new ApiError(401, '만료', { code: 'AUTH_EXPIRED_TOKEN' }))

    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(localStorage.getItem('refreshToken')).toBeNull()
    expect(window.location.href).toBe('/login')
    expect(toastError).not.toHaveBeenCalled()
  })

  it('401 with no code (legacy): redirects to /login', () => {
    localStorage.setItem('accessToken', 'a')

    handleApiError(new ApiError(401, 'unauthorized'))

    expect(localStorage.getItem('accessToken')).toBeNull()
    expect(window.location.href).toBe('/login')
    expect(toastError).not.toHaveBeenCalled()
  })

  it('401 with onUnauthorized override: spy called, no redirect, no clear', () => {
    localStorage.setItem('accessToken', 'a')
    const onUnauthorized = vi.fn()

    handleApiError(new ApiError(401, '만료', { code: 'AUTH_EXPIRED_TOKEN' }), {
      onUnauthorized,
    })

    expect(onUnauthorized).toHaveBeenCalledTimes(1)
    expect(localStorage.getItem('accessToken')).toBe('a')
    expect(window.location.href).toBe('')
    expect(toastError).not.toHaveBeenCalled()
  })

  it('403: toast.error with server message', () => {
    handleApiError(new ApiError(403, '권한 없음', { code: 'FORBIDDEN' }))

    expect(toastError).toHaveBeenCalledWith('권한 없음')
  })

  it('429 with retryAfter=30: toast contains "30"', () => {
    handleApiError(
      new ApiError(429, '요청 과다', { code: 'RATE_LIMITED', retryAfter: 30 }),
    )

    expect(toastError).toHaveBeenCalledTimes(1)
    const msg = toastError.mock.calls[0]?.[0] as string
    expect(msg).toContain('30')
  })

  it('422 with fieldErrors: no toast (form handles inline)', () => {
    handleApiError(
      new ApiError(422, '검증 실패', {
        code: 'VALIDATION_FAILED',
        fieldErrors: [{ field: 'nickname', code: 'Size', message: '2~20자' }],
      }),
    )

    expect(toastError).not.toHaveBeenCalled()
  })

  it('422 with empty fieldErrors array: fallback toast called', () => {
    handleApiError(
      new ApiError(422, '검증 실패', {
        code: 'VALIDATION_FAILED',
        fieldErrors: [],
      }),
    )

    expect(toastError).toHaveBeenCalledWith('검증 실패')
  })

  it('500 fallback: toast.error with server message', () => {
    handleApiError(new ApiError(500, '서버 오류'))

    expect(toastError).toHaveBeenCalledWith('서버 오류')
  })

  it('non-ApiError: generic toast', () => {
    handleApiError(new Error('unexpected'))

    expect(toastError).toHaveBeenCalledWith('오류가 발생했습니다.')
  })
})
