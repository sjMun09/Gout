'use client'

import { useCallback, useEffect, useRef, useState } from 'react'

type KakaoMapSdkStatus = 'missing-key' | 'loading' | 'ready' | 'error'
type KakaoMapLoadStatus = Exclude<KakaoMapSdkStatus, 'missing-key'>

interface KakaoLatLng {
  readonly __kakaoLatLngBrand?: never
}

interface KakaoMap {
  readonly __kakaoMapBrand?: never
}

interface KakaoMarker {
  setMap: (map: KakaoMap | null) => void
}

interface KakaoInfoWindow {
  open: (map: KakaoMap, marker: KakaoMarker) => void
}

interface KakaoMaps {
  LatLng: new (lat: number, lng: number) => KakaoLatLng
  Map: new (
    container: HTMLElement,
    options: { center: KakaoLatLng; level: number },
  ) => KakaoMap
  Marker: new (options: {
    position: KakaoLatLng
    map: KakaoMap
  }) => KakaoMarker
  InfoWindow: new (options: {
    content: string | HTMLElement
  }) => KakaoInfoWindow
  event: {
    addListener: (
      target: KakaoMarker,
      type: 'click',
      handler: () => void,
    ) => void
  }
  load: (callback: () => void) => void
}

declare global {
  interface Window {
    kakao?: {
      maps: KakaoMaps
    }
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
  sdkStatus: KakaoMapSdkStatus
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
  // TODO: API 키 발급 필요 — https://developers.kakao.com 에서 JavaScript 키 발급 후
  //   .env.local / 배포 환경변수에 NEXT_PUBLIC_KAKAO_MAP_KEY 로 지정.
  //   미설정 시 병원 페이지의 지도 영역이 안내 문구로 대체됨 (hospital/page.tsx 참조).
  const apiKey = process.env.NEXT_PUBLIC_KAKAO_MAP_KEY
  const mapRef = useRef<KakaoMap | null>(null)
  const markersRef = useRef<KakaoMarker[]>([])
  const [loadStatus, setLoadStatus] = useState<KakaoMapLoadStatus>('loading')
  const sdkStatus: KakaoMapSdkStatus = !apiKey ? 'missing-key' : loadStatus
  const mapReady = Boolean(coords && sdkStatus === 'ready')

  useEffect(() => {
    if (!apiKey) {
      mapRef.current = null
      return
    }

    if (!coords) {
      mapRef.current = null
      return
    }

    let cancelled = false

    const initMap = () => {
      const container = document.getElementById(containerId)
      const maps = window.kakao?.maps
      if (!container || !maps || cancelled) return
      const options = {
        center: new maps.LatLng(coords.lat, coords.lng),
        level: 4,
      }
      mapRef.current = new maps.Map(container, options)
      setLoadStatus('ready')
    }

    const handleScriptError = () => {
      if (cancelled) return
      setLoadStatus('error')
    }

    const loadKakaoMaps = () => {
      const maps = window.kakao?.maps
      if (!maps) {
        handleScriptError()
        return
      }
      maps.load(() => initMap())
    }

    const loadScript = () => {
      if (document.getElementById('kakao-map-script')) {
        // 스크립트는 로드되었으나 아직 window.kakao 가 없을 수 있음
        const existing = document.getElementById(
          'kakao-map-script',
        ) as HTMLScriptElement | null
        if (window.kakao?.maps) {
          loadKakaoMaps()
        } else if (existing) {
          existing.addEventListener('load', loadKakaoMaps, { once: true })
          existing.addEventListener('error', handleScriptError, { once: true })
        }
        return
      }
      const script = document.createElement('script')
      script.id = 'kakao-map-script'
      script.src = `//dapi.kakao.com/v2/maps/sdk.js?appkey=${apiKey}&autoload=false`
      script.async = true
      script.onload = loadKakaoMaps
      script.onerror = handleScriptError
      document.head.appendChild(script)
    }

    if (window.kakao?.maps) {
      loadKakaoMaps()
    } else {
      loadScript()
    }

    return () => {
      cancelled = true
    }
  }, [coords, apiKey, containerId])

  const addMarkers = useCallback((hospitals: HospitalMarkerInput[]) => {
    const map = mapRef.current
    const maps = window.kakao?.maps
    if (!map || !maps) return

    // 기존 마커 제거 (재검색 시 누적 방지)
    markersRef.current.forEach((m) => m.setMap(null))
    markersRef.current = []

    hospitals.forEach((h) => {
      if (h.latitude == null || h.longitude == null) return
      const position = new maps.LatLng(h.latitude, h.longitude)
      const marker = new maps.Marker({
        position,
        map,
      })
      const content = document.createElement('div')
      content.style.padding = '4px 8px'
      content.style.fontSize = '13px'
      content.textContent = h.name

      const infoWindow = new maps.InfoWindow({
        content,
      })
      maps.event.addListener(marker, 'click', () => {
        infoWindow.open(map, marker)
      })
      markersRef.current.push(marker)
    })
  }, [])

  return { mapReady, sdkStatus, addMarkers }
}
