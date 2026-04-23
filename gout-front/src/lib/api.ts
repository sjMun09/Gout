import type { FoodDetail, FoodItem, Hospital, PagedResponse } from '@/types'

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? ''

export async function apiFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token =
    typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })

  if (!res.ok) {
    throw new Error(`API ${res.status}: ${res.statusText}`)
  }

  const json = await res.json()
  if (json.success === false) {
    throw new Error(json.message ?? 'API 요청 실패')
  }
  return json.data as T
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

export interface FoodSearchParams {
  keyword?: string
  purineLevel?: string
  category?: string
  page?: number
  size?: number
}

export const foodApi = {
  search: (params: FoodSearchParams) => {
    const qs = new URLSearchParams()
    if (params.keyword) qs.set('keyword', params.keyword)
    if (params.purineLevel) qs.set('purineLevel', params.purineLevel)
    if (params.category) qs.set('category', params.category)
    qs.set('page', String(params.page ?? 0))
    qs.set('size', String(params.size ?? 20))
    return apiFetch<PagedResponse<FoodItem>>(`/api/foods?${qs.toString()}`)
  },
  getById: (id: string) => apiFetch<FoodDetail>(`/api/foods/${id}`),
  getCategories: () => apiFetch<string[]>('/api/foods/categories'),
}
