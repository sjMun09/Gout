'use client'

import Link from 'next/link'
import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  Utensils,
  Activity,
  AlertTriangle,
  MapPin,
  Lightbulb,
  Siren,
  Pill,
  MessageCircle,
  ArrowRight,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import {
  CATEGORY_LABELS,
  type MedicationLog,
  type PostSummary,
  type UricAcidLog,
  communityApi,
  healthApi,
} from '@/lib/api'
import { formatDateKr, formatDateTimeKr } from '@/lib/date'
import { TrendingUp } from 'lucide-react'

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

type FetchState<T> = {
  loading: boolean
  error: string | null
  data: T | null
}

const initialState = <T,>(): FetchState<T> => ({
  loading: true,
  error: null,
  data: null,
})

export default function HomePage() {
  const [hasToken, setHasToken] = useState<boolean | null>(null)
  const [uricState, setUricState] = useState<FetchState<UricAcidLog | null>>(
    initialState(),
  )
  const [medState, setMedState] = useState<FetchState<MedicationLog | null>>(
    initialState(),
  )
  const [postsState, setPostsState] = useState<FetchState<PostSummary[]>>(
    initialState(),
  )
  const [trendingState, setTrendingState] = useState<FetchState<PostSummary[]>>(
    initialState(),
  )

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setHasToken(
      typeof window !== 'undefined'
        ? !!localStorage.getItem('accessToken')
        : false,
    )
  }, [])

  const loadUric = useCallback(async () => {
    setUricState({ loading: true, error: null, data: null })
    try {
      const logs = await healthApi.getUricAcidLogs()
      const latest =
        [...logs].sort(
          (a, b) =>
            new Date(b.measuredAt).getTime() -
            new Date(a.measuredAt).getTime(),
        )[0] ?? null
      setUricState({ loading: false, error: null, data: latest })
    } catch (e) {
      setUricState({
        loading: false,
        error: e instanceof Error ? e.message : '요청 실패',
        data: null,
      })
    }
  }, [])

  const loadMed = useCallback(async () => {
    setMedState({ loading: true, error: null, data: null })
    try {
      const logs = await healthApi.getMedicationLogs()
      const latest =
        [...logs].sort(
          (a, b) =>
            new Date(b.takenAt).getTime() - new Date(a.takenAt).getTime(),
        )[0] ?? null
      setMedState({ loading: false, error: null, data: latest })
    } catch (e) {
      setMedState({
        loading: false,
        error: e instanceof Error ? e.message : '요청 실패',
        data: null,
      })
    }
  }, [])

  const loadPosts = useCallback(async () => {
    setPostsState({ loading: true, error: null, data: null })
    try {
      const page = await communityApi.getPosts({ page: 0, size: 3 })
      setPostsState({ loading: false, error: null, data: page.content })
    } catch (e) {
      setPostsState({
        loading: false,
        error: e instanceof Error ? e.message : '요청 실패',
        data: null,
      })
    }
  }, [])

  const loadTrending = useCallback(async () => {
    setTrendingState({ loading: true, error: null, data: null })
    try {
      const list = await communityApi.getTrending({ days: 7, limit: 5 })
      setTrendingState({ loading: false, error: null, data: list })
    } catch {
      // 실패 시 섹션 자체를 숨기므로 error 상태만 기록하고 재시도 UI는 노출하지 않는다
      setTrendingState({ loading: false, error: 'fetch_failed', data: null })
    }
  }, [])

  useEffect(() => {
    if (hasToken === null) return
    // 커뮤니티 API 는 공개 — 비로그인도 시도
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadPosts()
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadTrending()
    if (hasToken) {
      loadUric()
      loadMed()
    }
  }, [hasToken, loadPosts, loadTrending, loadUric, loadMed])

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

      {/* 비로그인 프롬프트 */}
      {hasToken === false && (
        <section>
          <div className="flex items-center justify-between gap-3 rounded-2xl border border-blue-100 bg-blue-50 p-4">
            <p className="text-base text-blue-900">
              로그인하면 개인 수치·복약 기록을 볼 수 있어요
            </p>
            <Link
              href="/login"
              className="inline-flex min-h-[40px] shrink-0 items-center justify-center rounded-xl bg-blue-600 px-4 text-sm font-semibold text-white hover:bg-blue-700"
            >
              로그인
            </Link>
          </div>
        </section>
      )}

      {/* 개인 요약 — 로그인 시만 */}
      {hasToken && (
        <section aria-labelledby="my-summary-title">
          <h2
            id="my-summary-title"
            className="mb-3 text-lg font-semibold text-gray-900"
          >
            내 기록 요약
          </h2>
          <div className="flex flex-col gap-3">
            <UricAcidCard state={uricState} />
            <MedicationCard state={medState} />
          </div>
        </section>
      )}

      {/* 커뮤니티 최신 3건 */}
      <section aria-labelledby="community-latest-title">
        <div className="mb-3 flex items-center justify-between">
          <h2
            id="community-latest-title"
            className="text-lg font-semibold text-gray-900"
          >
            커뮤니티 최신글
          </h2>
          <Link
            href="/community"
            className="inline-flex items-center gap-1 text-sm font-medium text-blue-600 hover:text-blue-700"
          >
            전체 보기
            <ArrowRight className="h-4 w-4" aria-hidden="true" />
          </Link>
        </div>
        <CommunityLatest state={postsState} />
      </section>

      {/* 트렌딩 섹션 — API 실패 시 숨김 */}
      {!trendingState.error && (
        <section aria-labelledby="trending-title">
          <div className="mb-3 flex items-center justify-between">
            <h2
              id="trending-title"
              className="text-lg font-semibold text-gray-900"
            >
              지금 인기 있는 글
            </h2>
            <Link
              href="/community"
              className="inline-flex items-center gap-1 text-sm font-medium text-blue-600 hover:text-blue-700"
            >
              전체 보기
              <ArrowRight className="h-4 w-4" aria-hidden="true" />
            </Link>
          </div>
          <TrendingSection state={trendingState} />
        </section>
      )}

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

