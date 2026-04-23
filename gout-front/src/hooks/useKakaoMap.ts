'use client'

import { useEffect, useRef, useState } from 'react'

declare global {
  interface Window {
    // 카카오맵 SDK는 공식 타입 패키지를 쓰지 않으므로 any로 둔다.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    kakao: any
  }
}

interface HospitalMarkerInput {
  id: string
  name: string
  latitude?: number
  longitude?: number
}

interface UseKakaoMapResult {
  mapReady: boolean
  addMarkers: (hospitals: HospitalMarkerInput[]) => void
}

/**
 * 카카오맵 JS SDK를 동적으로 로드하고 지도를 초기화하는 훅.
 * - NEXT_PUBLIC_KAKAO_MAP_KEY 가 없거나 coords 가 null 이면 초기화하지 않음
 * - 스크립트는 한 번만 로드 (id="kakao-map-script")
 * - addMarkers 로 마커 + 클릭 시 InfoWindow 출력
 */
export function useKakaoMap(
  containerId: string,
  coords: { lat: number; lng: number } | null,
): UseKakaoMapResult {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const mapRef = useRef<any>(null)
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const markersRef = useRef<any[]>([])
  const [mapReady, setMapReady] = useState(false)
  const apiKey = process.env.NEXT_PUBLIC_KAKAO_MAP_KEY

  useEffect(() => {
    if (!apiKey || !coords) return

    const initMap = () => {
      const container = document.getElementById(containerId)
      if (!container || !window.kakao?.maps) return
      const options = {
        center: new window.kakao.maps.LatLng(coords.lat, coords.lng),
        level: 4,
      }
      mapRef.current = new window.kakao.maps.Map(container, options)
      setMapReady(true)
    }

    const loadScript = () => {
      if (document.getElementById('kakao-map-script')) {
        // 스크립트는 로드되었으나 아직 window.kakao 가 없을 수 있음
        const existing = document.getElementById(
          'kakao-map-script',
        ) as HTMLScriptElement | null
        if (window.kakao?.maps) {
          window.kakao.maps.load(() => initMap())
        } else if (existing) {
          existing.addEventListener('load', () => {
            window.kakao.maps.load(() => initMap())
          })
        }
        return
      }
      const script = document.createElement('script')
      script.id = 'kakao-map-script'
      script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${apiKey}&autoload=false`
      script.async = true
      script.onload = () => {
        window.kakao.maps.load(() => initMap())
      }
      document.head.appendChild(script)
    }

    if (window.kakao?.maps) {
      window.kakao.maps.load(() => initMap())
    } else {
      loadScript()
    }
  }, [coords, apiKey, containerId])

  const addMarkers = (hospitals: HospitalMarkerInput[]) => {
    if (!mapRef.current || !window.kakao?.maps) return

    // 기존 마커 제거 (재검색 시 누적 방지)
    markersRef.current.forEach((m) => m.setMap(null))
    markersRef.current = []

    hospitals.forEach((h) => {
      if (h.latitude == null || h.longitude == null) return
      const position = new window.kakao.maps.LatLng(h.latitude, h.longitude)
      const marker = new window.kakao.maps.Marker({
        position,
        map: mapRef.current,
      })
      const infoWindow = new window.kakao.maps.InfoWindow({
        content: `<div style="padding:4px 8px;font-size:13px;">${h.name}</div>`,
      })
      window.kakao.maps.event.addListener(marker, 'click', () => {
        infoWindow.open(mapRef.current, marker)
      })
      markersRef.current.push(marker)
    })
  }

  return { mapReady, addMarkers }
}
