'use client'

import { useMemo, useState } from 'react'
import { Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { healthApi } from '@/lib/api'
import { useConfirm } from '@/lib/use-confirm'
import { formatDateTimeKr, nowLocalDatetime } from '@/lib/date'
import { useHealthLogs } from '../_hooks/use-health-logs'
import { ErrorBanner, Skeleton, parseError } from './shared'

export function MedicationTab() {
  const { logs, setLogs, loading, error, load } = useHealthLogs(
    healthApi.getMedicationLogs,
  )
  const [submitting, setSubmitting] = useState(false)
  const [medicationName, setMedicationName] = useState('')
  const [dosage, setDosage] = useState('')
  const [takenAt, setTakenAt] = useState(nowLocalDatetime())
  const { confirm, ConfirmDialog } = useConfirm()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!medicationName.trim()) {
      toast.error('약품명을 입력해주세요')
      return
    }
    if (!takenAt) {
      toast.error('복약 시간을 입력해주세요')
      return
    }
    setSubmitting(true)
    try {
      const iso = takenAt.length === 16 ? `${takenAt}:00` : takenAt
      await healthApi.createMedicationLog({
        medicationName: medicationName.trim(),
        dosage: dosage.trim() || undefined,
        takenAt: iso,
      })
      setMedicationName('')
      setDosage('')
      setTakenAt(nowLocalDatetime())
      await load()
      toast.success('복약 기록이 저장되었어요')
    } catch (err) {
      toast.error(parseError(err))
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: string) => {
    const ok = await confirm({
      title: '복약 기록 삭제',
      description: '이 복약 기록을 삭제할까요? 삭제한 기록은 되돌릴 수 없어요.',
      confirmText: '삭제',
      destructive: true,
    })
    if (!ok) return
    try {
      await healthApi.deleteMedicationLog(id)
      setLogs((prev) => prev.filter((l) => l.id !== id))
      toast.success('기록이 삭제되었어요')
    } catch (err) {
      toast.error(parseError(err))
    }
  }

  const sortedLatest = useMemo(
    () =>
      [...logs].sort(
        (a, b) => new Date(b.takenAt).getTime() - new Date(a.takenAt).getTime(),
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
          <span className="text-sm text-gray-700">
            약품명 <span className="text-red-500">*</span>
          </span>
          <input
            type="text"
            value={medicationName}
            onChange={(e) => setMedicationName(e.target.value)}
            placeholder="예: 알로퓨리놀"
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
            required
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">용량/용법</span>
          <input
            type="text"
            value={dosage}
            onChange={(e) => setDosage(e.target.value)}
            placeholder="예: 100mg 1정"
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">복약 시간</span>
          <input
            type="datetime-local"
            value={takenAt}
            onChange={(e) => setTakenAt(e.target.value)}
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
            required
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
                  <p className="text-base font-semibold text-gray-900">
                    {log.medicationName}
                  </p>
                  {log.dosage && (
                    <p className="mt-0.5 text-sm text-gray-700">{log.dosage}</p>
                  )}
                  <p className="mt-1 text-xs text-gray-500">
                    {formatDateTimeKr(log.takenAt)}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => handleDelete(log.id)}
                  aria-label={`${log.medicationName} 복약 기록 삭제`}
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
