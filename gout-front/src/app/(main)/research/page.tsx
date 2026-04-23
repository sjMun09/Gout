'use client'

import { useCallback, useEffect, useState } from 'react'
import { ExternalLink, FileText, X } from 'lucide-react'
import { contentApi, type Paper } from '@/lib/api'

interface CategoryTab {
  key: string // 'ALL' 또는 백엔드 category 값
  label: string
}

const categoryTabs: CategoryTab[] = [
  { key: 'ALL', label: '전체' },
  { key: 'food', label: '식이' },
  { key: 'exercise', label: '운동' },
  { key: 'medication', label: '약물' },
]

function getYear(publishedAt?: string): string {
  if (!publishedAt) return ''
  const d = new Date(publishedAt)
  if (Number.isNaN(d.getTime())) {
    // "2024-05" 같은 경우 처리
    const m = publishedAt.match(/^(\d{4})/)
    return m ? m[1] : ''
  }
  return String(d.getFullYear())
}

function getPaperUrl(paper: Paper): string | null {
  if (paper.sourceUrl) return paper.sourceUrl
  if (paper.doi) return `https://doi.org/${paper.doi}`
  if (paper.pmid) return `https://pubmed.ncbi.nlm.nih.gov/${paper.pmid}/`
  return null
}

export default function ResearchPage() {
  const [activeCategory, setActiveCategory] = useState<string>('ALL')
  const [papers, setPapers] = useState<Paper[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [selectedPaper, setSelectedPaper] = useState<Paper | null>(null)

  const loadMore = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await contentApi.getPapers({
        category: activeCategory === 'ALL' ? undefined : activeCategory,
        page: page + 1,
        size: 10,
      })
      setTotalPages(data.totalPages ?? 0)
      setPage(data.number ?? page + 1)
      setPapers((prev) => [...prev, ...(data.content ?? [])])
    } catch (err) {
      setError(err instanceof Error ? err.message : '불러오기 실패')
    } finally {
      setLoading(false)
    }
  }, [activeCategory, page])

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await contentApi.getPapers({
          category: activeCategory === 'ALL' ? undefined : activeCategory,
          page: 0,
          size: 10,
        })
        if (!cancelled) {
          setTotalPages(data.totalPages ?? 0)
          setPage(data.number ?? 0)
          setPapers(data.content ?? [])
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : '불러오기 실패')
          setPapers([])
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [activeCategory])

  const hasMore = page + 1 < totalPages

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      {/* 헤더 */}
      <header className="flex items-start gap-3">
        <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-blue-50 text-blue-700">
          <FileText className="h-5 w-5" aria-hidden="true" />
        </span>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">최신 연구</h1>
          <p className="mt-1 text-base text-gray-600">
            통풍 관련 논문을 AI가 한국어로 요약했습니다
          </p>
        </div>
      </header>

      {/* 카테고리 탭 */}
      <section aria-labelledby="research-category-title">
        <h2 id="research-category-title" className="sr-only">
          카테고리
        </h2>
        <div
          className="flex gap-2 overflow-x-auto pb-1"
          role="tablist"
          aria-label="논문 카테고리"
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
        <div className="rounded-2xl border border-red-100 bg-red-50 p-4 text-sm text-red-700">
          {error}
        </div>
      )}

      {loading && papers.length === 0 ? (
        <ul className="flex flex-col gap-2">
          {Array.from({ length: 4 }).map((_, idx) => (
            <li
              key={idx}
              className="animate-pulse rounded-2xl border border-gray-200 bg-white p-4"
            >
              <div className="mb-2 h-5 w-3/4 rounded bg-gray-200" />
              <div className="mb-2 h-3 w-1/2 rounded bg-gray-100" />
              <div className="h-3 w-full rounded bg-gray-100" />
            </li>
          ))}
        </ul>
      ) : papers.length === 0 && !loading ? (
        <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-6 text-center text-gray-500">
          아직 등록된 논문이 없어요
        </div>
      ) : (
        <ul className="flex flex-col gap-2">
          {papers.map((paper) => {
            const url = getPaperUrl(paper)
            const summary = paper.aiSummaryKo || paper.abstractKo || ''
            const year = getYear(paper.publishedAt)
            return (
              <li key={paper.id}>
                <button
                  type="button"
                  onClick={() => setSelectedPaper(paper)}
                  className="flex w-full flex-col gap-2 rounded-2xl border border-gray-200 bg-white p-4 text-left transition-colors hover:bg-gray-50"
                >
                  <p className="line-clamp-2 text-base font-semibold text-gray-900">
                    {paper.title}
                  </p>
                  {(paper.journalName || year) && (
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-gray-500">
                      {paper.journalName && <span>{paper.journalName}</span>}
                      {paper.journalName && year && (
                        <span aria-hidden="true">·</span>
                      )}
                      {year && <span>{year}</span>}
                    </div>
                  )}
                  {summary && (
                    <p className="line-clamp-3 text-sm leading-relaxed text-gray-700">
                      {summary}
                    </p>
                  )}
                  {url && (
                    <a
                      href={url}
                      target="_blank"
                      rel="noopener noreferrer"
                      onClick={(e) => e.stopPropagation()}
                      className="inline-flex w-fit items-center gap-1 text-sm font-medium text-blue-600 hover:underline"
                    >
                      원문 보기
                      <ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />
                    </a>
                  )}
                </button>
              </li>
            )
          })}
        </ul>
      )}

      {hasMore && papers.length > 0 && (
        <button
          type="button"
          onClick={() => loadMore()}
          disabled={loading}
          className="flex min-h-[48px] w-full items-center justify-center rounded-2xl border border-gray-200 bg-white px-4 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50 disabled:opacity-60"
        >
          {loading ? '불러오는 중…' : '더보기'}
        </button>
      )}

      {/* 상세 모달 */}
      {selectedPaper && (
        <PaperModal
          paper={selectedPaper}
          onClose={() => setSelectedPaper(null)}
        />
      )}
    </div>
  )
}

