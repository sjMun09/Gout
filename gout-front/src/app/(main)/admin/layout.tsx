'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useCallback, useEffect, useState } from 'react'
import { adminApi } from '@/lib/api'

/**
 * 관리자 전용 레이아웃.
 *
 * 현재 백엔드에 /api/me 류 엔드포인트가 없고 JWT 클레임에도 role 이 포함되지 않아
 * 클라이언트만으로 ADMIN 여부를 확인할 수 없다.
 * → /api/admin/users?size=1 을 프로브로 호출해서
 *   200 = ADMIN 확정 / 401, 403 = 권한 없음 → "/" 로 리다이렉트.
 */
export default function AdminLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const router = useRouter()
  const pathname = usePathname()
  const [authorized, setAuthorized] = useState<boolean | null>(null)

  const probe = useCallback(async () => {
    if (typeof window === 'undefined') return
    const token = localStorage.getItem('accessToken')
    if (!token) {
      router.replace('/')
      return
    }
    try {
      await adminApi.listUsers({ page: 0, size: 1 })
      setAuthorized(true)
    } catch {
      setAuthorized(false)
      router.replace('/')
    }
  }, [router])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    probe()
  }, [probe])

  if (authorized !== true) {
    return (
      <div className="flex min-h-[60vh] items-center justify-center px-5 py-6 text-sm text-gray-500">
        관리자 권한을 확인하는 중…
      </div>
    )
  }

  const tabs = [
    { href: '/admin/users', label: '유저' },
    { href: '/admin/posts', label: '게시글' },
    { href: '/admin/reports', label: '신고' },
  ]

  return (
    <div className="flex flex-col gap-4 px-5 py-6">
      <header>
        <h1 className="text-xl font-bold text-gray-900">관리자 패널</h1>
        <p className="mt-1 text-sm text-gray-500">
          유저 / 게시글 / 신고 처리
        </p>
      </header>
      <nav
        aria-label="관리자 메뉴"
        className="flex gap-2 overflow-x-auto border-b border-gray-200"
      >
        {tabs.map((tab) => {
          const active = pathname?.startsWith(tab.href)
          return (
            <Link
              key={tab.href}
              href={tab.href}
              className={`min-h-[44px] shrink-0 px-4 py-2 text-sm font-medium transition-colors ${
                active
                  ? 'border-b-2 border-blue-600 text-blue-700'
                  : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              {tab.label}
            </Link>
          )
        })}
      </nav>
      <div>{children}</div>
    </div>
  )
}
