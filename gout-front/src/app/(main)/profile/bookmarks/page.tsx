'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useCallback, useEffect, useState } from 'react'
import { Bookmark, ChevronLeft, Eye, Heart, MessageCircle } from 'lucide-react'
import {
  bookmarkApi,
  CATEGORY_LABELS,
  type PostSummary,
} from '@/lib/api'
import { useAuth } from '@/lib/auth'
import type { PagedResponse } from '@/types'

function formatDate(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const yyyy = date.getFullYear()
  const MM = String(date.getMonth() + 1).padStart(2, '0')
  const DD = String(date.getDate()).padStart(2, '0')
  return `${yyyy}.${MM}.${DD}`
}

export default function ProfileBookmarksPage() {
  const router = useRouter()
  const { isAuthenticated, isHydrated } = useAuth()
  const [items, setItems] = useState<PostSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  const load = useCallback(
    async (targetPage: number) => {
      setLoading(true)
      setError(null)
      try {
        const data: PagedResponse<PostSummary> = await bookmarkApi.list(
          targetPage,
          20,
        )
        // 백엔드에서 삭제된 게시글은 placeholder null 이 섞일 수 있어 필터링
        const safe = (data.content ?? []).filter(
          (p): p is PostSummary => p != null && !!p.id,
        )
        setItems(safe)
        setTotalPages(data.totalPages ?? 0)
        setPage(data.number ?? 0)
      } catch (err) {
        const msg =
          err instanceof Error ? err.message : '북마크를 불러오지 못했어요'
        // 401 → 로그인 페이지 유도
        if (msg.includes('401') || msg.includes('403')) {
          router.push('/login')
          return
        }
        setError(msg)
      } finally {
        setLoading(false)
      }
    },
    [router],
  )

  useEffect(() => {
    if (!isHydrated) return
    if (!isAuthenticated) {
      router.push('/login')
      return
    }
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load(0)
  }, [isAuthenticated, isHydrated, load, router])

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <div>
        <Link
          href="/profile"
          className="inline-flex h-10 items-center gap-1 rounded-full px-2 text-sm font-medium text-gray-700 transition-colors hover:bg-gray-100"
        >
          <ChevronLeft className="h-5 w-5" aria-hidden="true" />내 정보
        </Link>
      </div>

      <header>
        <h1 className="flex items-center gap-2 text-2xl font-bold text-gray-900">
          <Bookmark className="h-6 w-6" aria-hidden="true" />내 스크랩
        </h1>
        <p className="mt-1 text-sm text-gray-600">
          북마크한 게시글을 최신순으로 모아봤어요
        </p>
      </header>

      {loading ? (
        <div role="status" aria-label="불러오는 중" className="flex flex-col gap-2">
          {[0, 1, 2].map((i) => (
            <div
              key={i}
              className="h-24 animate-pulse rounded-2xl bg-gray-100"
              aria-hidden="true"
            />
          ))}
        </div>
      ) : error ? (
        <div
          role="alert"
          className="rounded-2xl border border-red-100 bg-red-50 p-4 text-sm text-red-700"
        >
          {error}
        </div>
      ) : items.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-gray-200 bg-white p-8 text-center text-sm text-gray-500">
          아직 스크랩한 게시글이 없어요
        </div>
      ) : (
        <ul className="flex flex-col gap-2">
          {items.map((p) => (
            <li key={p.id}>
              <Link
                href={`/community/${p.id}`}
                className="flex flex-col gap-2 rounded-2xl border border-gray-200 bg-white p-4 transition-colors hover:bg-gray-50"
              >
                <span className="inline-flex w-fit items-center rounded-full bg-blue-50 px-2.5 py-0.5 text-xs font-semibold text-blue-700">
                  {CATEGORY_LABELS[p.category] ?? p.category}
                </span>
                <h2 className="text-base font-semibold text-gray-900 line-clamp-2">
                  {p.title}
                </h2>
                <div className="flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-gray-500">
                  <span>{p.nickname}</span>
                  <span aria-hidden="true">·</span>
                  <span>{formatDate(p.createdAt)}</span>
                  <span className="inline-flex items-center gap-1">
                    <Eye className="h-3.5 w-3.5" aria-hidden="true" />
                    {p.viewCount ?? 0}
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <Heart className="h-3.5 w-3.5" aria-hidden="true" />
                    {p.likeCount ?? 0}
                  </span>
                  <span className="inline-flex items-center gap-1">
                    <MessageCircle className="h-3.5 w-3.5" aria-hidden="true" />
                    {p.commentCount ?? 0}
                  </span>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}

      {totalPages > 1 && (
        <nav aria-label="페이지 탐색" className="flex items-center justify-center gap-2">
          <button
            type="button"
            onClick={() => load(Math.max(0, page - 1))}
            disabled={page === 0 || loading}
            aria-label={`이전 페이지 (${page}페이지)`}
            className="min-h-[44px] rounded-full border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 disabled:opacity-50"
          >
            이전
          </button>
          <span aria-live="polite" aria-atomic="true" className="text-sm text-gray-600">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            onClick={() => load(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1 || loading}
            aria-label={`다음 페이지 (${page + 2}페이지)`}
            className="min-h-[44px] rounded-full border border-gray-300 bg-white px-4 text-sm font-medium text-gray-700 disabled:opacity-50"
          >
            다음
          </button>
        </nav>
      )}
    </div>
  )
}
