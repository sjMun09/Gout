'use client'

import { useEffect, useState } from 'react'
import {
  BookOpen,
  Check,
  ChevronDown,
  ChevronUp,
  ExternalLink,
  X,
} from 'lucide-react'
import {
  contentApi,
  type EvidenceStrength,
  type Guideline,
  type GuidelineCategory,
} from '@/lib/api'

interface CategoryTab {
  key: 'ALL' | GuidelineCategory
  label: string
}

const categoryTabs: CategoryTab[] = [
  { key: 'ALL', label: '전체' },
  { key: 'FOOD', label: '식이' },
  { key: 'EXERCISE', label: '운동' },
  { key: 'MEDICATION', label: '약물' },
  { key: 'LIFESTYLE', label: '생활습관' },
]

const evidenceBadgeClass: Record<EvidenceStrength, string> = {
  STRONG: 'bg-blue-700 text-white',
  MODERATE: 'bg-blue-100 text-blue-700',
  WEAK: 'bg-gray-100 text-gray-600',
}

const evidenceBadgeLabel: Record<EvidenceStrength, string> = {
  STRONG: '근거 강함',
  MODERATE: '근거 보통',
  WEAK: '근거 약함',
}

export default function EncyclopediaPage() {
  const [activeCategory, setActiveCategory] =
    useState<CategoryTab['key']>('ALL')
  const [guidelines, setGuidelines] = useState<Guideline[]>([])
  const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set())
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [retryCount, setRetryCount] = useState(0)

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await contentApi.getGuidelines(
          activeCategory === 'ALL' ? undefined : { category: activeCategory },
        )
        if (!cancelled) setGuidelines(data ?? [])
      } catch {
        if (!cancelled) {
          setError('가이드라인을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
          setGuidelines([])
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [activeCategory, retryCount])

  const toggleExpanded = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const doItems = guidelines.filter((g) => g.type === 'DO')
  const dontItems = guidelines.filter((g) => g.type === 'DONT')

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      {/* 헤더 */}
      <header className="flex items-start gap-3">
        <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-700">
          <BookOpen className="h-5 w-5" aria-hidden="true" />
        </span>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">통풍 백과</h1>
          <p className="mt-1 text-base text-gray-600">
            원인부터 관리법까지 한눈에
          </p>
        </div>
      </header>

      {/* 카테고리 탭 */}
      <section aria-labelledby="encyclopedia-category-title">
        <h2 id="encyclopedia-category-title" className="sr-only">
          카테고리
        </h2>
        <div
          className="flex gap-2 overflow-x-auto pb-1"
          role="tablist"
          aria-label="가이드라인 카테고리"
        >
          {categoryTabs.map((cat) => {
            const selected = activeCategory === cat.key
            return (
              <button
                key={cat.key}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => setActiveCategory(cat.key)}
                className={`min-h-[44px] shrink-0 rounded-full border px-4 text-sm font-medium transition-colors ${
                  selected
                    ? 'border-blue-600 bg-blue-600 text-white'
                    : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                {cat.label}
              </button>
            )
          })}
        </div>
      </section>

      {error && (
        <div role="alert" className="flex flex-col gap-2 rounded-2xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">
          <p>{error}</p>
          <button
            type="button"
            onClick={() => setRetryCount((c) => c + 1)}
            className="self-start rounded-lg border border-red-300 px-3 py-1.5 text-sm font-medium hover:bg-red-100"
          >
            다시 시도
          </button>
        </div>
      )}

      {loading && guidelines.length === 0 ? (
        <ul className="flex flex-col gap-2">
          {Array.from({ length: 4 }).map((_, idx) => (
            <li
              key={idx}
              className="animate-pulse rounded-2xl border border-gray-200 bg-white p-4"
            >
              <div className="mb-2 h-5 w-3/4 rounded bg-gray-200" />
              <div className="h-3 w-1/2 rounded bg-gray-100" />
            </li>
          ))}
        </ul>
      ) : guidelines.length === 0 && !loading ? (
        <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-6 text-center text-gray-500">
          해당 카테고리에 가이드라인이 없어요
        </div>
      ) : (
        <div className="flex flex-col gap-6">
          {doItems.length > 0 && (
            <GuidelineGroup
              title="이것은 하세요"
              type="DO"
              items={doItems}
              expandedIds={expandedIds}
              onToggle={toggleExpanded}
            />
          )}
          {dontItems.length > 0 && (
            <GuidelineGroup
              title="이것은 피하세요"
              type="DONT"
              items={dontItems}
              expandedIds={expandedIds}
              onToggle={toggleExpanded}
            />
          )}
        </div>
      )}
    </div>
  )
}

function GuidelineGroup({
  title,
  type,
  items,
  expandedIds,
  onToggle,
}: {
  title: string
  type: 'DO' | 'DONT'
  items: Guideline[]
  expandedIds: Set<string>
  onToggle: (id: string) => void
}) {
  const isDo = type === 'DO'
  return (
    <section aria-label={title}>
      <h2 className="mb-2 flex items-center gap-2 text-lg font-semibold text-gray-900">
        <span
          className={`inline-flex h-6 w-6 items-center justify-center rounded-full ${
            isDo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
          }`}
          aria-hidden="true"
        >
          {isDo ? (
            <Check className="h-4 w-4" />
          ) : (
            <X className="h-4 w-4" />
          )}
        </span>
        {title}
      </h2>
      <ul className="flex flex-col gap-2">
        {items.map((g) => (
          <GuidelineCard
            key={g.id}
            guideline={g}
            expanded={expandedIds.has(g.id)}
            onToggle={() => onToggle(g.id)}
          />
        ))}
      </ul>
    </section>
  )
}

function GuidelineCard({
  guideline,
  expanded,
  onToggle,
}: {
  guideline: Guideline
  expanded: boolean
  onToggle: () => void
}) {
  const isDo = guideline.type === 'DO'
  return (
    <li className="rounded-2xl border border-gray-200 bg-white p-4">
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={expanded}
        className="flex w-full items-start gap-3 text-left"
      >
        <span
          className={`mt-0.5 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full ${
            isDo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
          }`}
          aria-hidden="true"
        >
          {isDo ? (
            <Check className="h-4 w-4" />
          ) : (
            <X className="h-4 w-4" />
          )}
        </span>
        <div className="flex-1">
          <div className="flex items-start justify-between gap-2">
            <p className="text-base font-semibold text-gray-900">
              {guideline.title}
            </p>
            {expanded ? (
              <ChevronUp
                className="mt-0.5 h-5 w-5 shrink-0 text-gray-400"
                aria-hidden="true"
              />
            ) : (
              <ChevronDown
                className="mt-0.5 h-5 w-5 shrink-0 text-gray-400"
                aria-hidden="true"
              />
            )}
          </div>
          <div className="mt-1.5 flex flex-wrap items-center gap-1.5">
            <span
              className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${
                evidenceBadgeClass[guideline.evidenceStrength]
              }`}
            >
              {evidenceBadgeLabel[guideline.evidenceStrength]}
            </span>
          </div>
        </div>
      </button>
      {expanded && (
        <div className="mt-3 border-t border-gray-100 pt-3">
          <p className="whitespace-pre-line text-sm leading-relaxed text-gray-700">
            {guideline.content}
          </p>
          {(guideline.evidenceSource || guideline.evidenceDoi) && (
            <div className="mt-3 rounded-xl bg-gray-50 p-3 text-xs text-gray-600">
              <p className="font-semibold text-gray-700">근거 출처</p>
              {guideline.evidenceSource && (
                <p className="mt-1">{guideline.evidenceSource}</p>
              )}
              {guideline.evidenceDoi && (
                <a
                  href={`https://doi.org/${guideline.evidenceDoi}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="mt-1 inline-flex items-center gap-1 text-blue-600 hover:underline"
                >
                  DOI: {guideline.evidenceDoi}
                  <ExternalLink className="h-3 w-3" aria-hidden="true" />
                </a>
              )}
            </div>
          )}
        </div>
      )}
    </li>
  )
}
