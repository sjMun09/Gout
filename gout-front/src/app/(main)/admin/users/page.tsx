'use client'

import { useCallback, useEffect, useState } from 'react'
import { adminApi, type AdminUser } from '@/lib/api'
import type { PagedResponse } from '@/types'

export default function AdminUsersPage() {
  const [keyword, setKeyword] = useState('')
  const [keywordInput, setKeywordInput] = useState('')
  const [users, setUsers] = useState<AdminUser[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const fetchPage = useCallback(
    async (nextPage: number, kw: string) => {
      setLoading(true)
      setError(null)
      try {
        const data: PagedResponse<AdminUser> = await adminApi.listUsers({
          page: nextPage,
          size: 20,
          keyword: kw || undefined,
        })
        setUsers(data.content ?? [])
        setTotalPages(data.totalPages ?? 0)
        setPage(data.number ?? nextPage)
      } catch (e) {
        setError(e instanceof Error ? e.message : '불러오기 실패')
        setUsers([])
      } finally {
        setLoading(false)
      }
    },
    [],
  )

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchPage(0, keyword)
  }, [fetchPage, keyword])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setKeyword(keywordInput.trim())
  }

  const runAction = async (fn: () => Promise<void>, confirmMsg?: string) => {
    if (confirmMsg && !confirm(confirmMsg)) return
    setActionError(null)
    try {
      await fn()
      await fetchPage(page, keyword)
    } catch (e) {
      setActionError(e instanceof Error ? e.message : '처리 실패')
    }
  }

  return (
    <section className="flex flex-col gap-3">
      <form onSubmit={handleSearch} className="flex gap-2">
        <input
          type="text"
          value={keywordInput}
          onChange={(e) => setKeywordInput(e.target.value)}
          placeholder="닉네임 또는 이메일"
          className="min-h-[44px] flex-1 rounded-lg border border-gray-300 px-3 text-sm"
        />
        <button
          type="submit"
          className="min-h-[44px] rounded-lg bg-blue-600 px-4 text-sm font-semibold text-white"
        >
          검색
        </button>
      </form>

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
      ) : users.length === 0 ? (
        <p className="rounded-lg border border-dashed border-gray-200 p-6 text-center text-sm text-gray-500">
          유저가 없습니다.
        </p>
      ) : (
        <ul className="flex flex-col gap-2">
          {users.map((u) => {
            const isSuspended = u.status === 'SUSPENDED'
            const isAdmin = u.role === 'ADMIN'
            return (
              <li
                key={u.id}
                className="rounded-lg border border-gray-200 bg-white p-3"
              >
                <div className="flex flex-wrap items-center gap-2">
                  <span className="font-semibold text-gray-900">
                    {u.nickname}
                  </span>
                  <span className="text-xs text-gray-500">{u.email}</span>
                  {isAdmin && (
                    <span className="rounded-full bg-purple-50 px-2 py-0.5 text-[10px] font-semibold text-purple-700">
                      ADMIN
                    </span>
                  )}
                  {isSuspended && (
                    <span className="rounded-full bg-red-50 px-2 py-0.5 text-[10px] font-semibold text-red-700">
                      SUSPENDED
                    </span>
                  )}
                </div>
                <div className="mt-2 flex flex-wrap gap-2">
                  {isSuspended ? (
                    <button
                      type="button"
                      onClick={() =>
                        runAction(
                          () => adminApi.unsuspendUser(u.id),
                          `${u.nickname} 정지 해제?`,
                        )
                      }
                      className="min-h-[36px] rounded-md border border-gray-300 px-3 text-xs font-medium"
                    >
                      정지 해제
                    </button>
                  ) : (
                    <button
                      type="button"
                      onClick={() =>
                        runAction(
                          () => adminApi.suspendUser(u.id),
                          `${u.nickname} 정지?`,
                        )
                      }
                      className="min-h-[36px] rounded-md border border-red-300 bg-red-50 px-3 text-xs font-medium text-red-700"
                    >
                      정지
                    </button>
                  )}
                  {!isAdmin && (
                    <button
                      type="button"
                      onClick={() =>
                        runAction(
                          () => adminApi.promoteUser(u.id),
                          `${u.nickname} 를 ADMIN 으로 승격?`,
                        )
                      }
                      className="min-h-[36px] rounded-md border border-purple-300 bg-purple-50 px-3 text-xs font-medium text-purple-700"
                    >
                      ADMIN 승격
                    </button>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 pt-2 text-sm">
          <button
            type="button"
            onClick={() => fetchPage(Math.max(page - 1, 0), keyword)}
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
            onClick={() => fetchPage(page + 1, keyword)}
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
