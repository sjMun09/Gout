'use client'

import { useEffect, useMemo, useState } from 'react'
import { MapPin, Phone, Search } from 'lucide-react'
import { hospitalApi } from '@/lib/api'
import { useGeolocation } from '@/hooks/useGeolocation'
import { useKakaoMap } from '@/hooks/useKakaoMap'
import type { Hospital } from '@/types'

const MAP_CONTAINER_ID = 'hospital-kakao-map'
const SEARCH_RADIUS_METERS = 5000

function formatDistance(meters?: number): string | null {
  if (meters == null) return null
  return meters < 1000
    ? `${Math.round(meters)}m`
    : `${(meters / 1000).toFixed(1)}km`
}

export default function HospitalPage() {
  const kakaoKey = process.env.NEXT_PUBLIC_KAKAO_MAP_KEY
  const { coords, error: geoError, loading: geoLoading } = useGeolocation()
  const { mapReady, addMarkers } = useKakaoMap(MAP_CONTAINER_ID, coords)

  const [keyword, setKeyword] = useState('')
  const [searchKeyword, setSearchKeyword] = useState('')
  const [hospitals, setHospitals] = useState<Hospital[]>([])
  const [listLoading, setListLoading] = useState(false)
  const [listError, setListError] = useState<string | null>(null)
  const [retryCount, setRetryCount] = useState(0)

  const showMap = Boolean(kakaoKey) && coords != null

  // coords 또는 확정된 searchKeyword 변화 시 API 재호출
  useEffect(() => {
    // 위치도 없고 키워드도 없으면 아직 조회 불가 — effect 바깥으로 빠짐
    if (geoLoading) return
    if (!coords && !searchKeyword) return

    let cancelled = false
    const run = async () => {
      setListLoading(true)
      setListError(null)
      try {
        const page = await hospitalApi.search({
          keyword: searchKeyword || undefined,
          lat: coords?.lat,
          lng: coords?.lng,
          radius: coords ? SEARCH_RADIUS_METERS : undefined,
          page: 0,
          size: 20,
        })
        if (cancelled) return
        setHospitals(page.content)
      } catch (err) {
        if (cancelled) return
        setListError('병원 목록을 불러오지 못했어요. 잠시 후 다시 시도해주세요.')
        setHospitals([])
      } finally {
        if (!cancelled) setListLoading(false)
      }
    }

    run()
    return () => {
      cancelled = true
    }
  }, [coords, searchKeyword, geoLoading, retryCount])

  // 지도 준비 + 병원 목록 변경 시 마커 갱신
  useEffect(() => {
    if (!mapReady) return
    addMarkers(hospitals)
  }, [mapReady, hospitals, addMarkers])

  const hasResults = hospitals.length > 0
  const skeletonKeys = useMemo(() => ['s1', 's2', 's3'], [])

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setSearchKeyword(keyword.trim())
  }

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <header>
        <h1 className="text-2xl font-bold text-gray-900">병원 찾기</h1>
        <p className="mt-1 text-base text-gray-600">
          내 주변 통풍 전문 병원을 확인하세요
        </p>
      </header>

      {/* 검색 입력 */}
      <form onSubmit={handleSubmit} role="search" aria-label="병원 검색">
        <label htmlFor="hospital-keyword" className="sr-only">
          병원명 또는 진료과목 검색
        </label>
        <div className="relative">
          <Search
            className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400"
            aria-hidden="true"
          />
          <input
            id="hospital-keyword"
            type="search"
            inputMode="search"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            placeholder="병원명 또는 진료과목 검색"
            className="h-12 w-full rounded-2xl border border-gray-200 bg-white pl-11 pr-4 text-base text-gray-900 placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-100"
          />
        </div>
      </form>

      {/* 지도 영역 */}
      {showMap ? (
        <div
          id={MAP_CONTAINER_ID}
          role="application"
          aria-label="주변 병원 지도"
          className="h-60 w-full overflow-hidden rounded-2xl border border-gray-200 bg-gray-100"
        />
      ) : (
        <div
          role="img"
          aria-label="지도 안내 영역"
          className="flex h-60 items-center justify-center rounded-2xl border border-dashed border-gray-300 bg-gray-50 px-4"
        >
          <div className="flex flex-col items-center gap-2 text-center text-gray-500">
            <MapPin className="h-8 w-8" aria-hidden="true" />
            <p className="text-sm">
              {!kakaoKey
                ? '지도 API 키가 설정되지 않았어요.'
                : geoError ??
                  '위치 권한을 허용하면 주변 병원을 지도로 볼 수 있어요.'}
            </p>
          </div>
        </div>
      )}

      {/* 병원 목록 */}
      <section aria-labelledby="nearby-title">
        <h2
          id="nearby-title"
          className="mb-3 text-lg font-semibold text-gray-900"
        >
          {coords ? '내 주변 병원' : '병원 목록'}
        </h2>

        {listLoading ? (
          <ul className="flex flex-col gap-3" aria-label="불러오는 중">
            {skeletonKeys.map((k) => (
              <li
                key={k}
                className="h-24 animate-pulse rounded-2xl bg-gray-100"
                aria-hidden="true"
              />
            ))}
          </ul>
        ) : listError ? (
          <div
            role="alert"
            className="flex flex-col gap-3 rounded-2xl bg-red-50 p-4 text-sm text-red-700"
          >
            <p>{listError}</p>
            <button
              type="button"
              onClick={() => setRetryCount((c) => c + 1)}
              className="self-start rounded-xl border border-red-300 bg-white px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50"
            >
              다시 시도
            </button>
          </div>
        ) : !hasResults ? (
          <p className="rounded-2xl bg-gray-50 p-6 text-center text-sm text-gray-500">
            {searchKeyword
              ? '검색 결과가 없습니다. 다른 키워드로 시도해 보세요.'
              : '주변 병원이 없습니다.'}
          </p>
        ) : (
          <ul className="flex flex-col gap-3">
            {hospitals.map((h) => {
              const distance = formatDistance(h.distanceMeters)
              return (
                <li
                  key={h.id}
                  className="flex items-start justify-between gap-3 rounded-2xl border border-gray-200 bg-white p-4"
                >
                  <div className="flex-1 min-w-0">
                    <p className="truncate text-base font-semibold text-gray-900">
                      {h.name}
                    </p>
                    {h.address && (
                      <p className="mt-0.5 truncate text-sm text-gray-500">
                        {h.address}
                      </p>
                    )}
                    {(distance || h.departments?.length) && (
                      <div className="mt-2 flex flex-wrap items-center gap-1.5">
                        {distance && (
                          <span className="inline-flex items-center rounded-full bg-blue-50 px-2 py-0.5 text-xs font-medium text-blue-700">
                            {distance}
                          </span>
                        )}
                        {h.departments?.map((dept) => (
                          <span
                            key={dept}
                            className="inline-flex items-center rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-700"
                          >
                            {dept}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  {h.phone && (
                    <a
                      href={`tel:${h.phone}`}
                      aria-label={`${h.name}에 전화하기`}
                      className="inline-flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-700 hover:bg-blue-100"
                    >
                      <Phone className="h-5 w-5" aria-hidden="true" />
                    </a>
                  )}
                </li>
              )
            })}
          </ul>
        )}
      </section>
    </div>
  )
}
