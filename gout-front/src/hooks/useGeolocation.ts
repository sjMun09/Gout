'use client'

import { useEffect, useState } from 'react'

interface Coords {
  lat: number
  lng: number
}

interface UseGeolocationResult {
  coords: Coords | null
  error: string | null
  loading: boolean
}

/**
 * 브라우저 Geolocation API를 감싼 훅.
 * - 페이지 진입 시 1회 위치 조회
 * - 권한 거부 / 미지원 / 타임아웃 시 error 세팅
 */
export function useGeolocation(): UseGeolocationResult {
  const [coords, setCoords] = useState<Coords | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // 브라우저 지원 여부는 effect 내부에서 비동기적으로 처리 (SSR 대응)
    if (typeof navigator === 'undefined' || !navigator.geolocation) {
      // microtask 로 지연해 렌더 중 setState 회피
      queueMicrotask(() => {
        setError('위치 정보를 지원하지 않는 브라우저입니다.')
        setLoading(false)
      })
      return
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setCoords({ lat: pos.coords.latitude, lng: pos.coords.longitude })
        setLoading(false)
      },
      () => {
        setError('위치 권한이 거부되었습니다. 수동으로 검색해 주세요.')
        setLoading(false)
      },
      { timeout: 10000 },
    )
  }, [])

  return { coords, error, loading }
}
