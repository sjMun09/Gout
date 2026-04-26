'use client'

import { useEffect } from 'react'
import { reportClientError } from '@/lib/observability/client-error'

/**
 * 서비스워커 등록 컴포넌트.
 * - production 환경에서만 등록 (dev 중 캐시 혼선 방지)
 * - /sw.js 를 scope '/' 로 등록
 */
export default function ServiceWorkerRegister() {
  useEffect(() => {
    if (process.env.NODE_ENV !== 'production') return
    if (typeof window === 'undefined') return
    if (!('serviceWorker' in navigator)) return

    const register = () => {
      navigator.serviceWorker
        .register('/sw.js', { scope: '/' })
        .catch((err) => {
          reportClientError({ source: 'service-worker', error: err })
        })
    }

    if (document.readyState === 'complete') {
      register()
    } else {
      window.addEventListener('load', register, { once: true })
      return () => window.removeEventListener('load', register)
    }
  }, [])

  return null
}
