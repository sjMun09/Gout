'use client'

import { useEffect } from 'react'
import { create } from 'zustand'
import {
  AUTH_STORAGE_KEYS,
  clearAllTokens,
  getAccessToken,
  setAccessToken as writeAccessToken,
  setRefreshToken as writeRefreshToken,
} from './storage'

/**
 * 인증 상태 store.
 *
 * - SSR 환경에서 import 시 부수효과 (window 접근) 가 없도록 초기 토큰은 null 로 두고,
 *   `useAuthHydration()` 훅이 마운트 시점에 localStorage 에서 1회 동기화한다.
 *   이 패턴 덕분에 페이지마다 `typeof window === 'undefined'` 체크를 반복할 필요가 없다.
 *
 * - 노출 API: accessToken / isAuthenticated / isHydrated / setToken / clearTokens / logout.
 *   logout 은 쪽 → 라우팅까지 책임지지 않는다 (라우터 객체 의존성 회피).
 *   호출측 (예: useAuth().logout) 에서 라우팅을 처리한다.
 */
type AuthState = {
  accessToken: string | null
  /** localStorage 와 동기화가 한 번이라도 완료됐는가. SSR / 초기 렌더 분기에 사용. */
  isHydrated: boolean
  /** accessToken 존재 여부의 단순 derived flag. */
  isAuthenticated: boolean
  /** localStorage 에 저장된 토큰을 store 에 반영. 마운트 직후 useAuthHydration 이 호출한다. */
  hydrate: () => void
  /** 로그인/리프레시 등 토큰 갱신 시 사용. refreshToken 은 옵션. */
  setToken: (accessToken: string, refreshToken?: string) => void
  /** access + refresh 모두 제거. UI 상태도 비로그인으로 전환. */
  clearTokens: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  isHydrated: false,
  isAuthenticated: false,
  hydrate: () => {
    const token = getAccessToken()
    set({
      accessToken: token,
      isAuthenticated: !!token,
      isHydrated: true,
    })
  },
  setToken: (accessToken, refreshToken) => {
    writeAccessToken(accessToken)
    if (refreshToken !== undefined) {
      writeRefreshToken(refreshToken)
    }
    set({ accessToken, isAuthenticated: true })
  },
  clearTokens: () => {
    clearAllTokens()
    set({ accessToken: null, isAuthenticated: false })
  },
}))

/**
 * storage event 동기화 — 다른 탭에서 로그아웃 시 현재 탭도 비로그인 상태로 전환.
 * 이슈 #81 의 "요구가 생기면" 라인을 가볍게 충족 (handler 등록 1줄, 추가 의존성 없음).
 *
 * useAuthHydration 내부에서 1회 등록한다.
 */
function attachStorageSync(): () => void {
  if (typeof window === 'undefined') return () => {}
  const handler = (e: StorageEvent) => {
    if (e.key !== AUTH_STORAGE_KEYS.access && e.key !== null) return
    // key === null 은 localStorage.clear() 이벤트
    const token = getAccessToken()
    useAuthStore.setState({
      accessToken: token,
      isAuthenticated: !!token,
    })
  }
  window.addEventListener('storage', handler)
  return () => window.removeEventListener('storage', handler)
}

/**
 * 앱 루트에서 한 번 마운트되어 store 와 localStorage 를 동기화한다.
 * (Provider 컴포넌트로 감싸지 않고 useEffect 한 번으로 끝낸다 — Provider 추가 보일러플레이트 방지)
 */
export function useAuthHydration(): void {
  useEffect(() => {
    useAuthStore.getState().hydrate()
    const detach = attachStorageSync()
    return detach
  }, [])
}
