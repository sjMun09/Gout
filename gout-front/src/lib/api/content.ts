import type { PagedResponse } from '@/types'
import { apiFetch } from './client'

// ===== 콘텐츠 타입 =====

export type GuidelineType = 'DO' | 'DONT'
export type GuidelineCategory =
  | 'FOOD'
  | 'EXERCISE'
  | 'MEDICATION'
  | 'LIFESTYLE'
  | 'EMERGENCY'
export type EvidenceStrength = 'STRONG' | 'MODERATE' | 'WEAK'

export interface Guideline {
  id: string
  type: GuidelineType
  category: GuidelineCategory
  title: string
  content: string
  evidenceStrength: EvidenceStrength
  evidenceSource?: string
  evidenceDoi?: string
  targetAgeGroups?: string[]
}

export type AgeGroup =
  | 'TWENTIES'
  | 'THIRTIES'
  | 'FORTIES'
  | 'FIFTIES'
  | 'SIXTIES'
  | 'SEVENTIES_PLUS'

export interface AgeGroupContent {
  id: string
  ageGroup: AgeGroup
  title: string
  characteristics: string
  mainCauses: string
  warnings: string
  managementTips: string
  evidenceSource?: string
}

export interface Paper {
  id: string
  pmid?: string
  doi?: string
  title: string
  abstractKo?: string
  aiSummaryKo?: string
  authors?: string[]
  journalName?: string
  publishedAt?: string
  sourceUrl?: string
  category?: string
}

// ===== 콘텐츠 API =====

export const contentApi = {
  getGuidelines: (params?: {
    category?: GuidelineCategory | string
    type?: GuidelineType | string
  }) => {
    const qs = new URLSearchParams()
    if (params?.category) qs.set('category', params.category)
    if (params?.type) qs.set('type', params.type)
    const query = qs.toString()
    return apiFetch<Guideline[]>(
      `/api/guidelines${query ? `?${query}` : ''}`,
    )
  },
  getAllAgeContent: () =>
    apiFetch<AgeGroupContent[]>('/api/content/age-group'),
  getAgeContent: (group: AgeGroup | string) =>
    apiFetch<AgeGroupContent>(`/api/content/age-group/${group}`),
  getPapers: (params?: {
    category?: string
    page?: number
    size?: number
  }) => {
    const qs = new URLSearchParams()
    if (params?.category) qs.set('category', params.category)
    qs.set('page', String(params?.page ?? 0))
    qs.set('size', String(params?.size ?? 10))
    return apiFetch<PagedResponse<Paper>>(`/api/papers?${qs.toString()}`)
  },
  getSimilarPapers: (id: string, limit = 5) =>
    apiFetch<Paper[]>(`/api/papers/${id}/similar?limit=${limit}`),
}
