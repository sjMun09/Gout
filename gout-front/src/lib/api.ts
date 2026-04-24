import type { FoodDetail, FoodItem, Hospital, PagedResponse } from '@/types'
import type {
  MarkAllReadResponse,
  NotificationItem,
  UnreadCountResponse,
} from '@/types/notification'

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

/**
 * API 호출 시 던져지는 에러. 응답 body 가 있으면 파싱된 server message 를 그대로 싣는다.
 * 호출측에서 status 별 분기(예: 409 중복) 가 필요할 때 사용.
 */
export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
    this.name = 'ApiError'
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

  // 응답 body 는 항상 읽어본다 — 서버가 ApiResponse.error 로 message 를 내려주기 때문에
  // !res.ok 여도 본문의 message 를 에러에 실어야 호출측에서 사용자 안내가 가능하다.
  const text = await res.text()
  let json: unknown = null
  if (text) {
    try {
      json = JSON.parse(text)
    } catch {
      // 텍스트 응답 (드문 케이스) — 그대로 메시지로 사용
    }
  }

  if (!res.ok) {
    const message =
      (json as { message?: string } | null)?.message ??
      (typeof json === 'string' ? json : null) ??
      `API ${res.status}: ${res.statusText}`
    throw new ApiError(res.status, message)
  }

  if (json && typeof json === 'object' && (json as { success?: boolean }).success === false) {
    throw new ApiError(
      res.status,
      (json as { message?: string }).message ?? 'API 요청 실패',
    )
  }
  return (json as { data: T }).data
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
  imageUrls?: string[]
  tags?: string[]
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
  bookmarkCount?: number
  bookmarked?: boolean
  tags?: string[]
}

export interface CreatePostPayload {
  title: string
  content: string
  category: string
  isAnonymous: boolean
  imageUrls?: string[]
  tags?: string[]
}

export interface BookmarkStatus {
  bookmarked: boolean
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

export type PostSort = 'latest' | 'popular' | 'views'

export const communityApi = {
  getPosts: (params: {
    category?: string
    keyword?: string
    sort?: PostSort
    tag?: string
    page?: number
    size?: number
  }) => {
    const qs = new URLSearchParams()
    if (params.category) qs.set('category', params.category)
    if (params.keyword) qs.set('keyword', params.keyword)
    if (params.sort) qs.set('sort', params.sort)
    if (params.tag) qs.set('tag', params.tag)
    qs.set('page', String(params.page ?? 0))
    qs.set('size', String(params.size ?? 20))
    return apiFetch<PagedResponse<PostSummary>>(`/api/posts?${qs.toString()}`)
  },
  getTrending: (params: { days?: number; limit?: number } = {}) => {
    const qs = new URLSearchParams()
    qs.set('days', String(params.days ?? 7))
    qs.set('limit', String(params.limit ?? 5))
    return apiFetch<PostSummary[]>(`/api/posts/trending?${qs.toString()}`)
  },
  getPost: (id: string) => apiFetch<PostDetail>(`/api/posts/${id}`),
  createPost: (body: CreatePostPayload) =>
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

// ===== 게시글 이미지 업로드 =====
// multipart/form-data 업로드는 Content-Type 을 수동으로 두면 안 되므로
// apiFetch 를 그대로 쓰지 않고 여기서 직접 fetch 한다.
// 응답은 { urls: string[] } — 상대 URL 배열 (예: /api/uploads/posts/xxx.png).

export const postImageApi = {
  upload: async (files: File[]): Promise<string[]> => {
    if (!files || files.length === 0) return []
    const token =
      typeof window !== 'undefined'
        ? localStorage.getItem('accessToken')
        : null
    const formData = new FormData()
    files.forEach((f) => formData.append('files', f))

    const res = await fetch(`${API_BASE}/api/uploads/posts`, {
      method: 'POST',
      headers: {
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      body: formData,
    })
    if (!res.ok) {
      throw new Error(`업로드 실패: ${res.status}`)
    }
    const json = await res.json()
    if (json?.success === false) {
      throw new Error(json.message ?? '이미지 업로드 실패')
    }
    const urls: string[] | undefined = json?.data?.urls
    return Array.isArray(urls) ? urls : []
  },
  /** 상대 URL → 절대 URL 변환 (백엔드 베이스 붙이기). 이미 http 면 그대로. */
  absolute: (url: string): string => {
    if (!url) return url
    if (/^https?:\/\//i.test(url)) return url
    return `${API_BASE}${url}`
  },
}

// ===== 북마크 API =====

export const bookmarkApi = {
  toggle: (postId: string) =>
    apiFetch<{ bookmarked: boolean }>(`/api/posts/${postId}/bookmark`, {
      method: 'POST',
    }),
  list: (page = 0, size = 20) => {
    const qs = new URLSearchParams()
    qs.set('page', String(page))
    qs.set('size', String(size))
    return apiFetch<PagedResponse<PostSummary>>(
      `/api/me/bookmarks?${qs.toString()}`,
    )
  },
  status: (postId: string) =>
    apiFetch<BookmarkStatus>(`/api/posts/${postId}/bookmark-status`),
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

// ===== 알림 API =====

export const notificationApi = {
  list: (params: { page?: number; size?: number } = {}) => {
    const qs = new URLSearchParams()
    qs.set('page', String(params.page ?? 0))
    qs.set('size', String(params.size ?? 20))
    return apiFetch<PagedResponse<NotificationItem>>(
      `/api/notifications?${qs.toString()}`,
    )
  },
  unreadCount: () =>
    apiFetch<UnreadCountResponse>('/api/notifications/unread-count'),
  markRead: (id: string) =>
    apiFetch<void>(`/api/notifications/${id}/read`, { method: 'POST' }),
  markAllRead: () =>
    apiFetch<MarkAllReadResponse>('/api/notifications/read-all', {
      method: 'POST',
    }),
}

// ===== 신고 타입 =====

export type ReportTargetType = 'POST' | 'COMMENT'
export type ReportReason = 'SPAM' | 'ABUSE' | 'SEXUAL' | 'MISINFO' | 'ETC'

export const REPORT_REASON_LABELS: Record<ReportReason, string> = {
  SPAM: '스팸/광고',
  ABUSE: '욕설/비방',
  SEXUAL: '음란성',
  MISINFO: '허위 정보',
  ETC: '기타',
}

export interface ReportResponse {
  id: string
  targetType: ReportTargetType
  targetId: string
  reporterId: string
  reason: ReportReason
  detail?: string
  status: 'PENDING' | 'RESOLVED' | 'DISMISSED'
  createdAt: string
  resolvedAt?: string
}

// ===== 신고 API =====

export const reportApi = {
  create: (
    targetType: ReportTargetType,
    targetId: string,
    reason: ReportReason,
    detail?: string,
  ) =>
    apiFetch<ReportResponse>('/api/reports', {
      method: 'POST',
      body: JSON.stringify({ targetType, targetId, reason, detail }),
    }),
}
