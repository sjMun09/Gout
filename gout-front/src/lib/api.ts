import type { FoodDetail, FoodItem, Hospital, PagedResponse } from '@/types'

const API_BASE = process.env.NEXT_PUBLIC_API_URL ?? ''

/**
 * localStorage 의 accessToken 에서 현재 로그인 사용자 ID (JWT sub 클레임)를 추출한다.
 * JWT 검증은 수행하지 않으며, 토큰 부재/형식 오류 시 null 을 반환한다.
 * UI 권한 표시용 (예: 본인 댓글에 수정 버튼)으로만 사용. 실제 인가는 서버에서 검증.
 */
export function getCurrentUserId(): string | null {
  if (typeof window === 'undefined') return null
  const token = localStorage.getItem('accessToken')
  if (!token) return null
  const parts = token.split('.')
  if (parts.length < 2) return null
  try {
    const payload = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = payload + '==='.slice((payload.length + 3) % 4)
    const json = atob(padded)
    const claims = JSON.parse(json) as { sub?: string }
    return claims.sub ?? null
  } catch {
    return null
  }
}

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
  userId?: string
  content: string
  isAnonymous: boolean
  createdAt: string
  updatedAt?: string
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
  getPosts: (params: {
    category?: string
    keyword?: string
    page?: number
    size?: number
  }) => {
    const qs = new URLSearchParams()
    if (params.category) qs.set('category', params.category)
    if (params.keyword) qs.set('keyword', params.keyword)
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
  updateComment: (id: string, content: string) =>
    apiFetch<Comment>(`/api/comments/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ content }),
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
// 주의: 백엔드 /api/users/me 는 프로필 로컬 캐시용 레거시 — 서버 미구현이어도 무방.
// Agent-H 가 실제 계정(/api/me) 엔드포인트를 추가했다.

export type UserRole = 'USER' | 'ADMIN'
export type UserGender = 'MALE' | 'FEMALE' | 'OTHER'

/** GET /api/me 응답 — 실제 계정 프로필 */
export interface AccountProfile {
  id: string
  email: string
  nickname: string
  role: UserRole
  birthYear?: number | null
  gender?: UserGender | null
  createdAt?: string | null
}

export interface EditProfilePayload {
  nickname?: string
  birthYear?: number | null
  gender?: UserGender | null
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export const userApi = {
  // 레거시: 로컬 저장 프로필(/api/users/me) — 백엔드 미구현 시 호출측에서 폴백 처리.
  me: () => apiFetch<UserProfile>('/api/users/me'),
  updateMe: (body: Partial<UserProfile>) =>
    apiFetch<UserProfile>('/api/users/me', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  // 실제 계정 엔드포인트 (/api/me) — Agent-H 신규.
  getAccount: () => apiFetch<AccountProfile>('/api/me'),
  updateProfile: (body: EditProfilePayload) =>
    apiFetch<AccountProfile>('/api/me', {
      method: 'PATCH',
      body: JSON.stringify(body),
    }),
  changePassword: (body: ChangePasswordPayload) =>
    apiFetch<void>('/api/me/password', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  withdraw: () => apiFetch<void>('/api/me', { method: 'DELETE' }),
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
