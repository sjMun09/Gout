/**
 * 토큰 IO 캡슐화 — localStorage 직접 접근은 이 모듈을 통해서만 한다.
 * SSR 가드(typeof window) 를 한곳에 모아 페이지마다 반복되던 방어 코드를 제거한다.
 *
 * 키 네이밍은 기존 코드와 호환을 위해 그대로 유지 ('accessToken', 'refreshToken').
 */

const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'

export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null
  try {
    return window.localStorage.getItem(ACCESS_TOKEN_KEY)
  } catch {
    return null
  }
}

export function setAccessToken(token: string): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(ACCESS_TOKEN_KEY, token)
  } catch {
    // ignore quota / privacy mode
  }
}

export function clearAccessToken(): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.removeItem(ACCESS_TOKEN_KEY)
  } catch {
    // ignore
  }
}

export function getRefreshToken(): string | null {
  if (typeof window === 'undefined') return null
  try {
    return window.localStorage.getItem(REFRESH_TOKEN_KEY)
  } catch {
    return null
  }
}

export function setRefreshToken(token: string): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(REFRESH_TOKEN_KEY, token)
  } catch {
    // ignore
  }
}

export function clearRefreshToken(): void {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.removeItem(REFRESH_TOKEN_KEY)
  } catch {
    // ignore
  }
}

/** access + refresh 를 함께 비운다. 401 / 로그아웃 / 탈퇴 공통 정리. */
export function clearAllTokens(): void {
  clearAccessToken()
  clearRefreshToken()
}

export const AUTH_STORAGE_KEYS = {
  access: ACCESS_TOKEN_KEY,
  refresh: REFRESH_TOKEN_KEY,
} as const
