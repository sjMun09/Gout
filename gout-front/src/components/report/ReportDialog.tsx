'use client'

import { FormEvent, useEffect, useState } from 'react'
import { toast } from 'sonner'
import {
  ApiError,
  REPORT_REASON_LABELS,
  type ReportReason,
  type ReportTargetType,
  reportApi,
} from '@/lib/api'

type ToastKind = 'success' | 'error'

interface ReportDialogProps {
  open: boolean
  targetType: ReportTargetType
  targetId: string
  onClose: () => void
  /** 결과 토스트 표시. 상위에서 토스트 스택을 관리하지 않는 경우 alert 폴백. */
  onToast?: (kind: ToastKind, message: string) => void
}

const REASONS: ReportReason[] = ['SPAM', 'ABUSE', 'SEXUAL', 'MISINFO', 'ETC']

/**
 * 게시글/댓글 신고 다이얼로그.
 * - 라디오로 사유 선택 + 선택 detail 텍스트.
 * - 409 (중복 신고) 은 "이미 신고한 게시물입니다." 로 메시지 교체.
 * - 상위에서 onToast 를 내려주면 토스트, 아니면 alert 폴백.
 */
export default function ReportDialog({
  open,
  targetType,
  targetId,
  onClose,
  onToast,
}: ReportDialogProps) {
  // 폼 초기화는 상위에서 key 를 바꿔서 remount 하는 방식으로 처리한다.
  // (useEffect 에서 setState 금지 룰 회피)
  const [reason, setReason] = useState<ReportReason>('SPAM')
  const [detail, setDetail] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [errorMsg, setErrorMsg] = useState<string | null>(null)

  // ESC 로 닫기
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open, onClose])

  if (!open) return null

  const notify = (kind: ToastKind, message: string) => {
    if (onToast) onToast(kind, message)
    else if (kind === 'success') toast.success(message)
    else toast.error(message)
  }

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault()
    if (submitting) return
    setSubmitting(true)
    setErrorMsg(null)
    try {
      await reportApi.create(targetType, targetId, reason, detail.trim() || undefined)
      notify('success', '신고가 접수되었습니다.')
      onClose()
    } catch (err) {
      let msg = '신고 접수에 실패했습니다.'
      if (err instanceof ApiError) {
        if (err.status === 409) msg = '이미 신고한 게시물입니다.'
        else if (err.status === 401 || err.status === 403) msg = '로그인이 필요합니다.'
        else if (err.message) msg = err.message
      } else if (err instanceof Error && err.message) {
        msg = err.message
      }
      setErrorMsg(msg)
      notify('error', msg)
    } finally {
      setSubmitting(false)
    }
  }

  const titleLabel = targetType === 'POST' ? '게시글 신고' : '댓글 신고'

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="report-dialog-title"
      className="fixed inset-0 z-50 flex items-end justify-center bg-black/40 p-0 sm:items-center sm:p-4"
      onClick={(e) => {
        // 배경 클릭 시 닫기
        if (e.target === e.currentTarget) onClose()
      }}
    >
      <form
        onSubmit={handleSubmit}
        className="w-full max-w-md rounded-t-2xl bg-white p-5 shadow-xl sm:rounded-2xl"
      >
        <div className="mb-3 flex items-center justify-between">
          <h2 id="report-dialog-title" className="text-base font-semibold text-gray-900">
            {titleLabel}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="rounded-full p-1 text-gray-500 hover:bg-gray-100"
          >
            ✕
          </button>
        </div>

        <fieldset className="flex flex-col gap-2">
          <legend className="mb-1 text-sm font-medium text-gray-700">신고 사유</legend>
          {REASONS.map((r) => (
            <label
              key={r}
              className="flex cursor-pointer items-center gap-2 rounded-lg border border-gray-200 px-3 py-2 text-sm text-gray-800 has-[:checked]:border-blue-500 has-[:checked]:bg-blue-50"
            >
              <input
                type="radio"
                name="report-reason"
                value={r}
                checked={reason === r}
                onChange={() => setReason(r)}
                className="h-4 w-4 border-gray-300 text-blue-600"
              />
              {REPORT_REASON_LABELS[r]}
            </label>
          ))}
        </fieldset>

        <div className="mt-3">
          <label htmlFor="report-detail" className="mb-1 block text-sm font-medium text-gray-700">
            상세 내용 (선택)
          </label>
          <textarea
            id="report-detail"
            value={detail}
            onChange={(e) => setDetail(e.target.value)}
            maxLength={2000}
            placeholder="신고 사유를 더 자세히 설명해주세요."
            className="min-h-[80px] w-full resize-y rounded-lg border border-gray-200 bg-white p-2 text-sm placeholder:text-gray-500 focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-200"
          />
        </div>

        {errorMsg && (
          <p className="mt-2 text-xs text-red-600" role="alert">
            {errorMsg}
          </p>
        )}

        <div className="mt-4 flex items-center justify-end gap-2">
          <button
            type="button"
            onClick={onClose}
            disabled={submitting}
            className="inline-flex min-h-[44px] items-center justify-center rounded-full border border-gray-200 bg-white px-4 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-60"
          >
            취소
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="inline-flex min-h-[44px] items-center justify-center rounded-full bg-red-600 px-4 text-sm font-semibold text-white hover:bg-red-700 disabled:opacity-60"
          >
            {submitting ? '접수 중…' : '신고'}
          </button>
        </div>
      </form>
    </div>
  )
}
