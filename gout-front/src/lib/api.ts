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

// ===== 병원 API =====

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

// ===== 커뮤니티 타입 =====

export interface PostSummary {
  id: string
  title: string
  category: string
  viewCount: number
  likeCount: number
  commentCount: number
  isAnonymous: boolean
  createdAt: string
  nickname: string
}

export interface Comment {
  id: string
  postId: string
  parentId?: string
  content: string
  isAnonymous: boolean
  createdAt: string
  nickname: string
}

export interface PostDetail extends PostSummary {
  content: string
  comments: Comment[]
  liked: boolean
}

export const CATEGORY_LABELS: Record<string, string> = {
  FREE: '자유',
  QUESTION: '질문',
  FOOD_EXPERIENCE: '식단 경험',
  EXERCISE: '운동',
  MEDICATION: '약물',
  SUCCESS_STORY: '관리 성공담',
  HOSPITAL_REVIEW: '병원 경험',
}

// ===== 커뮤니티 API =====

export const communityApi = {
  getPosts: (params: { category?: string; page?: number; size?: number }) => {
    const qs = new URLSearchParams()
    if (params.category) qs.set('category', params.category)
    qs.set('page', String(params.page ?? 0))
    qs.set('size', String(params.size ?? 20))
    return apiFetch<PagedResponse<PostSummary>>(`/api/posts?${qs.toString()}`)
  },
  getPost: (id: string) => apiFetch<PostDetail>(`/api/posts/${id}`),
  createPost: (body: {
    title: string
    content: string
    category: string
    isAnonymous: boolean
  }) =>
    apiFetch<PostSummary>('/api/posts', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  getComments: (postId: string) =>
    apiFetch<Comment[]>(`/api/posts/${postId}/comments`),
  createComment: (
    postId: string,
    body: { content: string; parentId?: string; isAnonymous: boolean },
  ) =>
    apiFetch<Comment>(`/api/posts/${postId}/comments`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  toggleLike: (postId: string) =>
    apiFetch<void>(`/api/posts/${postId}/like`, { method: 'POST' }),
}

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

// ===== 사용자 타입 =====

export type UserAgeGroup =
  | 'TWENTIES'
  | 'THIRTIES'
  | 'FORTIES'
  | 'FIFTIES'
  | 'SIXTIES'
  | 'SEVENTIES_PLUS'

export interface UserProfile {
  id?: string
  nickname?: string
  ageGroup?: UserAgeGroup
  goutDiagnosedAt?: string // ISO date (YYYY-MM-DD)
  targetUricAcid?: number // mg/dL
  email?: string
}

// ===== 사용자 API =====
// 주의: 백엔드에 /api/users/me 엔드포인트 존재 여부 미확인.
// 호출 실패 시 호출측에서 localStorage 폴백 처리할 것.

export const userApi = {
  me: () => apiFetch<UserProfile>('/api/users/me'),
  updateMe: (body: Partial<UserProfile>) =>
    apiFetch<UserProfile>('/api/users/me', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
}

// ===== 건강 기록 타입 =====

export interface UricAcidLog {
  id: string
  value: number
  measuredAt: string
  memo?: string
  createdAt: string
}

export interface GoutAttackLog {
  id: string
  attackedAt: string
  painLevel?: number
  location?: string
  durationDays?: number
  suspectedCause?: string
  memo?: string
  createdAt: string
}

export interface MedicationLog {
  id: string
  medicationName: string
  dosage?: string
  takenAt: string
  createdAt: string
}

// ===== 건강 기록 API =====

// ===== 관리자 타입 =====

export type AdminUserStatus = 'ACTIVE' | 'SUSPENDED'
export type AdminUserRole = 'USER' | 'ADMIN'

export interface AdminUser {
  id: string
  email: string
  nickname: string
  role: AdminUserRole
  status: AdminUserStatus
  createdAt: string
}

export type ReportStatus = 'PENDING' | 'RESOLVED' | 'DISMISSED'

export interface AdminReport {
  id: string
  targetType: string
  targetId: string
  reason: string
  reporterNickname?: string
  status: ReportStatus
  createdAt: string
  resolvedAt?: string
}

// 관리자 API 의 일부는 다른 에이전트의 머지 대기 중일 수 있어 404 를 허용한다.
export class AdminEndpointNotReady extends Error {
  constructor() {
    super('관리자 API 가 아직 활성화되지 않았습니다.')
    this.name = 'AdminEndpointNotReady'
  }
}

async function adminFetch<T>(path: string, options?: RequestInit): Promise<T> {
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

  if (res.status === 404) {
    // Agent-F 머지 전 / 엔드포인트 미연결
    throw new AdminEndpointNotReady()
  }
  if (!res.ok) {
    throw new Error(`API ${res.status}: ${res.statusText}`)
  }

  const json = await res.json()
  if (json.success === false) {
    throw new Error(json.message ?? 'API 요청 실패')
  }
  return json.data as T
}

// ===== 관리자 API =====

export const adminApi = {
  // 유저
  listUsers: (params: { page?: number; size?: number; keyword?: string } = {}) => {
    const qs = new URLSearchParams()
    qs.set('page', String(params.page ?? 0))
    qs.set('size', String(params.size ?? 20))
    if (params.keyword) qs.set('keyword', params.keyword)
    return adminFetch<PagedResponse<AdminUser>>(
      `/api/admin/users?${qs.toString()}`,
    )
  },
  suspendUser: (id: string) =>
    adminFetch<void>(`/api/admin/users/${id}/suspend`, { method: 'POST' }),
  unsuspendUser: (id: string) =>
    adminFetch<void>(`/api/admin/users/${id}/unsuspend`, { method: 'POST' }),
  promoteUser: (id: string) =>
    adminFetch<void>(`/api/admin/users/${id}/promote`, { method: 'POST' }),

  // 게시글 (관리자 목록은 기존 /api/posts 로 읽고, 조치만 /api/admin/posts 로)
  hidePost: (id: string) =>
    adminFetch<void>(`/api/admin/posts/${id}/hide`, { method: 'POST' }),
  deletePost: (id: string) =>
    adminFetch<void>(`/api/admin/posts/${id}`, { method: 'DELETE' }),

  // 댓글
  deleteComment: (id: string) =>
    adminFetch<void>(`/api/admin/comments/${id}`, { method: 'DELETE' }),

  // 신고 (Agent-F 머지 후 활성화)
  listReports: (
    params: { status?: ReportStatus; page?: number; size?: number } = {},
  ) => {
    const qs = new URLSearchParams()
    qs.set('status', params.status ?? 'PENDING')
    qs.set('page', String(params.page ?? 0))
    qs.set('size', String(params.size ?? 20))
    return adminFetch<PagedResponse<AdminReport>>(
      `/api/admin/reports?${qs.toString()}`,
    )
  },
  resolveReport: (id: string) =>
    adminFetch<void>(`/api/admin/reports/${id}/resolve`, { method: 'POST' }),
  dismissReport: (id: string) =>
    adminFetch<void>(`/api/admin/reports/${id}/dismiss`, { method: 'POST' }),
}

// ===== 건강 기록 API =====

export const healthApi = {
  getUricAcidLogs: () =>
    apiFetch<UricAcidLog[]>('/api/health/uric-acid-logs'),
  createUricAcidLog: (body: {
    value: number
    measuredAt: string
    memo?: string
  }) =>
    apiFetch<UricAcidLog>('/api/health/uric-acid-logs', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  deleteUricAcidLog: (id: string) =>
    apiFetch<void>(`/api/health/uric-acid-logs/${id}`, { method: 'DELETE' }),

  getGoutAttackLogs: () =>
    apiFetch<GoutAttackLog[]>('/api/health/gout-attack-logs'),
  createGoutAttackLog: (
    body: Partial<GoutAttackLog> & { attackedAt: string },
  ) =>
    apiFetch<GoutAttackLog>('/api/health/gout-attack-logs', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  deleteGoutAttackLog: (id: string) =>
    apiFetch<void>(`/api/health/gout-attack-logs/${id}`, { method: 'DELETE' }),

  getMedicationLogs: () =>
    apiFetch<MedicationLog[]>('/api/health/medication-logs'),
  createMedicationLog: (body: {
    medicationName: string
    dosage?: string
    takenAt: string
  }) =>
    apiFetch<MedicationLog>('/api/health/medication-logs', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  deleteMedicationLog: (id: string) =>
    apiFetch<void>(`/api/health/medication-logs/${id}`, { method: 'DELETE' }),
}
