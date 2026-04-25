'use client'

import { useEffect, useState } from 'react'
import {
  AlertTriangle,
  ChevronDown,
  ChevronUp,
  Lightbulb,
  Search,
  Users,
  Zap,
} from 'lucide-react'
import { contentApi, type AgeGroup, type AgeGroupContent } from '@/lib/api'

interface AgeOption {
  key: AgeGroup
  label: string
}

const ageOptions: AgeOption[] = [
  { key: 'TWENTIES', label: '20대' },
  { key: 'THIRTIES', label: '30대' },
  { key: 'FORTIES', label: '40대' },
  { key: 'FIFTIES', label: '50대' },
  { key: 'SIXTIES', label: '60대' },
  { key: 'SEVENTIES_PLUS', label: '70대+' },
]

type SectionKey = 'characteristics' | 'mainCauses' | 'warnings' | 'managementTips'

interface SectionDef {
  key: SectionKey
  label: string
  icon: typeof Search
  iconClass: string
}

const sectionDefs: SectionDef[] = [
  {
    key: 'characteristics',
    label: '이 연령대 통풍 특징',
    icon: Search,
    iconClass: 'bg-blue-50 text-blue-700',
  },
  {
    key: 'mainCauses',
    label: '주요 원인',
    icon: Zap,
    iconClass: 'bg-amber-50 text-amber-700',
  },
  {
    key: 'warnings',
    label: '주의사항',
    icon: AlertTriangle,
    iconClass: 'bg-red-50 text-red-700',
  },
  {
    key: 'managementTips',
    label: '관리 방법',
    icon: Lightbulb,
    iconClass: 'bg-green-50 text-green-700',
  },
]

export default function AgeInfoPage() {
  const [selectedAge, setSelectedAge] = useState<AgeGroup>('THIRTIES')
  const [content, setContent] = useState<AgeGroupContent | null>(null)
  const [expandedSections, setExpandedSections] = useState<Set<SectionKey>>(
    new Set(['characteristics']),
  )
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [retryCount, setRetryCount] = useState(0)

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await contentApi.getAgeContent(selectedAge)
        if (!cancelled) setContent(data ?? null)
      } catch {
        if (!cancelled) {
          setError('연령별 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.')
          setContent(null)
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [selectedAge, retryCount])

  const toggleSection = (key: SectionKey) => {
    setExpandedSections((prev) => {
      const next = new Set(prev)
      if (next.has(key)) next.delete(key)
      else next.add(key)
      return next
    })
  }

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      {/* 헤더 */}
      <header className="flex items-start gap-3">
        <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-700">
          <Users className="h-5 w-5" aria-hidden="true" />
        </span>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">연령별 정보</h1>
          <p className="mt-1 text-base text-gray-600">
            내 연령대에 맞는 통풍 관리 팁
          </p>
        </div>
      </header>

      {/* 연령대 선택 버튼 (2x3 그리드) */}
      <section aria-labelledby="age-select-title">
        <h2 id="age-select-title" className="sr-only">
          연령대 선택
        </h2>
        <div className="grid grid-cols-3 gap-2">
          {ageOptions.map((opt) => {
            const selected = selectedAge === opt.key
            return (
              <button
                key={opt.key}
                type="button"
                onClick={() => setSelectedAge(opt.key)}
                aria-pressed={selected}
                className={`min-h-[48px] rounded-2xl border px-3 text-sm font-semibold transition-colors ${
                  selected
                    ? 'border-blue-600 bg-blue-600 text-white'
                    : 'border-gray-200 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                {opt.label}
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

      {loading && !content ? (
        <div className="flex flex-col gap-2">
          {Array.from({ length: 4 }).map((_, idx) => (
            <div
              key={idx}
              className="animate-pulse rounded-2xl border border-gray-200 bg-white p-4"
            >
              <div className="mb-2 h-5 w-1/2 rounded bg-gray-200" />
              <div className="h-3 w-full rounded bg-gray-100" />
            </div>
          ))}
        </div>
      ) : !content && !loading ? (
        <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-6 text-center text-gray-500">
          해당 연령대 정보가 아직 없어요
        </div>
      ) : content ? (
        <div className="flex flex-col gap-3">
          {content.title && (
            <div className="rounded-2xl border border-blue-100 bg-blue-50 p-4">
              <p className="text-base font-semibold text-blue-900">
                {content.title}
              </p>
            </div>
          )}
          {sectionDefs.map((section) => {
            const Icon = section.icon
            const expanded = expandedSections.has(section.key)
            const body = content[section.key]
            if (!body) return null
            return (
              <div
                key={section.key}
                className="rounded-2xl border border-gray-200 bg-white"
              >
                <button
                  type="button"
                  onClick={() => toggleSection(section.key)}
                  aria-expanded={expanded}
                  className="flex w-full items-center gap-3 p-4 text-left"
                >
                  <span
                    className={`inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full ${section.iconClass}`}
                    aria-hidden="true"
                  >
                    <Icon className="h-[18px] w-[18px]" />
                  </span>
                  <p className="flex-1 text-base font-semibold text-gray-900">
                    {section.label}
                  </p>
                  {expanded ? (
                    <ChevronUp
                      className="h-5 w-5 text-gray-500"
                      aria-hidden="true"
                    />
                  ) : (
                    <ChevronDown
                      className="h-5 w-5 text-gray-500"
                      aria-hidden="true"
                    />
                  )}
                </button>
                {expanded && (
                  <div className="border-t border-gray-100 px-4 pb-4 pt-3">
                    <p className="whitespace-pre-line text-sm leading-relaxed text-gray-700">
                      {body}
                    </p>
                  </div>
                )}
              </div>
            )
          })}
          {content.evidenceSource && (
            <div className="rounded-2xl bg-gray-50 p-3 text-xs text-gray-600">
              <p className="font-semibold text-gray-700">근거 출처</p>
              <p className="mt-1 whitespace-pre-line">
                {content.evidenceSource}
              </p>
            </div>
          )}
        </div>
      ) : null}
    </div>
  )
}
