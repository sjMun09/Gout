import type { PagedResponse } from '@/types'
import { getAccessToken } from '@/lib/auth/storage'
import type { PostCategoryKey } from '@/constants'
import { API_BASE, apiFetch } from './client'

export { CATEGORY_LABELS } from '@/constants'

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
  category: PostCategoryKey
  isAnonymous: boolean
  imageUrls?: string[]
  tags?: string[]
}

export interface BookmarkStatus {
  bookmarked: boolean
}

// ===== 커뮤니티 API =====

export type PostSort = 'latest' | 'popular' | 'views'

export const communityApi = {
  getPosts: (params: {
    category?: PostCategoryKey | string
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
    const token = getAccessToken()
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
