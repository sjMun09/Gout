'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { AlertTriangle, ArrowLeft, UserX } from 'lucide-react'
import { toast } from 'sonner'
import { userApi } from '@/lib/api'
import { useAuth } from '@/lib/auth'

export default function WithdrawPage() {
  const router = useRouter()
  const { isAuthenticated, isHydrated, clearTokens } = useAuth()

  const [confirmed, setConfirmed] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!isHydrated) return
    if (!isAuthenticated) {
      router.push('/login')
    }
  }, [isAuthenticated, isHydrated, router])

  const handleWithdraw = async () => {
    if (!confirmed) {
      setError('안내 사항을 확인해주세요.')
      return
    }
    setError(null)
    setSubmitting(true)
    try {
      await userApi.withdraw()
      // 토큰은 store 를 통해 일괄 정리 (access + refresh 모두).
      // goutcare:profile 캐시는 인증 토큰이 아니므로 직접 제거.
      clearTokens()
      try {
        localStorage.removeItem('goutcare:profile')
      } catch {
        // ignore
      }
      toast.success('회원 탈퇴가 완료되었어요. 이용해주셔서 감사합니다.')
      router.push('/login')
    } catch (e) {
      setError(
        e instanceof Error ? e.message : '탈퇴 처리에 실패했습니다.',
      )
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <header className="flex items-center gap-3">
        <Link
          href="/profile"
          aria-label="뒤로"
          className="inline-flex h-10 w-10 items-center justify-center rounded-full hover:bg-gray-100"
        >
          <ArrowLeft className="h-5 w-5" />
        </Link>
        <h1 className="text-2xl font-bold text-gray-900">회원 탈퇴</h1>
      </header>

      <div className="rounded-2xl border border-red-200 bg-red-50 p-4">
        <div className="flex items-center gap-2 text-red-700">
          <AlertTriangle className="h-5 w-5" aria-hidden="true" />
          <span className="text-base font-semibold">정말 탈퇴하시겠습니까?</span>
        </div>
        <ul className="mt-3 list-disc space-y-1 pl-5 text-sm text-red-800">
          <li>작성한 요산 기록/복약 기록 등 활동 내역이 더 이상 조회되지 않습니다.</li>
          <li>같은 이메일로 다시 가입하려면 운영팀 문의가 필요할 수 있습니다.</li>
          <li>탈퇴 즉시 남아있는 로그인 세션은 모두 만료됩니다.</li>
        </ul>
      </div>

      {error && (
        <div
          role="alert"
          className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"
        >
          {error}
        </div>
      )}

      <label className="flex items-start gap-2 rounded-2xl border border-gray-200 bg-white p-4">
        <input
          type="checkbox"
          checked={confirmed}
          onChange={(e) => setConfirmed(e.target.checked)}
          className="mt-1 h-5 w-5"
        />
        <span className="text-sm text-gray-800">
          위 안내 사항을 모두 확인했으며, 탈퇴에 동의합니다.
        </span>
      </label>

      <div className="flex gap-3">
        <Link
          href="/profile"
          className="inline-flex min-h-[48px] flex-1 items-center justify-center rounded-xl border border-gray-300 bg-white text-base font-semibold text-gray-700 hover:bg-gray-50"
        >
          취소
        </Link>
        <button
          type="button"
          onClick={handleWithdraw}
          disabled={!confirmed || submitting}
          className="inline-flex min-h-[48px] flex-1 items-center justify-center gap-2 rounded-xl bg-red-600 text-base font-semibold text-white hover:bg-red-700 disabled:opacity-50"
        >
          <UserX className="h-5 w-5" aria-hidden="true" />
          {submitting ? '처리 중…' : '탈퇴하기'}
        </button>
      </div>
    </div>
  )
}
