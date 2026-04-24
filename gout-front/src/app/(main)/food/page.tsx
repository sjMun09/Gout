'use client'

import { Search, X } from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { foodApi } from '@/lib/api'
import type { ApiPurineLevel, FoodDetail, FoodItem } from '@/types'

// 신호등 탭: 백엔드 purineLevel 파라미터와 1:1 매핑
// "주의" 탭은 MEDIUM, "피하세요" 탭은 VERY_HIGH로 단순화 (요구사항)
type TabKey = 'ALL' | 'LOW' | 'MEDIUM' | 'VERY_HIGH'

const TABS: { key: TabKey; label: string; level?: ApiPurineLevel }[] = [
  { key: 'ALL', label: '전체' },
  { key: 'LOW', label: '좋아요', level: 'LOW' },
  { key: 'MEDIUM', label: '주의', level: 'MEDIUM' },
  { key: 'VERY_HIGH', label: '피하세요', level: 'VERY_HIGH' },
]

// 퓨린 레벨 → 신호등 색상
const LEVEL_DOT: Record<ApiPurineLevel, string> = {
  LOW: 'bg-green-500',
  MEDIUM: 'bg-yellow-400',
  HIGH: 'bg-orange-500',
  VERY_HIGH: 'bg-red-500',
}

const LEVEL_TEXT: Record<ApiPurineLevel, string> = {
  LOW: '좋아요',
  MEDIUM: '주의',
  HIGH: '많이 주의',
  VERY_HIGH: '피하세요',
}

const LEVEL_BADGE: Record<ApiPurineLevel, string> = {
  LOW: 'bg-green-50 text-green-700 border-green-200',
  MEDIUM: 'bg-yellow-50 text-yellow-700 border-yellow-200',
  HIGH: 'bg-orange-50 text-orange-700 border-orange-200',
  VERY_HIGH: 'bg-red-50 text-red-700 border-red-200',
}

// 입력값 debounce 훅
function useDebounced<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debounced
}

