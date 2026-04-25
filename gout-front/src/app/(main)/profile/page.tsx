'use client'

import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useCallback, useEffect, useState } from 'react'
import {
  Bookmark,
  ChevronRight,
  KeyRound,
  LogOut,
  Save,
  UserCircle2,
  UserPen,
  UserX,
} from 'lucide-react'
import {
  type UserAgeGroup,
  type UserProfile,
  userApi,
} from '@/lib/api'

const AGE_GROUP_LABELS: Record<UserAgeGroup, string> = {
  TWENTIES: '20대',
  THIRTIES: '30대',
  FORTIES: '40대',
  FIFTIES: '50대',
  SIXTIES: '60대',
  SEVENTIES_PLUS: '70대 이상',
}

const AGE_GROUP_OPTIONS: UserAgeGroup[] = [
  'TWENTIES',
  'THIRTIES',
  'FORTIES',
  'FIFTIES',
  'SIXTIES',
  'SEVENTIES_PLUS',
]

const LOCAL_KEY = 'goutcare:profile'

function loadLocalProfile(): UserProfile | null {
  if (typeof window === 'undefined') return null
  try {
    const raw = localStorage.getItem(LOCAL_KEY)
    if (!raw) return null
    return JSON.parse(raw) as UserProfile
  } catch {
    return null
  }
}

function saveLocalProfile(p: UserProfile) {
  if (typeof window === 'undefined') return
  try {
    localStorage.setItem(LOCAL_KEY, JSON.stringify(p))
  } catch {
    // ignore quota/serialization errors
  }
}

