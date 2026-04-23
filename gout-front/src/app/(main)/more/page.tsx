import Link from 'next/link'
import {
  BookOpen,
  Users,
  Dumbbell,
  FileText,
  MessageCircle,
  Settings,
  ChevronRight,
  UserCircle2,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

interface MoreItem {
  href: string
  label: string
  description: string
  icon: LucideIcon
}

const moreItems: MoreItem[] = [
  {
    href: '/profile',
    label: '내 정보',
    description: '닉네임·연령대·목표 수치',
    icon: UserCircle2,
  },
  {
    href: '/encyclopedia',
    label: '통풍 백과',
    description: '원인부터 관리법까지',
    icon: BookOpen,
  },
  {
    href: '/age-info',
    label: '연령별 정보',
    description: '20대·40대·60대 맞춤',
    icon: Users,
  },
  {
    href: '/exercise',
    label: '운동 가이드',
    description: '통풍 환자에게 맞는 운동',
    icon: Dumbbell,
  },
  {
    href: '/research',
    label: '논문 정보',
    description: '최신 연구 요약',
    icon: FileText,
  },
  {
    href: '/community',
    label: '커뮤니티',
    description: '같은 고민을 가진 분들과',
    icon: MessageCircle,
  },
  {
    href: '/settings',
    label: '설정',
    description: '알림·계정·앱 설정',
    icon: Settings,
  },
]

export default function MorePage() {
  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <header>
        <h1 className="text-2xl font-bold text-gray-900">더보기</h1>
        <p className="mt-1 text-base text-gray-600">
          학습·커뮤니티·설정을 한 곳에서
        </p>
      </header>

      <ul className="flex flex-col gap-2">
        {moreItems.map((item) => {
          const Icon = item.icon
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                className="flex min-h-[64px] items-center gap-3 rounded-2xl border border-gray-200 bg-white p-4 transition-colors hover:bg-gray-50"
              >
                <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-700">
                  <Icon className="h-5 w-5" aria-hidden="true" />
                </span>
                <div className="flex-1">
                  <p className="text-base font-semibold text-gray-900">
                    {item.label}
                  </p>
                  <p className="text-sm text-gray-500">{item.description}</p>
                </div>
                <ChevronRight
                  className="h-5 w-5 text-gray-400"
                  aria-hidden="true"
                />
              </Link>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
