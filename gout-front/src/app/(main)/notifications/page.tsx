'use client'

import { useCallback, useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Bell, CheckCheck } from 'lucide-react'
import { notificationApi } from '@/lib/api'
import type { NotificationItem } from '@/types/notification'
import { cn } from '@/lib/utils'

function formatRelative(iso: string): string {
  const now = Date.now()
  const then = new Date(iso).getTime()
  if (Number.isNaN(then)) return ''
  const diffSec = Math.max(0, Math.floor((now - then) / 1000))
  if (diffSec < 60) return '방금 전'
  const diffMin = Math.floor(diffSec / 60)
  if (diffMin < 60) return `${diffMin}분 전`
  const diffHr = Math.floor(diffMin / 60)
  if (diffHr < 24) return `${diffHr}시간 전`
  const diffDay = Math.floor(diffHr / 24)
  if (diffDay < 7) return `${diffDay}일 전`
  return new Date(iso).toLocaleDateString('ko-KR')
}

export default function NotificationsPage() {
  const router = useRouter()
  const [items, setItems] = useState<NotificationItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const res = await notificationApi.list({ page: 0, size: 50 })
      setItems(res.content ?? [])
    } catch {
      setError('알림을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const handleMarkAll = async () => {
    try {
      await notificationApi.markAllRead()
      await load()
    } catch {
      setError('알림을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
    }
  }

  const handleItemClick = async (n: NotificationItem) => {
    // 낙관적 업데이트
    if (!n.read) {
      setItems((prev) =>
        prev.map((it) => (it.id === n.id ? { ...it, read: true } : it)),
      )
      try {
        await notificationApi.markRead(n.id)
      } catch {
        // 실패해도 이동은 막지 않는다 (다음 폴링에서 복구)
      }
    }
    if (n.link) {
      router.push(n.link)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="sticky top-0 z-10 flex items-center justify-between border-b border-gray-200 bg-white px-4 py-3">
        <h1 className="text-lg font-semibold text-gray-900">알림</h1>
        <button
          type="button"
          onClick={handleMarkAll}
          className="inline-flex items-center gap-1 text-sm text-blue-600 hover:text-blue-700 disabled:text-gray-400"
          disabled={items.every((i) => i.read)}
        >
          <CheckCheck className="h-4 w-4" aria-hidden="true" />
          <span>전체 읽음</span>
        </button>
      </header>

      <main className="px-4 py-3">
        {loading && (
          <p className="py-8 text-center text-sm text-gray-500">
            불러오는 중...
          </p>
        )}
        {error && (
          <div role="alert" className="flex flex-col items-center gap-3 py-8">
            <p className="text-center text-sm text-red-500">{error}</p>
            <button
              type="button"
              onClick={load}
              className="rounded-lg border border-red-300 px-4 py-2 text-sm font-medium text-red-600 hover:bg-red-50"
            >
              다시 시도
            </button>
          </div>
        )}
        {!loading && !error && items.length === 0 && (
          <div className="flex flex-col items-center gap-2 py-16 text-gray-500">
            <Bell className="h-10 w-10 text-gray-300" aria-hidden="true" />
            <p className="text-sm">새로운 알림이 없습니다.</p>
          </div>
        )}

        <ul className="flex flex-col gap-2">
          {items.map((n) => (
            <li key={n.id}>
              <button
                type="button"
                onClick={() => handleItemClick(n)}
                className={cn(
                  'flex w-full flex-col items-start gap-1 rounded-lg border px-3 py-3 text-left transition-colors',
                  n.read
                    ? 'border-gray-200 bg-white hover:bg-gray-50'
                    : 'border-blue-100 bg-blue-50 hover:bg-blue-100',
                )}
              >
                <div className="flex w-full items-center justify-between">
                  <span
                    className={cn(
                      'text-sm font-medium',
                      n.read ? 'text-gray-700' : 'text-gray-900',
                    )}
                  >
                    {n.title}
                  </span>
                  <span className="text-xs text-gray-500">
                    {formatRelative(n.createdAt)}
                  </span>
                </div>
                {n.body && (
                  <p className="line-clamp-2 text-sm text-gray-600">{n.body}</p>
                )}
              </button>
            </li>
          ))}
        </ul>
      </main>
    </div>
  )
}
