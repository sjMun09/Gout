'use client'

import { useMemo, useState } from 'react'
import { Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { healthApi } from '@/lib/api'
import type { UricAcidLog } from '@/lib/api'
import {
  URIC_ACID_POLICY,
  formatUricAcidValue,
  getUricAcidStatus,
  isValidUricAcidValue,
  roundUricAcidValue,
} from '@/lib/health-policy'
import { useConfirm } from '@/lib/use-confirm'
import { formatDateKr, todayYmd } from '@/lib/date'
import { useHealthLogs } from '../_hooks/use-health-logs'
import { useUricAcidTarget } from '../_hooks/use-uric-acid-target'
import { ErrorBanner, Skeleton, parseError } from './shared'
import { UricAcidChart } from './uric-acid-chart'

export function UricAcidTab({ accessToken }: { accessToken?: string | null }) {
  const { logs, setLogs, loading, error, load } = useHealthLogs(
    healthApi.getUricAcidLogs,
  )
  const target = useUricAcidTarget(accessToken)
  const [submitting, setSubmitting] = useState(false)
  const [measuredAt, setMeasuredAt] = useState(todayYmd())
  const [valueStr, setValueStr] = useState('')
  const [memo, setMemo] = useState('')
  const { confirm, ConfirmDialog } = useConfirm()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const value = parseFloat(valueStr)
    if (!isValidUricAcidValue(value)) {
      toast.error(
        `수치는 ${formatUricAcidValue(URIC_ACID_POLICY.minValue)} ~ ${formatUricAcidValue(
          URIC_ACID_POLICY.maxValue,
        )} 사이 값을 입력해주세요`,
      )
      return
    }
    if (!measuredAt) {
      toast.error('측정일을 입력해주세요')
      return
    }
    setSubmitting(true)
    try {
      await healthApi.createUricAcidLog({
        value: roundUricAcidValue(value),
        measuredAt,
        memo: memo.trim() || undefined,
      })
      setValueStr('')
      setMemo('')
      setMeasuredAt(todayYmd())
      await load()
      toast.success('요산수치 기록이 저장되었어요')
    } catch (err) {
      toast.error(parseError(err))
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: string) => {
    const ok = await confirm({
      title: '요산수치 기록 삭제',
      description: '이 기록을 삭제할까요? 삭제한 기록은 되돌릴 수 없어요.',
      confirmText: '삭제',
      destructive: true,
    })
    if (!ok) return
    try {
      await healthApi.deleteUricAcidLog(id)
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
          new Date(b.measuredAt).getTime() - new Date(a.measuredAt).getTime(),
      ),
    [logs],
  )

  if (loading) return <Skeleton />

  return (
    <div className="flex flex-col gap-5">
      <ConfirmDialog />
      {error && <ErrorBanner message={error} />}

      <section className="rounded-2xl border border-gray-200 bg-white p-4">
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-900">
            최근 추이 (최대 {URIC_ACID_POLICY.chartVisibleCount}개)
          </h2>
          <span className="text-xs text-gray-500">
            목표 기준 {formatUricAcidValue(target)} {URIC_ACID_POLICY.unit}
          </span>
        </div>
        <UricAcidChart logs={logs} target={target} />
      </section>

      <form
        onSubmit={handleSubmit}
        className="flex flex-col gap-3 rounded-2xl border border-gray-200 bg-white p-4"
      >
        <h2 className="text-base font-semibold text-gray-900">기록 추가</h2>
        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">측정일</span>
          <input
            type="date"
            value={measuredAt}
            max={todayYmd()}
            onChange={(e) => setMeasuredAt(e.target.value)}
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
            required
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">수치 ({URIC_ACID_POLICY.unit})</span>
          <input
            type="number"
            inputMode="decimal"
            step={URIC_ACID_POLICY.step}
            min={URIC_ACID_POLICY.minValue}
            max={URIC_ACID_POLICY.maxValue}
            value={valueStr}
            onChange={(e) => setValueStr(e.target.value)}
            placeholder="예: 6.2"
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
            required
          />
        </label>
        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">메모 (선택)</span>
          <textarea
            value={memo}
            onChange={(e) => setMemo(e.target.value)}
            rows={2}
            placeholder="공복/식후 여부 등"
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

      <UricAcidLogList logs={sortedLatest} target={target} onDelete={handleDelete} />
    </div>
  )
}

function UricAcidLogList({
  logs,
  target,
  onDelete,
}: {
  logs: UricAcidLog[]
  target: number
  onDelete: (id: string) => void
}) {
  return (
    <section className="flex flex-col gap-2">
      <h2 className="text-base font-semibold text-gray-900">전체 기록</h2>
      {logs.length === 0 ? (
        <p className="rounded-2xl border border-dashed border-gray-200 bg-white p-6 text-center text-sm text-gray-500">
          아직 기록이 없어요
        </p>
      ) : (
        <ul className="flex flex-col gap-2">
          {logs.map((log) => {
            const normal = getUricAcidStatus(log.value, target) === 'target'
            return (
              <li
                key={log.id}
                className="flex items-start justify-between gap-3 rounded-2xl border border-gray-200 bg-white p-4"
              >
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm text-gray-600">
                      {formatDateKr(log.measuredAt)}
                    </span>
                    <span
                      className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                        normal
                          ? 'bg-blue-100 text-blue-700'
                          : 'bg-red-100 text-red-700'
                      }`}
                    >
                      {normal ? '목표 도달' : '관리 필요'}
                    </span>
                  </div>
                  <p className="mt-1 text-lg font-bold text-gray-900">
                    {formatUricAcidValue(log.value)}{' '}
                    <span className="text-sm font-normal text-gray-600">
                      {URIC_ACID_POLICY.unit}
                    </span>
                  </p>
                  {log.memo && (
                    <p className="mt-1 text-sm text-gray-600">{log.memo}</p>
                  )}
                </div>
                <button
                  type="button"
                  onClick={() => onDelete(log.id)}
                  aria-label={`${formatDateKr(log.measuredAt)} 요산수치 기록 삭제`}
                  className="text-gray-600 hover:text-red-600"
                >
                  <Trash2 className="h-5 w-5" aria-hidden="true" />
                </button>
              </li>
            )
          })}
        </ul>
      )}
    </section>
  )
}
