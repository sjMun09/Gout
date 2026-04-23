import Link from 'next/link'
import {
  Utensils,
  Activity,
  AlertTriangle,
  MapPin,
  Lightbulb,
  Siren,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'

interface QuickAction {
  href: string
  label: string
  description: string
  icon: LucideIcon
  color: string
}

const quickActions: QuickAction[] = [
  {
    href: '/food',
    label: '음식 확인',
    description: '퓨린 신호등',
    icon: Utensils,
    color: 'bg-green-50 text-green-700',
  },
  {
    href: '/record?tab=uric',
    label: '수치 입력',
    description: '요산 기록',
    icon: Activity,
    color: 'bg-blue-50 text-blue-700',
  },
  {
    href: '/record?tab=attack',
    label: '발작 기록',
    description: '통증 일지',
    icon: AlertTriangle,
    color: 'bg-orange-50 text-orange-700',
  },
  {
    href: '/hospital',
    label: '병원 찾기',
    description: '내 주변',
    icon: MapPin,
    color: 'bg-purple-50 text-purple-700',
  },
]

export default function HomePage() {
  return (
    <div className="flex flex-col gap-6 px-5 py-6">
      {/* 인사말 */}
      <section>
        <h1 className="text-2xl font-bold text-gray-900">
          안녕하세요 <span aria-hidden="true">👋</span>
        </h1>
        <p className="mt-1 text-base text-gray-600">
          오늘도 관리 잘 하고 계신가요?
        </p>
      </section>

      {/* 퀵 액션 2x2 그리드 */}
      <section aria-labelledby="quick-actions-title">
        <h2
          id="quick-actions-title"
          className="mb-3 text-lg font-semibold text-gray-900"
        >
          빠른 기능
        </h2>
        <div className="grid grid-cols-2 gap-3">
          {quickActions.map((action) => {
            const Icon = action.icon
            return (
              <Link
                key={action.href}
                href={action.href}
                className="flex min-h-[112px] flex-col items-start justify-between rounded-2xl border border-gray-200 bg-white p-4 shadow-sm transition-colors hover:bg-gray-50"
              >
                <span
                  className={`inline-flex h-10 w-10 items-center justify-center rounded-full ${action.color}`}
                >
                  <Icon className="h-5 w-5" aria-hidden="true" />
                </span>
                <div>
                  <p className="text-base font-semibold text-gray-900">
                    {action.label}
                  </p>
                  <p className="text-sm text-gray-500">{action.description}</p>
                </div>
              </Link>
            )
          })}
        </div>
      </section>

      {/* 오늘의 팁 */}
      <section aria-labelledby="tip-title">
        <h2
          id="tip-title"
          className="mb-3 text-lg font-semibold text-gray-900"
        >
          오늘의 팁
        </h2>
        <div className="flex items-start gap-3 rounded-2xl border border-blue-100 bg-blue-50 p-4">
          <span className="mt-0.5 inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-blue-100 text-blue-700">
            <Lightbulb className="h-5 w-5" aria-hidden="true" />
          </span>
          <p className="text-base leading-6 text-blue-900">
            물을 하루 2리터 이상 드시면 요산 배출에 도움이 됩니다.
          </p>
        </div>
      </section>

      {/* SOS 버튼 */}
      <section aria-labelledby="sos-title" className="mt-2">
        <h2 id="sos-title" className="sr-only">
          응급 가이드
        </h2>
        <Link
          href="/emergency"
          className="flex min-h-[56px] w-full items-center justify-center gap-2 rounded-2xl bg-red-600 px-5 py-4 text-base font-bold text-white shadow-sm transition-colors hover:bg-red-700"
        >
          <Siren className="h-5 w-5" aria-hidden="true" />
          발작 중이신가요? 응급 가이드
        </Link>
      </section>
    </div>
  )
}
