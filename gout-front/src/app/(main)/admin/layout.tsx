'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { hasAdminRole } from '@/lib/api'

/**
 * 관리자 전용 레이아웃.
 *
 * accessToken 의 roles 클레임(JwtTokenProvider 가 access 토큰에 포함)으로
 * ADMIN 여부를 클라이언트에서 바로 판정한다. 실제 인가는 백엔드가 수행하므로
 * 이 판정은 UX 라우팅용. 토큰 부재/형식 오류/ADMIN 아님 → "/" 로 리다이렉트.
 *
 * (과거: /api/admin/users?size=1 을 프로브로 호출 — 불필요한 유저 데이터 읽기 발생.
 *  감사 MED-005 로 개선.)
 */
export default function AdminLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const router = useRouter()
  const pathname = usePathname()
  const [authorized, setAuthorized] = useState<boolean | null>(null)

  useEffect(() => {
    if (typeof window === 'undefined') return
    if (hasAdminRole()) {
      setAuthorized(true)
      return
    }
    setAuthorized(false)
    router.replace('/')
  }, [router])

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
