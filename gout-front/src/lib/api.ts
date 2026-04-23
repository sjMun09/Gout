import type { Hospital, PagedResponse } from '@/types'

// 백엔드 공통 응답 포맷
interface ApiResponse<T> {
  success: boolean
  data: T
  error?: { code?: string; message?: string }
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? ''

/**
 * 백엔드 API 공용 fetch 헬퍼.
 * - 응답을 {success, data} 포맷으로 파싱해 data만 반환
 * - 네트워크/서버 오류 시 Error throw
 */
export async function apiFetch<T>(
  path: string,
  init?: RequestInit,
): Promise<T> {
  const url = `${API_BASE_URL}${path}`
  const res = await fetch(url, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(init?.headers ?? {}),
    },
  })

  if (!res.ok) {
    throw new Error(`API ${res.status}: ${res.statusText}`)
  }

  const json = (await res.json()) as ApiResponse<T>
  if (json.success === false) {
    throw new Error(json.error?.message ?? 'API 요청 실패')
  }
  return json.data
}

export const hospitalApi = {
  search: (params: {
    keyword?: string
    lat?: number
    lng?: number
    radius?: number
    page?: number
    size?: number
  }) => {
    const qs = new URLSearchParams()
    if (params.keyword) qs.set('keyword', params.keyword)
    if (params.lat != null) qs.set('lat', String(params.lat))
    if (params.lng != null) qs.set('lng', String(params.lng))
    if (params.radius != null) qs.set('radius', String(params.radius))
    qs.set('page', String(params.page ?? 0))
    qs.set('size', String(params.size ?? 20))
    return apiFetch<PagedResponse<Hospital>>(`/api/hospitals?${qs}`)
  },
}
