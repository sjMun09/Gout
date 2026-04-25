'use client'

import { useCallback, useEffect, useState } from 'react'
import { adminApi, type AdminReport } from '@/lib/api'

export default function AdminReportsPage() {
  const [reports, setReports] = useState<AdminReport[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const fetchReports = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await adminApi.listReports({ status: 'PENDING', size: 50 })
      setReports(data.content ?? [])
    } catch (e) {
      setError(e instanceof Error ? e.message : '불러오기 실패')
      setReports([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchReports()
  }, [fetchReports])

  const runAction = async (fn: () => Promise<void>) => {
    setActionError(null)
    try {
      await fn()
      await fetchReports()
    } catch (e) {
      setActionError(e instanceof Error ? e.message : '처리 실패')
    }
  }

  return (
    <section className="flex flex-col gap-3">
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
      ) : reports.length === 0 ? (
        <p className="rounded-lg border border-dashed border-gray-200 p-6 text-center text-sm text-gray-500">
          대기 중 신고가 없습니다.
        </p>
      ) : (
        <ul className="flex flex-col gap-2">
          {reports.map((r) => (
            <li
              key={r.id}
              className="rounded-lg border border-gray-200 bg-white p-3"
            >
              <div className="flex items-center gap-2 text-xs text-gray-500">
                <span className="rounded-full bg-red-50 px-2 py-0.5 font-semibold text-red-700">
                  {r.targetType}
                </span>
                <span>{r.targetId}</span>
              </div>
              <p className="mt-1 text-sm text-gray-900">{r.reason}</p>
              {r.reporterNickname && (
                <p className="mt-1 text-xs text-gray-500">
                  신고자: {r.reporterNickname}
                </p>
              )}
              <div className="mt-2 flex gap-2">
                <button
                  type="button"
                  onClick={() => runAction(() => adminApi.resolveReport(r.id))}
                  className="min-h-[36px] rounded-md border border-green-300 bg-green-50 px-3 text-xs font-medium text-green-700"
                >
                  처리 완료
                </button>
                <button
                  type="button"
                  onClick={() => runAction(() => adminApi.dismissReport(r.id))}
                  className="min-h-[36px] rounded-md border border-gray-300 px-3 text-xs font-medium"
                >
                  반려
                </button>
              </div>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
