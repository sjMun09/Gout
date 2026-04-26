'use client'

import { useRouter } from 'next/navigation'
import { useCallback } from 'react'
import { useAuthStore } from './store'

/**
 * 페이지/컴포넌트에서 인증 상태와 액션을 한곳에서 가져오는 훅.
 *
 * - `accessToken`: 토큰 문자열. 헤더 직접 조립이 필요한 곳에서만 쓰고, 일반 호출은 apiFetch 가 알아서 붙인다.
 * - `isAuthenticated`: 토큰 존재 여부. 페이지의 인증 분기 (로그인 유도 화면 등) 에서 사용.
 * - `isHydrated`: localStorage 동기화 완료 여부. SSR/CSR 깜빡임 방지용 — false 면 스켈레톤.
 * - `setToken`, `clearTokens`: 로그인/로그아웃/탈퇴 시 토큰 조작.
 * - `logout`: 토큰 정리 + /login 으로 이동까지 일원화. 백엔드 로그아웃 API 가 생기면 여기서 호출.
 */
export function useAuth() {
  const router = useRouter()
  const accessToken = useAuthStore((s) => s.accessToken)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isHydrated = useAuthStore((s) => s.isHydrated)
  const setToken = useAuthStore((s) => s.setToken)
  const clearTokens = useAuthStore((s) => s.clearTokens)

  const logout = useCallback(() => {
    clearTokens()
    router.push('/login')
  }, [clearTokens, router])

  return {
    accessToken,
    isAuthenticated,
    isHydrated,
    setToken,
    clearTokens,
    logout,
  }
}
