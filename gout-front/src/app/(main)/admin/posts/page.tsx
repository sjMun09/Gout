'use client'

import { useCallback, useEffect, useState } from 'react'
import {
  adminApi,
  communityApi,
  CATEGORY_LABELS,
  type PostSummary,
} from '@/lib/api'
import { useConfirm } from '@/lib/use-confirm'

export default function AdminPostsPage() {
  const [posts, setPosts] = useState<PostSummary[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const { confirm, ConfirmDialog } = useConfirm()

  const fetchPage = useCallback(async (nextPage: number) => {
    setLoading(true)
    setError(null)
    try {
      const data = await communityApi.getPosts({ page: nextPage, size: 20 })
      setPosts(data.content ?? [])
      setTotalPages(data.totalPages ?? 0)
      setPage(data.number ?? nextPage)
    } catch (e) {
      setError(e instanceof Error ? e.message : '불러오기 실패')
      setPosts([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchPage(0)
  }, [fetchPage])

  const runAction = async (fn: () => Promise<void>, confirmMsg: string) => {
    const ok = await confirm({
      title: '관리자 작업 확인',
      description: confirmMsg,
      confirmText: '실행',
      destructive: true,
    })
    if (!ok) return
    setActionError(null)
    try {
      await fn()
      await fetchPage(page)
    } catch (e) {
      setActionError(e instanceof Error ? e.message : '처리 실패')
    }
  }

  return (
    <section className="flex flex-col gap-3">
      <ConfirmDialog />
      {error && (
        <p className="rounded-lg border border-red-100 bg-red-50 p-3 text-sm text-red-700">
          {error}
        </p>
      )}
      {actionError && (
        <p className="rounded-lg border border-amber-100 bg-amber-50 p-3 text-sm text-amber-800">
          {actionError}
        </p>
      )}

      {loading ? (
        <p className="py-6 text-center text-sm text-gray-500">불러오는 중…</p>
      ) : posts.length === 0 ? (
        <p className="rounded-lg border border-dashed border-gray-200 p-6 text-center text-sm text-gray-500">
          게시글이 없습니다.
        </p>
      ) : (
        <ul className="flex flex-col gap-2">
          {posts.map((p) => (
            <li
              key={p.id}
              className="rounded-lg border border-gray-200 bg-white p-3"
            >
              <div className="flex items-center gap-2">
                <span className="rounded-full bg-blue-50 px-2 py-0.5 text-[10px] font-semibold text-blue-700">
                  {CATEGORY_LABELS[p.category] ?? p.category}
                </span>
                <span className="text-xs text-gray-500">{p.nickname}</span>
              </div>
              <p className="mt-1 text-sm font-semibold text-gray-900">
                {p.title}
              </p>
              <div className="mt-2 flex gap-2">
                <button
                  type="button"
                  onClick={() =>
                    runAction(
                      () => adminApi.hidePost(p.id),
                      '이 게시글을 숨김 처리할까요?',
                    )
                  }
                  className="min-h-[36px] rounded-md border border-gray-300 px-3 text-xs font-medium"
                >
                  숨김
                </button>
                <button
                  type="button"
                  onClick={() =>
                    runAction(
                      () => adminApi.deletePost(p.id),
                      '이 게시글을 삭제 처리할까요?',
                    )
                  }
                  className="min-h-[36px] rounded-md border border-red-300 bg-red-50 px-3 text-xs font-medium text-red-700"
                >
                  삭제
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 pt-2 text-sm">
          <button
            type="button"
            onClick={() => fetchPage(Math.max(page - 1, 0))}
            disabled={page <= 0 || loading}
            className="min-h-[36px] rounded-md border border-gray-300 px-3 disabled:opacity-50"
          >
            이전
          </button>
          <span className="text-gray-600">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            onClick={() => fetchPage(page + 1)}
            disabled={page + 1 >= totalPages || loading}
            className="min-h-[36px] rounded-md border border-gray-300 px-3 disabled:opacity-50"
          >
            다음
          </button>
        </div>
      )}
    </section>
  )
}