export default function FoodPage() {
  const [keywordInput, setKeywordInput] = useState('')
  const [activeTab, setActiveTab] = useState<TabKey>('ALL')
  const [selectedId, setSelectedId] = useState<string | null>(null)

  const keyword = useDebounced(keywordInput, 300)
  const purineLevel = useMemo(
    () => TABS.find((t) => t.key === activeTab)?.level,
    [activeTab]
  )

  // useInfiniteQuery: "더 보기"로 페이지 누적. 검색 조건(keyword, purineLevel)이
  // queryKey에 들어있어 바뀌면 자동으로 첫 페이지부터 새로 가져옴.
  const {
    data,
    isLoading,
    isFetching,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: ['foods', keyword, purineLevel ?? null],
    queryFn: ({ pageParam }) =>
      foodApi.search({
        keyword: keyword || undefined,
        purineLevel,
        page: pageParam as number,
        size: 20,
      }),
    initialPageParam: 0,
    getNextPageParam: (lastPage) =>
      lastPage.number + 1 < lastPage.totalPages ? lastPage.number + 1 : undefined,
    staleTime: 60_000,
  })

  // 페이지들의 content를 하나로 flat. id 기준 중복 제거.
  const items = useMemo<FoodItem[]>(() => {
    if (!data) return []
    const seen = new Set<string>()
    const out: FoodItem[] = []
    for (const page of data.pages) {
      for (const item of page.content) {
        if (seen.has(item.id)) continue
        seen.add(item.id)
        out.push(item)
      }
    }
    return out
  }, [data])

  const showSkeleton = isLoading && items.length === 0
  const showEmpty = !isLoading && !isError && items.length === 0
  const loadingMore = isFetching && !isLoading && isFetchingNextPage

  return (
    <div className="flex flex-col gap-5 px-5 py-6 pb-24">
      <header>
        <h1 className="text-2xl font-bold text-gray-900">음식 확인</h1>
        <p className="mt-1 text-base text-gray-600">
          퓨린 수치를 신호등으로 쉽게 확인하세요
        </p>
      </header>

      {/* 검색창 */}
      <div className="relative">
        <label htmlFor="food-search" className="sr-only">
          음식 검색
        </label>
        <Search
          className="pointer-events-none absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-gray-400"
          aria-hidden="true"
        />
        <input
          id="food-search"
          type="search"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          placeholder="음식 이름을 검색하세요"
          className="h-14 w-full rounded-2xl border border-gray-200 bg-white pl-12 pr-4 text-base placeholder:text-gray-400 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
        />
      </div>

      {/* 필터 탭 */}
      <section aria-labelledby="filter-title">
        <h2 id="filter-title" className="sr-only">
          퓨린 레벨 필터
        </h2>
        <div
          className="flex gap-2 overflow-x-auto pb-1"
          role="tablist"
          aria-label="퓨린 레벨 필터"
        >
          {TABS.map((tab) => {
            const active = tab.key === activeTab
            return (
              <button
                key={tab.key}
                type="button"
                role="tab"
                aria-selected={active}
                onClick={() => setActiveTab(tab.key)}
                className={`min-h-[48px] shrink-0 rounded-full border px-5 text-base font-medium transition-colors ${
                  active
                    ? 'border-blue-600 bg-blue-600 text-white'
                    : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                {tab.label}
              </button>
            )
          })}
        </div>
      </section>

      {/* 목록 영역 */}
      <section aria-live="polite" className="flex flex-col gap-3">
        {showSkeleton && <FoodSkeletonList />}

        {isError && (
          <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-center text-red-700">
            데이터를 불러오지 못했어요. 잠시 후 다시 시도해주세요.
          </div>
        )}

        {showEmpty && (
          <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-8 text-center text-gray-500">
            검색 결과가 없어요
          </div>
        )}

        {items.map((item) => (
          <FoodCard
            key={item.id}
            item={item}
            onClick={() => setSelectedId(item.id)}
          />
        ))}

        {hasNextPage && (
          <button
            type="button"
            onClick={() => fetchNextPage()}
            disabled={loadingMore}
            aria-busy={loadingMore}
            className="mt-2 h-12 w-full rounded-2xl border border-gray-200 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
          >
            {loadingMore ? '불러오는 중...' : '더 보기'}
          </button>
        )}
      </section>

      {/* 상세 모달 */}
      {selectedId && (
        <FoodDetailModal
          id={selectedId}
          onClose={() => setSelectedId(null)}
        />
      )}
    </div>
  )
}

// --- 하위 컴포넌트 ---

function FoodCard({
  item,
  onClick,
}: {
  item: FoodItem
  onClick: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className="flex w-full items-center gap-4 rounded-2xl border border-gray-200 bg-white p-4 text-left transition-colors hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-300"
    >
      <span
        className={`inline-block h-4 w-4 shrink-0 rounded-full ${LEVEL_DOT[item.purineLevel]}`}
        aria-label={LEVEL_TEXT[item.purineLevel]}
      />
      <div className="flex min-w-0 flex-1 flex-col">
        <div className="flex items-center gap-2">
          <span className="truncate text-base font-semibold text-gray-900">
            {item.name}
          </span>
          {item.nameEn && (
            <span className="truncate text-sm text-gray-500">
              {item.nameEn}
            </span>
          )}
        </div>
        {item.category && (
          <span className="mt-0.5 text-sm text-gray-500">{item.category}</span>
        )}
      </div>
      {item.purineContent != null && (
        <div className="flex shrink-0 flex-col items-end">
          <span className="text-base font-semibold text-gray-900">
            {item.purineContent}
          </span>
          <span className="text-xs text-gray-500">mg/100g</span>
        </div>
      )}
    </button>
  )
}

function FoodSkeletonList() {
  return (
    <div className="flex flex-col gap-3" role="status" aria-label="불러오는 중">
      {[0, 1, 2].map((i) => (
        <div
          key={i}
          className="flex items-center gap-4 rounded-2xl border border-gray-200 bg-white p-4"
        >
          <div className="h-4 w-4 animate-pulse rounded-full bg-gray-200" />
          <div className="flex flex-1 flex-col gap-2">
            <div className="h-4 w-32 animate-pulse rounded bg-gray-200" />
            <div className="h-3 w-20 animate-pulse rounded bg-gray-100" />
          </div>
          <div className="h-6 w-10 animate-pulse rounded bg-gray-200" />
        </div>
      ))}
    </div>
  )
}

function FoodDetailModal({
  id,
  onClose,
}: {
  id: string
  onClose: () => void
}) {
  const { data, isLoading, isError } = useQuery<FoodDetail>({
    queryKey: ['food', id],
    queryFn: () => foodApi.getById(id),
    staleTime: 60_000,
  })

  const closeButtonRef = useRef<HTMLButtonElement>(null)
  const dialogRef = useRef<HTMLDivElement>(null)

  // 모달 열릴 때 닫기 버튼으로 포커스 이동
  useEffect(() => {
    closeButtonRef.current?.focus()
  }, [])

  // 모달 열린 동안 body 스크롤 잠금 — 모바일에서 배경 스크롤 방지
  useEffect(() => {
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [])

  // ESC로 닫기 + focus trap
  useEffect(() => {
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose()
        return
      }
      // focus trap: Tab / Shift+Tab
      if (e.key !== 'Tab') return
      const dialog = dialogRef.current
      if (!dialog) return
      const focusable = dialog.querySelectorAll<HTMLElement>(
        'a[href], button:not([disabled]), input, textarea, select, [tabindex]:not([tabindex="-1"])',
      )
      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      if (e.shiftKey) {
        if (document.activeElement === first) {
          e.preventDefault()
          last?.focus()
        }
      } else {
        if (document.activeElement === last) {
          e.preventDefault()
          first?.focus()
        }
      }
    }
    window.addEventListener('keydown', handler)
    return () => window.removeEventListener('keydown', handler)
  }, [onClose])

  return (
    <div
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="food-detail-title"
      onClick={onClose}
    >
      <div
        ref={dialogRef}
        className="max-h-[85vh] w-full max-w-md overflow-y-auto rounded-t-3xl bg-white p-6 shadow-xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="mb-4 flex items-start justify-between">
          <h2
            id="food-detail-title"
            className="text-xl font-bold text-gray-900"
          >
            {data?.name ?? '상세 정보'}
          </h2>
          <button
            ref={closeButtonRef}
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="-m-2 rounded-full p-2 text-gray-500 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-blue-300"
          >
            <X className="h-6 w-6" aria-hidden="true" />
          </button>
        </div>

        {isLoading && (
          <div className="py-8 text-center text-gray-500">불러오는 중...</div>
        )}

        {isError && (
          <div className="py-8 text-center text-red-600">
            상세 정보를 불러오지 못했어요.
          </div>
        )}

        {data && (
          <div className="flex flex-col gap-4">
            <div className="flex flex-wrap items-center gap-2">
              <span
                className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1 text-sm font-medium ${LEVEL_BADGE[data.purineLevel]}`}
              >
                <span
                  className={`inline-block h-2 w-2 rounded-full ${LEVEL_DOT[data.purineLevel]}`}
                />
                {LEVEL_TEXT[data.purineLevel]}
              </span>
              {data.category && (
                <span className="rounded-full bg-gray-100 px-3 py-1 text-sm text-gray-700">
                  {data.category}
                </span>
              )}
              {data.nameEn && (
                <span className="text-sm text-gray-500">{data.nameEn}</span>
              )}
            </div>

            {data.purineContent != null && (
              <InfoRow label="퓨린 함량">
                <span className="font-semibold text-gray-900">
                  {data.purineContent} mg
                </span>
                <span className="ml-1 text-sm text-gray-500">/ 100g</span>
              </InfoRow>
            )}

            {data.description && (
              <InfoBlock label="설명" text={data.description} />
            )}

            {data.caution && (
              <InfoBlock label="주의사항" text={data.caution} tone="warn" />
            )}

            {data.evidenceNotes && (
              <InfoBlock label="근거 노트" text={data.evidenceNotes} />
            )}
          </div>
        )}
      </div>
    </div>
  )
}

function InfoRow({
  label,
  children,
}: {
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="flex items-baseline justify-between border-b border-gray-100 pb-3">
      <span className="text-sm text-gray-500">{label}</span>
      <span className="text-base">{children}</span>
    </div>
  )
}

function InfoBlock({
  label,
  text,
  tone = 'default',
}: {
  label: string
  text: string
  tone?: 'default' | 'warn'
}) {
  const toneCls =
    tone === 'warn'
      ? 'bg-red-50 border-red-100 text-red-900'
      : 'bg-gray-50 border-gray-100 text-gray-800'
  return (
    <div className={`rounded-xl border p-3 ${toneCls}`}>
      <div className="mb-1 text-sm font-semibold">{label}</div>
      <p className="whitespace-pre-wrap text-base leading-relaxed">{text}</p>
    </div>
  )
}
