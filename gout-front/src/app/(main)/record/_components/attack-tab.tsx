'use client'

import { useMemo, useState } from 'react'
import { Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { healthApi } from '@/lib/api'
import { useConfirm } from '@/lib/use-confirm'
import { formatDateKr, todayYmd } from '@/lib/date'
import { useHealthLogs } from '../_hooks/use-health-logs'
import { ErrorBanner, Skeleton, parseError } from './shared'

export function AttackTab() {
  const { logs, setLogs, loading, error, load } = useHealthLogs(
    healthApi.getGoutAttackLogs,
  )
  const [submitting, setSubmitting] = useState(false)
  const [attackedAt, setAttackedAt] = useState(todayYmd())
  const [painLevel, setPainLevel] = useState<number>(5)
  const [location, setLocation] = useState('')
  const [durationStr, setDurationStr] = useState('')
  const [suspectedCause, setSuspectedCause] = useState('')
  const [memo, setMemo] = useState('')
  const { confirm, ConfirmDialog } = useConfirm()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!attackedAt) {
      toast.error('발작일을 입력해주세요')
      return
    }
    setSubmitting(true)
    try {
      const duration = durationStr ? parseInt(durationStr, 10) : undefined
      await healthApi.createGoutAttackLog({
        attackedAt,
        painLevel,
        location: location.trim() || undefined,
        durationDays:
          Number.isFinite(duration) && duration !== undefined && duration > 0
            ? duration
            : undefined,
        suspectedCause: suspectedCause.trim() || undefined,
        memo: memo.trim() || undefined,
      })
      setAttackedAt(todayYmd())
      setPainLevel(5)
      setLocation('')
      setDurationStr('')
      setSuspectedCause('')
      setMemo('')
      await load()
      toast.success('발작 기록이 저장되었어요')
    } catch (err) {
      toast.error(parseError(err))
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: string) => {
    const ok = await confirm({
      title: '발작 기록 삭제',
      description: '이 발작 기록을 삭제할까요? 삭제한 기록은 되돌릴 수 없어요.',
      confirmText: '삭제',
      destructive: true,
    })
    if (!ok) return
    try {
      await healthApi.deleteGoutAttackLog(id)
      setLogs((prev) => prev.filter((l) => l.id !== id))
      toast.success('기록이 삭제되었어요')
    } catch (err) {
      toast.error(parseError(err))
    }
  }

  const sortedLatest = useMemo(
    () =>
      [...logs].sort(
        (a, b) =>
          new Date(b.attackedAt).getTime() - new Date(a.attackedAt).getTime(),
      ),
    [logs],
  )

  if (loading) return <Skeleton />

  return (
    <div className="flex flex-col gap-5">
      <ConfirmDialog />
      {error && <ErrorBanner message={error} />}

      <form
        onSubmit={handleSubmit}
        className="flex flex-col gap-3 rounded-2xl border border-gray-200 bg-white p-4"
      >
        <h2 className="text-base font-semibold text-gray-900">기록 추가</h2>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">발작일</span>
          <input
            type="date"
            value={attackedAt}
            max={todayYmd()}
            onChange={(e) => setAttackedAt(e.target.value)}
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
            required
          />
        </label>

        <div className="flex flex-col gap-1">
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-700">통증 강도</span>
            <span className="text-sm font-semibold text-gray-900">
              {painLevel} / 10
            </span>
          </div>
          <input
            type="range"
            min={1}
            max={10}
            step={1}
            value={painLevel}
            onChange={(e) => setPainLevel(parseInt(e.target.value, 10))}
            className="w-full"
          />
          <div className="flex justify-between text-[10px] text-gray-500">
            <span>1 약함</span>
            <span>10 극심</span>
          </div>
        </div>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">부위</span>
          <input
            type="text"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            placeholder="예: 엄지발가락"
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">지속기간 (일)</span>
          <input
            type="number"
            inputMode="numeric"
            min="1"
            max="60"
            value={durationStr}
            onChange={(e) => setDurationStr(e.target.value)}
            placeholder="예: 3"
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">추정 원인</span>
          <input
            type="text"
            value={suspectedCause}
            onChange={(e) => setSuspectedCause(e.target.value)}
            placeholder="예: 회식 후 음주"
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">메모</span>
          <textarea
            value={memo}
            onChange={(e) => setMemo(e.target.value)}
            rows={2}
            className="rounded-xl border border-gray-300 p-3 text-base"
          />
        </label>

        <button
          type="submit"
          disabled={submitting}
          aria-busy={submitting}
          className="min-h-[48px] rounded-xl bg-blue-600 text-base font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {submitting ? '저장 중…' : '기록 추가'}
        </button>
      </form>

      <section className="flex flex-col gap-2">
        <h2 className="text-base font-semibold text-gray-900">전체 기록</h2>
        {sortedLatest.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-gray-200 bg-white p-6 text-center text-sm text-gray-500">
            아직 기록이 없어요
          </p>
        ) : (
          <ul className="flex flex-col gap-2">
            {sortedLatest.map((log) => (
              <li
                key={log.id}
                className="flex items-start justify-between gap-3 rounded-2xl border border-gray-200 bg-white p-4"
              >
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-gray-600">
                      {formatDateKr(log.attackedAt)}
                    </span>
                    {typeof log.painLevel === 'number' && (
                      <span className="rounded-full bg-red-100 px-2 py-0.5 text-xs font-medium text-red-700">
                        통증 {log.painLevel}/10
                      </span>
                    )}
                  </div>
                  <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-sm text-gray-700">
                    {log.location && <span>부위: {log.location}</span>}
                    {typeof log.durationDays === 'number' && (
                      <span>기간: {log.durationDays}일</span>
                    )}
                  </div>
                  {log.suspectedCause && (
                    <p className="mt-1 text-sm text-gray-600">
                      추정 원인: {log.suspectedCause}
                    </p>
                  )}
                  {log.memo && (
                    <p className="mt-1 text-sm text-gray-600">{log.memo}</p>
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => handleDelete(log.id)}
                  aria-label={`${formatDateKr(log.attackedAt)} 발작 기록 삭제`}
                  className="text-gray-600 hover:text-red-600"
                >
                  <Trash2 className="h-5 w-5" aria-hidden="true" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