function PaperModal({
  paper,
  onClose,
}: {
  paper: Paper
  onClose: () => void
}) {
  const url = getPaperUrl(paper)
  const year = getYear(paper.publishedAt)
  const [showSimilar, setShowSimilar] = useState(false)
  const [similar, setSimilar] = useState<Paper[]>([])
  const [similarLoading, setSimilarLoading] = useState(false)
  const [similarError, setSimilarError] = useState<string | null>(null)
  const [similarLoaded, setSimilarLoaded] = useState(false)

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    const prevOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = prevOverflow
    }
  }, [onClose])

  const toggleSimilar = async () => {
    const next = !showSimilar
    setShowSimilar(next)
    if (next && !similarLoaded) {
      setSimilarLoading(true)
      setSimilarError(null)
      try {
        const data = await contentApi.getSimilarPapers(paper.id, 5)
        setSimilar(data ?? [])
        setSimilarLoaded(true)
      } catch (err) {
        setSimilarError(
          err instanceof Error ? err.message : '유사 논문 불러오기 실패',
        )
      } finally {
        setSimilarLoading(false)
      }
    }
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="paper-modal-title"
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 sm:items-center"
      onClick={onClose}
    >
      <div
        className="flex max-h-[85vh] w-full max-w-md flex-col overflow-hidden rounded-t-3xl bg-white sm:rounded-3xl"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between gap-3 border-b border-gray-100 p-4">
          <h2
            id="paper-modal-title"
            className="flex-1 text-base font-semibold text-gray-900"
          >
            {paper.title}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-full text-gray-500 hover:bg-gray-100"
          >
            <X className="h-5 w-5" aria-hidden="true" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-4">
          <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-gray-500">
            {paper.journalName && <span>{paper.journalName}</span>}
            {paper.journalName && year && <span aria-hidden="true">·</span>}
            {year && <span>{year}</span>}
          </div>

          {paper.authors && paper.authors.length > 0 && (
            <p className="mt-2 text-xs text-gray-500">
              {paper.authors.join(', ')}
            </p>
          )}

          {paper.aiSummaryKo && (
            <section className="mt-4">
              <h3 className="mb-1.5 text-sm font-semibold text-gray-900">
                AI 요약
              </h3>
              <p className="whitespace-pre-line text-sm leading-relaxed text-gray-700">
                {paper.aiSummaryKo}
              </p>
            </section>
          )}

          {paper.abstractKo && (
            <section className="mt-4">
              <h3 className="mb-1.5 text-sm font-semibold text-gray-900">
                초록 (한국어)
              </h3>
              <p className="whitespace-pre-line text-sm leading-relaxed text-gray-700">
                {paper.abstractKo}
              </p>
            </section>
          )}

          <section className="mt-4">
            <button
              type="button"
              onClick={toggleSimilar}
              aria-expanded={showSimilar}
              className="inline-flex min-h-[40px] items-center justify-center rounded-full border border-gray-200 bg-white px-4 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-50"
            >
              {showSimilar ? '유사 논문 숨기기' : '유사 논문 보기'}
            </button>

            {showSimilar && (
              <div className="mt-3">
                {similarLoading && (
                  <p className="text-sm text-gray-500">불러오는 중…</p>
                )}
                {similarError && (
                  <p className="text-sm text-red-600">{similarError}</p>
                )}
                {!similarLoading && !similarError && similar.length === 0 && similarLoaded && (
                  <p className="text-sm text-gray-500">
                    유사 논문이 아직 없어요 (임베딩이 없거나 후보가 부족합니다)
                  </p>
                )}
                {similar.length > 0 && (
                  <ul className="flex flex-col gap-2">
                    {similar.map((s) => {
                      const sUrl = getPaperUrl(s)
                      const sYear = getYear(s.publishedAt)
                      return (
                        <li
                          key={s.id}
                          className="rounded-2xl border border-gray-200 bg-white p-3"
                        >
                          <p className="line-clamp-2 text-sm font-semibold text-gray-900">
                            {s.title}
                          </p>
                          {(s.journalName || sYear) && (
                            <div className="mt-1 flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-gray-500">
                              {s.journalName && <span>{s.journalName}</span>}
                              {s.journalName && sYear && (
                                <span aria-hidden="true">·</span>
                              )}
                              {sYear && <span>{sYear}</span>}
                            </div>
                          )}
                          {sUrl && (
                            <a
                              href={sUrl}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="mt-1.5 inline-flex items-center gap-1 text-xs font-medium text-blue-600 hover:underline"
                            >
                              원문 보기
                              <ExternalLink
                                className="h-3 w-3"
                                aria-hidden="true"
                              />
                            </a>
                          )}
                        </li>
                      )
                    })}
                  </ul>
                )}
              </div>
            )}
          </section>
        </div>

        {url && (
          <div className="border-t border-gray-100 p-4">
            <a
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl bg-blue-600 px-4 text-sm font-semibold text-white transition-colors hover:bg-blue-700"
            >
              원문 보기
              <ExternalLink className="h-4 w-4" aria-hidden="true" />
            </a>
          </div>
        )}
      </div>
    </div>
  )
}
