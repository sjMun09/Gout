'use client'

import { useEffect, useMemo, useState } from 'react'
import {
  Check,
  ChevronDown,
  ChevronUp,
  Dumbbell,
  ExternalLink,
  X,
} from 'lucide-react'
import {
  contentApi,
  type EvidenceStrength,
  type Guideline,
  type GuidelineType,
} from '@/lib/api'

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

export default function ExercisePage() {
  const [activeTab, setActiveTab] = useState<GuidelineType>('DO')
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
        const data = await contentApi.getGuidelines({ category: 'EXERCISE' })
        if (!cancelled) setGuidelines(data ?? [])
      } catch {
        if (!cancelled) {
          setError('운동 가이드를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
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
  }, [retryCount])

  const filtered = useMemo(
    () => guidelines.filter((g) => g.type === activeTab),
    [guidelines, activeTab],
  )

  const toggleExpanded = (id: string) => {
    setExpandedIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      {/* 헤더 */}
      <header className="flex items-start gap-3">
        <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-700">
          <Dumbbell className="h-5 w-5" aria-hidden="true" />
        </span>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">운동 가이드</h1>
          <p className="mt-1 text-base text-gray-600">
            통풍 환자에게 맞는 운동 지침입니다
          </p>
        </div>
      </header>

      {/* DO / DONT 탭 */}
      <div
        className="grid grid-cols-2 gap-2"
        role="tablist"
        aria-label="운동 가이드 탭"
      >
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'DO'}
          onClick={() => setActiveTab('DO')}
          className={`flex min-h-[48px] items-center justify-center gap-2 rounded-2xl border text-sm font-semibold transition-colors ${
            activeTab === 'DO'
              ? 'border-green-600 bg-green-600 text-white'
              : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
          }`}
        >
          <Check className="h-4 w-4" aria-hidden="true" />
          권장 운동
        </button>
        <button
          type="button"
          role="tab"
          aria-selected={activeTab === 'DONT'}
          onClick={() => setActiveTab('DONT')}
          className={`flex min-h-[48px] items-center justify-center gap-2 rounded-2xl border text-sm font-semibold transition-colors ${
            activeTab === 'DONT'
              ? 'border-red-600 bg-red-600 text-white'
              : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
          }`}
        >
          <X className="h-4 w-4" aria-hidden="true" />
          피해야 할 운동
        </button>
      </div>

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
      ) : filtered.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-6 text-center text-gray-500">
          {activeTab === 'DO'
            ? '등록된 권장 운동이 없어요'
            : '등록된 주의 운동이 없어요'}
        </div>
      ) : (
        <ul className="flex flex-col gap-2">
          {filtered.map((g) => {
            const expanded = expandedIds.has(g.id)
            const isDo = g.type === 'DO'
            return (
              <li
                key={g.id}
                className="rounded-2xl border border-gray-200 bg-white p-4"
              >
                <button
                  type="button"
                  onClick={() => toggleExpanded(g.id)}
                  aria-expanded={expanded}
                  className="flex w-full items-start gap-3 text-left"
                >
                  <span
                    className={`mt-0.5 inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full ${
                      isDo
                        ? 'bg-green-100 text-green-700'
                        : 'bg-red-100 text-red-700'
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
                        {g.title}
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
                    <span
                      className={`mt-1.5 inline-flex items-center rounded-full px-2 py-0.5 text-xs font-semibold ${
                        evidenceBadgeClass[g.evidenceStrength]
                      }`}
                    >
                      {evidenceBadgeLabel[g.evidenceStrength]}
                    </span>
                  </div>
                </button>
                {expanded && (
                  <div className="mt-3 border-t border-gray-100 pt-3">
                    <p className="whitespace-pre-line text-sm leading-relaxed text-gray-700">
                      {g.content}
                    </p>
                    {(g.evidenceSource || g.evidenceDoi) && (
                      <div className="mt-3 rounded-xl bg-gray-50 p-3 text-xs text-gray-600">
                        <p className="font-semibold text-gray-700">
                          근거 출처
                        </p>
                        {g.evidenceSource && (
                          <p className="mt-1">{g.evidenceSource}</p>
                        )}
                        {g.evidenceDoi && (
                          <a
                            href={`https://doi.org/${g.evidenceDoi}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="mt-1 inline-flex items-center gap-1 text-blue-600 hover:underline"
                          >
                            DOI: {g.evidenceDoi}
                            <ExternalLink
                              className="h-3 w-3"
                              aria-hidden="true"
                            />
                          </a>
                        )}
                      </div>
                    )}
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
