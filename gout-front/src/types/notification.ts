// 인앱 알림 타입

export type NotificationType =
  | 'COMMENT_ON_POST'
  | 'REPLY_ON_COMMENT'
  | 'POST_LIKE'

export interface NotificationItem {
  id: string
  type: NotificationType | string
  title: string
  body?: string
  link?: string
  read: boolean
  readAt?: string | null
  createdAt: string
}

export interface UnreadCountResponse {
  count: number
}

export interface MarkAllReadResponse {
  updated: number
}