/* ========== 서브 컴포넌트 ========== */

function SectionSkeleton() {
  return <div className="h-24 animate-pulse rounded-2xl bg-gray-100" />
}

function SectionError({ onRetry }: { onRetry?: () => void }) {
  return (
    <div
      role="alert"
      aria-live="polite"
      className="rounded-2xl border border-gray-200 bg-white p-4 text-sm text-gray-600"
    >
      데이터를 불러오지 못했어요
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="ml-2 font-semibold text-blue-600 hover:underline"
        >
          다시 시도
        </button>
      )}
    </div>
  )
}

function EmptyCta({
  message,
  href,
  cta,
}: {
  message: string
  href: string
  cta: string
}) {
  return (
    <Link
      href={href}
      className="flex items-center justify-between rounded-2xl border border-dashed border-gray-200 bg-white p-4 text-sm text-gray-600 hover:bg-gray-50"
    >
      <span>{message}</span>
      <span className="inline-flex items-center gap-1 text-blue-600">
        {cta}
        <ArrowRight className="h-4 w-4" aria-hidden="true" />
      </span>
    </Link>
  )
}

function UricAcidCard({
  state,
}: {
  state: FetchState<UricAcidLog | null>
}) {
  const badge = useMemo(() => {
    if (!state.data) return null
    const normal = state.data.value <= 6.0
    return (
      <span
        className={`rounded-full px-2 py-0.5 text-xs font-medium ${
          normal
            ? 'bg-blue-100 text-blue-700'
            : 'bg-red-100 text-red-700'
        }`}
      >
        {normal ? '목표 도달' : '관리 필요'}
      </span>
    )
  }, [state.data])

  if (state.loading) return <SectionSkeleton />
  if (state.error) return <SectionError />
  if (!state.data) {
    return (
      <EmptyCta
        message="요산수치 기록이 아직 없어요"
        href="/record?tab=uric"
        cta="기록 추가"
      />
    )
  }

  return (
    <Link
      href="/record?tab=uric"
      className="block rounded-2xl border border-gray-200 bg-white p-4 hover:bg-gray-50"
    >
      <div className="flex items-center gap-3">
        <span className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-700">
          <Activity className="h-5 w-5" aria-hidden="true" />
        </span>
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <span className="text-sm text-gray-600">최근 요산수치</span>
            {badge}
          </div>
          <p className="mt-0.5 text-xl font-bold text-gray-900">
            {state.data.value.toFixed(1)}{' '}
            <span className="text-sm font-normal text-gray-600">mg/dL</span>
          </p>
          <p className="text-xs text-gray-500">
            측정일 {formatDateKr(state.data.measuredAt)}
          </p>
        </div>
      </div>
    </Link>
  )
}

