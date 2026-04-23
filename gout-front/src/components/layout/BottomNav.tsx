'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { Home, Utensils, ClipboardList, MapPin, Menu } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { cn } from '@/lib/utils'

interface BottomNavItem {
  href: string
  label: string
  icon: LucideIcon
  ariaLabel: string
}

const navItems: BottomNavItem[] = [
  { href: '/home', label: '홈', icon: Home, ariaLabel: '홈으로 이동' },
  { href: '/food', label: '음식', icon: Utensils, ariaLabel: '음식 정보' },
  { href: '/record', label: '기록', icon: ClipboardList, ariaLabel: '건강 기록' },
  { href: '/hospital', label: '병원', icon: MapPin, ariaLabel: '병원 찾기' },
  { href: '/more', label: '더보기', icon: Menu, ariaLabel: '더보기' },
]

export default function BottomNav() {
  const pathname = usePathname()

  return (
    <nav
      aria-label="주요 메뉴"
      className="fixed bottom-0 left-1/2 -translate-x-1/2 w-full max-w-md h-16 border-t border-gray-200 bg-white z-50"
    >
      <ul className="flex h-full items-stretch justify-around">
        {navItems.map((item) => {
          const Icon = item.icon
          const isActive =
            pathname === item.href || pathname?.startsWith(`${item.href}/`)

          return (
            <li key={item.href} className="flex-1">
              <Link
                href={item.href}
                aria-label={item.ariaLabel}
                aria-current={isActive ? 'page' : undefined}
                className={cn(
                  'flex h-full flex-col items-center justify-center gap-0.5 text-xs font-medium transition-colors',
                  'min-h-[48px]',
                  isActive
                    ? 'text-blue-600'
                    : 'text-gray-500 hover:text-gray-800',
                )}
              >
                <Icon
                  className={cn('h-6 w-6', isActive && 'stroke-[2.5]')}
                  aria-hidden="true"
                />
                <span className="text-[13px] leading-none">{item.label}</span>
              </Link>
            </li>
          )
        })}
      </ul>
    </nav>
  )
}
