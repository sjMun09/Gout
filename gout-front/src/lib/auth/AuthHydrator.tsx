'use client'

import { useAuthHydration } from './store'

/**
 * 앱 마운트 시 인증 store 와 localStorage 를 1회 동기화한다.
 * Provider 컴포넌트가 아니라 부수효과 전용 — children 을 받지 않는다.
 *
 * Root layout (server component) 에 그대로 꽂으면 client 경계로 인식된다.
 */
export default function AuthHydrator(): null {
  useAuthHydration()
  return null
}
