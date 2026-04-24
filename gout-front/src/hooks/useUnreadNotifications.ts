'use client'

import { useCallback, useEffect, useRef, useState } from 'react'
import { notificationApi } from '@/lib/api'

const DEFAULT_POLL_MS = 30_000

/**
 * 안읽은 알림 수를 주기적으로 폴링한다.
 *
 * - 로그인 토큰이 없으면 폴링하지 않는다.
 * - 탭이 숨겨져 있으면 폴링을 중단하고, 다시 보이면 즉시 한 번 갱신한다.
 * - SSE/WebSocket 대신 short-polling 으로 단순함 우선. 기본 30초.
 */
export function useUnreadNotifications(intervalMs: number = DEFAULT_POLL_MS) {
  const [count, setCount] = useState<number>(0)
  const [error, setError] = useState<Error | null>(null)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const hasToken = useCallback(() => {
    if (typeof window === 'undefined') return false
    return Boolean(window.localStorage.getItem('accessToken'))
  }, [])

  const refresh = useCallback(async () => {
    if (!hasToken()) {
      setCount(0)
      return
    }
    try {
      const res = await notificationApi.unreadCount()
      setCount(res.count ?? 0)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e : new Error(String(e)))
    }
  }, [hasToken])

  useEffect(() => {
    // 최초 1회
    refresh()

    const clear = () => {
      if (timerRef.current) {
        clearInterval(timerRef.current)
        timerRef.current = null
      }
    }
    const start = () => {
      clear()
      timerRef.current = setInterval(refresh, intervalMs)
    }

    start()

    const onVisibility = () => {
      if (document.visibilityState === 'visible') {
        refresh()
        start()
      } else {
        clear()
      }
    }
    document.addEventListener('visibilitychange', onVisibility)

    return () => {
      clear()
      document.removeEventListener('visibilitychange', onVisibility)
    }
  }, [intervalMs, refresh])

  return { count, error, refresh }
}
