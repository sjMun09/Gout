import type { PagedResponse } from '@/types'
import { API_BASE } from './client'

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
