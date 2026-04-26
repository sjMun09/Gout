'use client'

import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { Suspense } from 'react'
import { useAuth } from '@/lib/auth'
import { AttackTab } from './_components/attack-tab'
import { MedicationTab } from './_components/medication-tab'
import { HeaderBlock } from './_components/shared'
import { UricAcidTab } from './_components/uric-acid-tab'

type TabKey = 'uric' | 'attack' | 'medication'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'uric', label: '요산수치' },
  { key: 'attack', label: '발작일지' },
  { key: 'medication', label: '복약' },
]

function isValidTab(value: string | null): value is TabKey {
  return value === 'uric' || value === 'attack' || value === 'medication'
}

export default function RecordPage() {
  return (
    <Suspense fallback={<RecordSkeleton />}>
      <RecordContent />
    </Suspense>
  )
}

function RecordContent() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const tabParam = searchParams.get('tab')
  const activeTab: TabKey = isValidTab(tabParam) ? tabParam : 'uric'
  const { isAuthenticated, isHydrated, accessToken } = useAuth()
  const hasToken: boolean | null = isHydrated ? isAuthenticated : null

  const changeTab = (key: TabKey) => {
    const params = new URLSearchParams(searchParams.toString())
    params.set('tab', key)
    router.replace(`/record?${params.toString()}`)
  }

  if (hasToken === null) return <RecordSkeleton />
  if (!hasToken) return <LoginRequired />

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <HeaderBlock />

      <div
        className="flex rounded-2xl bg-gray-100 p-1"
        role="tablist"
        aria-label="기록 종류"
      >
        {TABS.map((tab) => (
          <button
            key={tab.key}
            id={`tab-${tab.key}`}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.key}
            aria-controls={`tabpanel-${tab.key}`}
            onClick={() => changeTab(tab.key)}
            className={`min-h-[48px] flex-1 rounded-xl text-base font-medium transition-colors ${
              activeTab === tab.key
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      <TabPanel activeTab={activeTab} accessToken={accessToken} />
    </div>
  )
}

function RecordSkeleton() {
  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <HeaderBlock />
      <div className="h-60 animate-pulse rounded-2xl bg-gray-100" />
    </div>
  )
}

function LoginRequired() {
  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center gap-4 px-5 py-10 text-center">
      <h1 className="text-2xl font-bold text-gray-900">로그인이 필요해요</h1>
      <p className="text-base text-gray-600">
        건강 기록은 로그인 후 이용할 수 있어요
      </p>
      <Link
        href="/login"
        className="mt-2 inline-flex min-h-[48px] items-center justify-center rounded-xl bg-blue-600 px-6 text-base font-semibold text-white hover:bg-blue-700"
      >
        로그인 하러 가기
      </Link>
    </div>
  )
}

function TabPanel({
  activeTab,
  accessToken,
}: {
  activeTab: TabKey
  accessToken?: string | null
}) {
  if (activeTab === 'uric') {
    return (
      <div id="tabpanel-uric" role="tabpanel" aria-labelledby="tab-uric">
        <UricAcidTab accessToken={accessToken} />
      </div>
    )
  }

  if (activeTab === 'attack') {
    return (
      <div id="tabpanel-attack" role="tabpanel" aria-labelledby="tab-attack">
        <AttackTab />
      </div>
    )
  }

  return (
    <div
      id="tabpanel-medication"
      role="tabpanel"
      aria-labelledby="tab-medication"
    >
      <MedicationTab />
    </div>
  )
}
