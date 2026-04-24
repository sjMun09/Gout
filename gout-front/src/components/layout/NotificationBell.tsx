'use client'

import Link from 'next/link'
import { Bell } from 'lucide-react'
import { useUnreadNotifications } from '@/hooks/useUnreadNotifications'
import { cn } from '@/lib/utils'

/**
 * 우상단에 고정되는 벨 아이콘. 안읽은 알림 수를 빨간 뱃지로 노출.
 * /notifications 로 이동한다.
 */
export default function NotificationBell() {
  const { count } = useUnreadNotifications()
  const display = count > 99 ? '99+' : String(count)

  return (
    <Link
      href="/notifications"
      aria-label={
        count > 0 ? `알림 ${count}개 있음` : '알림 센터로 이동'
      }
      className={cn(
        'fixed top-3 right-3 z-50 inline-flex h-10 w-10 items-center justify-center',
        'rounded-full bg-white/90 text-gray-700 shadow-sm backdrop-blur',
        'hover:bg-white hover:text-gray-900 transition-colors',
      )}
    >
      <Bell className="h-5 w-5" aria-hidden="true" />
      {count > 0 && (
        <span
          className={cn(
            'absolute -top-0.5 -right-0.5 inline-flex min-w-[18px] h-[18px] px-1',
            'items-center justify-center rounded-full bg-red-500 text-[10px] font-bold text-white',
          )}
          aria-hidden="true"
        >
          {display}
        </span>
      )}
    </Link>
  )
}
