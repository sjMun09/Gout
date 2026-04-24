'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useCallback, useEffect, useState } from 'react'
import { ArrowLeft, Save } from 'lucide-react'
import {
  type AccountProfile,
  type UserGender,
  userApi,
} from '@/lib/api'

const GENDER_OPTIONS: { value: UserGender; label: string }[] = [
  { value: 'MALE', label: '남성' },
  { value: 'FEMALE', label: '여성' },
  { value: 'OTHER', label: '기타' },
]

export default function ProfileEditPage() {
  const router = useRouter()

  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const [nickname, setNickname] = useState('')
  const [birthYear, setBirthYear] = useState<string>('')
  const [gender, setGender] = useState<UserGender | ''>('')

  const bootstrap = useCallback(async () => {
    const token =
      typeof window !== 'undefined'
        ? localStorage.getItem('accessToken')
        : null
    if (!token) {
      router.push('/login')
      return
    }

    try {
      const me: AccountProfile = await userApi.getAccount()
      setNickname(me.nickname ?? '')
      setBirthYear(me.birthYear != null ? String(me.birthYear) : '')
      setGender((me.gender ?? '') as UserGender | '')
    } catch (e) {
      setError(e instanceof Error ? e.message : '프로필을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }, [router])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    bootstrap()
  }, [bootstrap])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    const trimmed = nickname.trim()
    if (trimmed && (trimmed.length < 2 || trimmed.length > 20)) {
      setError('닉네임은 2~20자 사이여야 합니다.')
      return
    }

    let birthYearNum: number | null | undefined = undefined
    if (birthYear.trim() !== '') {
      const n = parseInt(birthYear, 10)
      if (!Number.isFinite(n) || n < 1900 || n > new Date().getFullYear()) {
        setError('올바른 출생연도를 입력해주세요.')
        return
      }
      birthYearNum = n
    }

    setSaving(true)
    try {
      await userApi.updateProfile({
        nickname: trimmed || undefined,
        birthYear: birthYearNum,
        gender: (gender || undefined) as UserGender | undefined,
      })
      alert('저장되었습니다.')
      router.push('/profile')
    } catch (e) {
      setError(e instanceof Error ? e.message : '저장에 실패했습니다.')
    } finally {
      setSaving(false)
    }
  }

  if (loading) {
    return (
      <div className="flex flex-col gap-4 px-5 py-6">
        <div className="h-8 w-32 animate-pulse rounded bg-gray-200" />
        <div className="h-64 animate-pulse rounded-2xl bg-gray-100" />
      </div>
    )
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
        <h1 className="text-2xl font-bold text-gray-900">프로필 수정</h1>
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
      >
        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">닉네임</span>
          <input
            type="text"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="2~20자"
            maxLength={20}
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">출생연도</span>
          <input
            type="number"
            inputMode="numeric"
            value={birthYear}
            onChange={(e) => setBirthYear(e.target.value)}
            placeholder="예: 1980"
            min="1900"
            max={new Date().getFullYear()}
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <fieldset className="flex flex-col gap-2">
          <legend className="text-sm text-gray-700">성별</legend>
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setGender('')}
              aria-pressed={gender === ''}
              className={`min-h-[44px] rounded-xl border px-4 text-base ${
                gender === ''
                  ? 'border-blue-600 bg-blue-50 text-blue-700'
                  : 'border-gray-300 bg-white text-gray-700'
              }`}
            >
              선택 안 함
            </button>
            {GENDER_OPTIONS.map((opt) => (
              <button
                type="button"
                key={opt.value}
                onClick={() => setGender(opt.value)}
                aria-pressed={gender === opt.value}
                className={`min-h-[44px] rounded-xl border px-4 text-base ${
                  gender === opt.value
                    ? 'border-blue-600 bg-blue-50 text-blue-700'
                    : 'border-gray-300 bg-white text-gray-700'
                }`}
              >
                {opt.label}
              </button>
            ))}
          </div>
        </fieldset>

        <button
          type="submit"
          disabled={saving}
          className="inline-flex min-h-[48px] items-center justify-center gap-2 rounded-xl bg-blue-600 text-base font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          <Save className="h-5 w-5" aria-hidden="true" />
          {saving ? '저장 중…' : '저장'}
        </button>
      </form>
    </div>
  )
}
