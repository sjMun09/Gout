import type { FoodDetail, FoodItem, PagedResponse } from '@/types'
import { apiFetch } from './client'

// ===== 음식 API =====

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
