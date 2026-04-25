import type {
  MarkAllReadResponse,
  NotificationItem,
  UnreadCountResponse,
} from '@/types/notification'
import type { PagedResponse } from '@/types'
import { apiFetch } from './client'

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