export default function ProfilePage() {
  const router = useRouter()

  const [hasToken, setHasToken] = useState<boolean | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [serverBackedUp, setServerBackedUp] = useState<boolean | null>(null)

  const [nickname, setNickname] = useState('')
  const [ageGroup, setAgeGroup] = useState<UserAgeGroup | ''>('')
  const [goutDiagnosedAt, setGoutDiagnosedAt] = useState('')
  const [targetUricAcid, setTargetUricAcid] = useState<string>('6.0')

  const bootstrap = useCallback(async () => {
    const token =
      typeof window !== 'undefined'
        ? localStorage.getItem('accessToken')
        : null
    setHasToken(!!token)

    // 먼저 로컬 캐시 반영 (오프라인/서버 미지원 대비)
    const local = loadLocalProfile()
    if (local) {
      setNickname(local.nickname ?? '')
      setAgeGroup((local.ageGroup as UserAgeGroup) ?? '')
      setGoutDiagnosedAt(local.goutDiagnosedAt ?? '')
      setTargetUricAcid(
        typeof local.targetUricAcid === 'number'
          ? String(local.targetUricAcid)
          : '6.0',
      )
    }

    if (!token) {
      setLoading(false)
      return
    }

    try {
      const me = await userApi.me()
      setNickname(me.nickname ?? '')
      setAgeGroup((me.ageGroup as UserAgeGroup) ?? '')
      setGoutDiagnosedAt(me.goutDiagnosedAt ?? '')
      setTargetUricAcid(
        typeof me.targetUricAcid === 'number'
          ? String(me.targetUricAcid)
          : '6.0',
      )
      setServerBackedUp(true)
      saveLocalProfile(me)
    } catch {
      // 백엔드 /api/users/me 미구현 또는 401 — 로컬만 사용
      setServerBackedUp(false)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    bootstrap()
  }, [bootstrap])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const targetNum = parseFloat(targetUricAcid)
    if (!Number.isFinite(targetNum) || targetNum < 1 || targetNum > 20) {
      alert('목표 요산수치는 1.0 ~ 20.0 사이 값을 입력해주세요')
      return
    }

    const payload: UserProfile = {
      nickname: nickname.trim() || undefined,
      ageGroup: (ageGroup || undefined) as UserAgeGroup | undefined,
      goutDiagnosedAt: goutDiagnosedAt || undefined,
      targetUricAcid: Math.round(targetNum * 10) / 10,
    }

    setSaving(true)
    saveLocalProfile(payload) // 낙관적 로컬 저장

    try {
      await userApi.updateMe(payload)
      setServerBackedUp(true)
      alert('저장되었습니다')
    } catch {
      setServerBackedUp(false)
      // TODO: 백엔드 /api/users/me 구현 후 에러 분기 구체화
      alert('서버 연동 준비 중입니다. 기기에만 임시 저장되었어요.')
    } finally {
      setSaving(false)
    }
  }

  const handleLogout = () => {
    if (!confirm('로그아웃 할까요?')) return
    try {
      localStorage.removeItem('accessToken')
    } catch {
      // ignore
    }
    router.push('/login')
  }

  if (loading) {
    return (
      <div className="flex flex-col gap-4 px-5 py-6">
        <div className="h-8 w-32 animate-pulse rounded bg-gray-200" />
        <div className="h-64 animate-pulse rounded-2xl bg-gray-100" />
      </div>
    )
  }

  if (!hasToken) {
    return (
      <div className="flex min-h-[70vh] flex-col items-center justify-center gap-4 px-5 py-10 text-center">
        <span
          className="inline-flex h-16 w-16 items-center justify-center rounded-full bg-blue-50 text-blue-600"
          aria-hidden="true"
        >
          <UserCircle2 className="h-8 w-8" />
        </span>
        <h1 className="text-2xl font-bold text-gray-900">로그인이 필요해요</h1>
        <p className="max-w-xs text-base text-gray-600">
          내 정보를 보고 수정하려면 로그인 해주세요
        </p>
        <Link
          href="/login"
          className="mt-2 inline-flex min-h-[48px] items-center justify-center rounded-xl bg-blue-600 px-6 text-base font-semibold text-white hover:bg-blue-700"
        >
          로그인 하러 가기
        </Link>
      </div>
    )
  }

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <header>
        <h1 className="text-2xl font-bold text-gray-900">내 정보</h1>
        <p className="mt-1 text-base text-gray-600">
          나에게 맞는 관리를 위해 정보를 알려주세요
        </p>
      </header>

      {serverBackedUp === false && (
        <div
          role="alert"
          className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-800"
        >
          서버 연동 준비 중이에요. 변경사항은 현재 기기에만 임시 저장됩니다.
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
            placeholder="예: 통풍이"
            maxLength={20}
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">연령대</span>
          <select
            value={ageGroup}
            onChange={(e) => setAgeGroup(e.target.value as UserAgeGroup | '')}
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          >
            <option value="">선택 안 함</option>
            {AGE_GROUP_OPTIONS.map((g) => (
              <option key={g} value={g}>
                {AGE_GROUP_LABELS[g]}
              </option>
            ))}
          </select>
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">통풍 진단일</span>
          <input
            type="date"
            value={goutDiagnosedAt}
            onChange={(e) => setGoutDiagnosedAt(e.target.value)}
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
        </label>

        <label className="flex flex-col gap-1">
          <span className="text-sm text-gray-700">
            목표 요산수치 (mg/dL)
          </span>
          <input
            type="number"
            inputMode="decimal"
            step="0.1"
            min="1"
            max="20"
            value={targetUricAcid}
            onChange={(e) => setTargetUricAcid(e.target.value)}
            className="min-h-[44px] rounded-xl border border-gray-300 px-3 text-base"
          />
          <span className="text-xs text-gray-500">
            일반적으로 6.0 mg/dL 이하를 권장해요
          </span>
        </label>

        <button
          type="submit"
          disabled={saving}
          aria-busy={saving}
          className="inline-flex min-h-[48px] items-center justify-center gap-2 rounded-xl bg-blue-600 text-base font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          <Save className="h-5 w-5" aria-hidden="true" />
          {saving ? '저장 중…' : '저장'}
        </button>
      </form>

      <section className="flex flex-col gap-2">
        <h2 className="text-sm font-semibold text-gray-600">계정 관리</h2>
        <div className="flex flex-col divide-y divide-gray-200 rounded-2xl border border-gray-200 bg-white">
          <Link
            href="/profile/edit"
            className="flex min-h-[48px] items-center gap-3 px-4 text-base text-gray-800 hover:bg-gray-50"
          >
            <UserPen className="h-5 w-5 text-gray-500" aria-hidden="true" />
            프로필 수정 (닉네임 · 출생연도 · 성별)
          </Link>
          <Link
            href="/profile/password"
            className="flex min-h-[48px] items-center gap-3 px-4 text-base text-gray-800 hover:bg-gray-50"
          >
            <KeyRound className="h-5 w-5 text-gray-500" aria-hidden="true" />
            비밀번호 변경
          </Link>
          <Link
            href="/profile/bookmarks"
            className="flex min-h-[48px] items-center justify-between gap-3 px-4 text-base text-gray-800 hover:bg-gray-50"
          >
            <span className="inline-flex items-center gap-3">
              <Bookmark className="h-5 w-5 text-gray-500" aria-hidden="true" />
              내 스크랩
            </span>
            <ChevronRight className="h-5 w-5 text-gray-500" aria-hidden="true" />
          </Link>
          <Link
            href="/profile/withdraw"
            className="flex min-h-[48px] items-center gap-3 px-4 text-base text-red-600 hover:bg-red-50"
          >
            <UserX className="h-5 w-5" aria-hidden="true" />
            회원 탈퇴
          </Link>
        </div>
      </section>

      <button
        type="button"
        onClick={handleLogout}
        className="inline-flex min-h-[48px] items-center justify-center gap-2 rounded-xl border border-gray-300 bg-white text-base font-semibold text-gray-700 hover:bg-gray-50"
      >
        <LogOut className="h-5 w-5" aria-hidden="true" />
        로그아웃
      </button>
    </div>
  )
}