function MedicationCard({
  state,
}: {
  state: FetchState<MedicationLog | null>
}) {
  if (state.loading) return <SectionSkeleton />
  if (state.error) return <SectionError />
  if (!state.data) {
    return (
      <EmptyCta
        message="복약 기록이 아직 없어요"
        href="/record?tab=medication"
        cta="기록 추가"
      />
    )
  }

  return (
    <Link
      href="/record?tab=medication"
      className="block rounded-2xl border border-gray-200 bg-white p-4 hover:bg-gray-50"
    >
      <div className="flex items-center gap-3">
        <span className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-green-50 text-green-700">
          <Pill className="h-5 w-5" aria-hidden="true" />
        </span>
        <div className="flex-1">
          <p className="text-sm text-gray-600">최근 복약</p>
          <p className="mt-0.5 text-base font-semibold text-gray-900">
            {state.data.medicationName}
            {state.data.dosage ? (
              <span className="ml-1 text-sm font-normal text-gray-600">
                {state.data.dosage}
              </span>
            ) : null}
          </p>
          <p className="text-xs text-gray-500">
            {formatDateTimeKr(state.data.takenAt)}
          </p>
        </div>
      </div>
    </Link>
  )
}

function TrendingSection({ state }: { state: FetchState<PostSummary[]> }) {
  if (state.loading) {
    return (
      <div className="flex flex-col gap-2">
        <SectionSkeleton />
        <SectionSkeleton />
        <SectionSkeleton />
      </div>
    )
  }
  const list = state.data ?? []
  if (list.length === 0) return null
  return (
    <ul className="flex flex-col gap-2">
      {list.map((post) => (
        <li key={post.id}>
          <Link
            href={`/community/${post.id}`}
            className="flex items-center gap-3 rounded-2xl border border-gray-200 bg-white p-4 hover:bg-gray-50"
          >
            <span className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-orange-50 text-orange-600">
              <TrendingUp className="h-5 w-5" aria-hidden="true" />
            </span>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs text-orange-700">
                  {CATEGORY_LABELS[post.category] ?? post.category}
                </span>
              </div>
              <p className="mt-0.5 truncate text-base font-semibold text-gray-900">
                {post.title}
              </p>
              <p className="text-xs text-gray-500">
                좋아요 {post.likeCount} · 조회 {post.viewCount} · 댓글 {post.commentCount}
              </p>
            </div>
          </Link>
        </li>
      ))}
    </ul>
  )
}

function CommunityLatest({ state }: { state: FetchState<PostSummary[]> }) {
  if (state.loading) {
    return (
      <div className="flex flex-col gap-2">
        <SectionSkeleton />
        <SectionSkeleton />
        <SectionSkeleton />
      </div>
    )
  }
  if (state.error) return <SectionError />
  const list = state.data ?? []
  if (list.length === 0) {
    return (
      <EmptyCta
        message="아직 등록된 글이 없어요"
        href="/community/write"
        cta="글쓰기"
      />
    )
  }
  return (
    <ul className="flex flex-col gap-2">
      {list.map((post) => (
        <li key={post.id}>
          <Link
            href={`/community/${post.id}`}
            className="flex items-center gap-3 rounded-2xl border border-gray-200 bg-white p-4 hover:bg-gray-50"
          >
            <span className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-700">
              <MessageCircle className="h-5 w-5" aria-hidden="true" />
            </span>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-700">
                  {CATEGORY_LABELS[post.category] ?? post.category}
                </span>
                <span className="text-xs text-gray-500">
                  {formatDateKr(post.createdAt)}
                </span>
              </div>
              <p className="mt-0.5 truncate text-base font-semibold text-gray-900">
                {post.title}
              </p>
              <p className="text-xs text-gray-500">
                좋아요 {post.likeCount} · 댓글 {post.commentCount}
              </p>
            </div>
          </Link>
        </li>
      ))}
    </ul>
  )
}
