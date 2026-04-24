'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useEffect, useState } from 'react'
import { ArrowLeft, KeyRound } from 'lucide-react'
import { userApi } from '@/lib/api'

export default function ChangePasswordPage() {
  const router = useRouter()

  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const token =
      typeof window !== 'undefined'
        ? localStorage.getItem('accessToken')
        : null
    if (!token) {
      router.push('/login')
    }
  }, [router])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (newPassword.length < 8) {
      setError('새 비밀번호는 8자 이상이어야 합니다.')
      return
    }
    if (newPassword !== confirmPassword) {
      setError('새 비밀번호 확인이 일치하지 않습니다.')
      return
    }
    if (currentPassword === newPassword) {
      setError('현재 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.')
      return
    }

    setSubmitting(true)
    try {
      await userApi.changePassword({ currentPassword, newPassword })
      alert(
        '비밀번호가 변경되었습니다. 기존 로그인 상태는 유지됩니다. 다음 로그인부터 새 비밀번호를 사용해주세요.',
      )
      router.push('/profile')
    } catch (e) {
      // 400/401: 현재 비밀번호 불일치.
      setError(
        e instanceof Error
          ? e.message.includes('401') || e.message.includes('400')
            ? '현재 비밀번호가 올바르지 않습니다.'
            : e.message
          : '비밀번호 변경에 실패했습니다.',
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
        <h1 className="text-2xl font-bold text-gray-900">비밀번호 변경</h1>
      </header>

      {error && (
        <div
          role="alert"
          className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700"
        >
          {error}
        </div>
      )}

      <form
        onSubmit={handleSubmit}
        className="flex flex-col gap-4 rounded-2xl border border-gray-200 bg-white p-4"
        autoComplete="off"
      >
        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">현재 비밀번호</span>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            required
            autoComplete="current-password"
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">새 비밀번호 (8자 이상)</span>
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
            minLength={8}
            autoComplete="new-password"
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">새 비밀번호 확인</span>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            required
            minLength={8}
            autoComplete="new-password"
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <button
          type="submit"
          disabled={submitting}
          className="inline-flex min-h-[48px] items-center justify-center gap-2 rounded-xl bg-blue-600 text-base font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          <KeyRound className="h-5 w-5" aria-hidden="true" />
          {submitting ? '변경 중…' : '비밀번호 변경'}
        </button>
      </form>
    </div>
  )
}
