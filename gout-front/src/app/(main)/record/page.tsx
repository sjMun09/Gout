'use client'

import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useCallback, useEffect, useMemo, useState } from 'react'
import { Trash2 } from 'lucide-react'
import {
  GoutAttackLog,
  MedicationLog,
  UricAcidLog,
  healthApi,
} from '@/lib/api'

type TabKey = 'uric' | 'attack' | 'medication'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'uric', label: '요산수치' },
  { key: 'attack', label: '발작일지' },
  { key: 'medication', label: '복약' },
]

function isValidTab(value: string | null): value is TabKey {
  return value === 'uric' || value === 'attack' || value === 'medication'
}

function todayYmd(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function nowLocalDatetime(): string {
  // datetime-local expects "YYYY-MM-DDTHH:mm"
  const d = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(
    d.getHours(),
  )}:${pad(d.getMinutes())}`
}

function formatDateKr(isoLike: string): string {
  // isoLike: "YYYY-MM-DD" or ISO string
  const d = new Date(isoLike)
  if (Number.isNaN(d.getTime())) return isoLike
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
}

function formatDateTimeKr(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(
    d.getHours(),
  )}:${pad(d.getMinutes())}`
}

export default function RecordPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const tabParam = searchParams.get('tab')
  const activeTab: TabKey = isValidTab(tabParam) ? tabParam : 'uric'

  const [hasToken, setHasToken] = useState<boolean | null>(null)

  useEffect(() => {
    // localStorage 접근은 클라이언트에서만 (SSR 방어)
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setHasToken(!!localStorage.getItem('accessToken'))
  }, [])

  const changeTab = (key: TabKey) => {
    const params = new URLSearchParams(searchParams.toString())
    params.set('tab', key)
    router.replace(`/record?${params.toString()}`)
  }

  if (hasToken === null) {
    // 초기 하이드레이션 중
    return (
      <div className="flex flex-col gap-5 px-5 py-6">
        <HeaderBlock />
        <div className="h-60 animate-pulse rounded-2xl bg-gray-100" />
      </div>
    )
  }

  if (!hasToken) {
    return (
      <div className="flex min-h-[70vh] flex-col items-center justify-center gap-4 px-5 py-10 text-center">
        <h1 className="text-2xl font-bold text-gray-900">로그인이 필요해요</h1>
        <p className="text-base text-gray-600">
          건강 기록은 로그인 후 이용할 수 있어요
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
      <HeaderBlock />

      {/* 탭 */}
      <div
        className="flex rounded-2xl bg-gray-100 p-1"
        role="tablist"
        aria-label="기록 종류"
      >
        {TABS.map((tab) => (
          <button
            key={tab.key}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.key}
            onClick={() => changeTab(tab.key)}
            className={`min-h-[48px] flex-1 rounded-xl text-base font-medium transition-colors ${
              activeTab === tab.key
                ? 'bg-white text-gray-900 shadow-sm'
                : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'uric' && <UricAcidTab />}
      {activeTab === 'attack' && <AttackTab />}
      {activeTab === 'medication' && <MedicationTab />}
    </div>
  )
}

function HeaderBlock() {
  return (
    <header>
      <h1 className="text-2xl font-bold text-gray-900">건강 기록</h1>
      <p className="mt-1 text-base text-gray-600">
        꾸준한 기록이 건강 관리의 시작입니다
      </p>
    </header>
  )
}

function Skeleton() {
  return (
    <div className="flex flex-col gap-3">
      <div className="h-32 animate-pulse rounded-2xl bg-gray-100" />
      <div className="h-20 animate-pulse rounded-2xl bg-gray-100" />
      <div className="h-20 animate-pulse rounded-2xl bg-gray-100" />
    </div>
  )
}

function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">
      {message}
    </div>
  )
}

function parseError(e: unknown): string {
  const msg = e instanceof Error ? e.message : String(e)
  if (msg.includes('401')) return '세션이 만료됐어요, 다시 로그인해주세요'
  return '요청 처리 중 오류가 발생했어요'
}

/* ========== Tab 1: 요산수치 ========== */

function UricAcidTab() {
  const [logs, setLogs] = useState<UricAcidLog[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const [measuredAt, setMeasuredAt] = useState(todayYmd())
  const [valueStr, setValueStr] = useState('')
  const [memo, setMemo] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await healthApi.getUricAcidLogs()
      setLogs(data)
    } catch (e) {
      setError(parseError(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const value = parseFloat(valueStr)
    if (!Number.isFinite(value) || value < 0 || value > 20) {
      alert('수치는 0.0 ~ 20.0 사이 값을 입력해주세요')
      return
    }
    if (!measuredAt) {
      alert('측정일을 입력해주세요')
      return
    }
    setSubmitting(true)
    try {
      await healthApi.createUricAcidLog({
        value: Math.round(value * 10) / 10,
        measuredAt,
        memo: memo.trim() || undefined,
      })
      setValueStr('')
      setMemo('')
      setMeasuredAt(todayYmd())
      await load()
    } catch (err) {
      alert(parseError(err))
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: string) => {
    if (!confirm('이 기록을 삭제할까요?')) return
    try {
      await healthApi.deleteUricAcidLog(id)
      setLogs((prev) => prev.filter((l) => l.id !== id))
    } catch (err) {
      alert(parseError(err))
    }
  }

  // 최신순 정렬된 목록
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
      {error && <ErrorBanner message={error} />}

      {/* 그래프 */}
      <section className="rounded-2xl border border-gray-200 bg-white p-4">
        <div className="mb-2 flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-900">
            최근 추이 (최대 10개)
          </h2>
          <span className="text-xs text-gray-500">정상 기준 6.0 mg/dL</span>
        </div>
        <UricAcidChart logs={logs} />
      </section>

      {/* 입력 폼 */}
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
          <span className="text-sm text-gray-700">수치 (mg/dL)</span>
          <input
            type="number"
            inputMode="decimal"
            step="0.1"
            min="0"
            max="20"
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
          className="min-h-[48px] rounded-xl bg-blue-600 text-base font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
        >
          {submitting ? '저장 중…' : '기록 추가'}
        </button>
      </form>

      {/* 목록 */}
      <section className="flex flex-col gap-2">
        <h2 className="text-base font-semibold text-gray-900">전체 기록</h2>
        {sortedLatest.length === 0 ? (
          <p className="rounded-2xl border border-dashed border-gray-200 bg-white p-6 text-center text-sm text-gray-500">
            아직 기록이 없어요
          </p>
        ) : (
          <ul className="flex flex-col gap-2">
            {sortedLatest.map((log) => {
              const normal = log.value <= 6.0
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
                      {log.value.toFixed(1)}{' '}
                      <span className="text-sm font-normal text-gray-600">
                        mg/dL
                      </span>
                    </p>
                    {log.memo && (
                      <p className="mt-1 text-sm text-gray-600">{log.memo}</p>
                    )}
                  </div>
                  <button
                    type="button"
                    onClick={() => handleDelete(log.id)}
                    aria-label="삭제"
                    className="text-gray-400 hover:text-red-600"
                  >
                    <Trash2 className="h-5 w-5" />
                  </button>
                </li>
              )
            })}
          </ul>
        )}
      </section>
    </div>
  )
}

function UricAcidChart({ logs }: { logs: UricAcidLog[] }) {
  const W = 300
  const H = 120
  const PAD_L = 24
  const PAD_R = 8
  const PAD_T = 10
  const PAD_B = 20
  const plotW = W - PAD_L - PAD_R
  const plotH = H - PAD_T - PAD_B

  // 최근 10개, measuredAt 오름차순
  const data = useMemo(() => {
    return [...logs]
      .sort(
        (a, b) =>
          new Date(a.measuredAt).getTime() - new Date(b.measuredAt).getTime(),
      )
      .slice(-10)
  }, [logs])

  if (data.length === 0) {
    return (
      <div className="flex h-[120px] items-center justify-center text-sm text-gray-500">
        아직 기록이 없어요
      </div>
    )
  }

  const maxVal = Math.max(12, ...data.map((d) => d.value + 1))
  const minVal = 0
  const THRESHOLD = 6.0

  const x = (i: number) => {
    if (data.length === 1) return PAD_L + plotW / 2
    return PAD_L + (i / (data.length - 1)) * plotW
  }
  const y = (v: number) => {
    const ratio = (v - minVal) / (maxVal - minVal)
    return PAD_T + plotH - ratio * plotH
  }

  const thresholdY = y(THRESHOLD)

  // 구간별 색상: 정상(<=6)=blue, 초과(>6)=red
  // 연속 구간을 이어붙이기 위해 세그먼트 단위로 polyline 두 세트를 그림
  const segments: { color: 'blue' | 'red'; points: string }[] = []
  for (let i = 0; i < data.length - 1; i++) {
    const a = data[i]
    const b = data[i + 1]
    const color: 'blue' | 'red' =
      a.value > THRESHOLD || b.value > THRESHOLD ? 'red' : 'blue'
    segments.push({
      color,
      points: `${x(i)},${y(a.value)} ${x(i + 1)},${y(b.value)}`,
    })
  }

  return (
    <svg
      viewBox={`0 0 ${W} ${H}`}
      className="h-auto w-full"
      role="img"
      aria-label="요산수치 추이 그래프"
    >
      {/* Y축 눈금 (0, 6, maxVal) */}
      <line
        x1={PAD_L}
        y1={PAD_T}
        x2={PAD_L}
        y2={PAD_T + plotH}
        stroke="#e5e7eb"
        strokeWidth="1"
      />
      <line
        x1={PAD_L}
        y1={PAD_T + plotH}
        x2={PAD_L + plotW}
        y2={PAD_T + plotH}
        stroke="#e5e7eb"
        strokeWidth="1"
      />

      {/* 정상 기준선 6.0 (점선) */}
      <line
        x1={PAD_L}
        y1={thresholdY}
        x2={PAD_L + plotW}
        y2={thresholdY}
        stroke="#f59e0b"
        strokeWidth="1"
        strokeDasharray="4 3"
      />
      <text x={PAD_L + 2} y={thresholdY - 3} fontSize="9" fill="#f59e0b">
        6.0
      </text>

      {/* Y축 라벨 */}
      <text x="2" y={PAD_T + 4} fontSize="9" fill="#6b7280">
        {maxVal.toFixed(0)}
      </text>
      <text x="2" y={PAD_T + plotH + 4} fontSize="9" fill="#6b7280">
        0
      </text>

      {/* 선 구간 */}
      {segments.map((seg, i) => (
        <polyline
          key={i}
          points={seg.points}
          fill="none"
          stroke={seg.color === 'blue' ? '#2563eb' : '#dc2626'}
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      ))}

      {/* 데이터 포인트 */}
      {data.map((d, i) => {
        const over = d.value > THRESHOLD
        return (
          <g key={d.id}>
            <circle
              cx={x(i)}
              cy={y(d.value)}
              r="3"
              fill={over ? '#dc2626' : '#2563eb'}
            />
          </g>
        )
      })}

      {/* X축 라벨 - 첫/마지막 날짜만 */}
      {data.length > 0 && (
        <>
          <text
            x={x(0)}
            y={H - 4}
            fontSize="9"
            fill="#6b7280"
            textAnchor="start"
          >
            {data[0].measuredAt.slice(5)}
          </text>
          {data.length > 1 && (
            <text
              x={x(data.length - 1)}
              y={H - 4}
              fontSize="9"
              fill="#6b7280"
              textAnchor="end"
            >
              {data[data.length - 1].measuredAt.slice(5)}
            </text>
          )}
        </>
      )}
    </svg>
  )
}

/* ========== Tab 2: 발작일지 ========== */

function AttackTab() {
  const [logs, setLogs] = useState<GoutAttackLog[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const [attackedAt, setAttackedAt] = useState(todayYmd())
  const [painLevel, setPainLevel] = useState<number>(5)
  const [location, setLocation] = useState('')
  const [durationStr, setDurationStr] = useState('')
  const [suspectedCause, setSuspectedCause] = useState('')
  const [memo, setMemo] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await healthApi.getGoutAttackLogs()
      setLogs(data)
    } catch (e) {
      setError(parseError(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!attackedAt) {
      alert('발작일을 입력해주세요')
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
      // reset
      setAttackedAt(todayYmd())
      setPainLevel(5)
      setLocation('')
      setDurationStr('')
      setSuspectedCause('')
      setMemo('')
      await load()
    } catch (err) {
      alert(parseError(err))
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: string) => {
    if (!confirm('이 기록을 삭제할까요?')) return
    try {
      await healthApi.deleteGoutAttackLog(id)
      setLogs((prev) => prev.filter((l) => l.id !== id))
    } catch (err) {
      alert(parseError(err))
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
                  aria-label="삭제"
                  className="text-gray-400 hover:text-red-600"
                >
                  <Trash2 className="h-5 w-5" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}

/* ========== Tab 3: 복약 ========== */

function MedicationTab() {
  const [logs, setLogs] = useState<MedicationLog[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const [medicationName, setMedicationName] = useState('')
  const [dosage, setDosage] = useState('')
  const [takenAt, setTakenAt] = useState(nowLocalDatetime())

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await healthApi.getMedicationLogs()
      setLogs(data)
    } catch (e) {
      setError(parseError(e))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!medicationName.trim()) {
      alert('약품명을 입력해주세요')
      return
    }
    if (!takenAt) {
      alert('복약 시간을 입력해주세요')
      return
    }
    setSubmitting(true)
    try {
      // datetime-local → ISO (초 단위 보강)
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
    } catch (err) {
      alert(parseError(err))
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: string) => {
    if (!confirm('이 기록을 삭제할까요?')) return
    try {
      await healthApi.deleteMedicationLog(id)
      setLogs((prev) => prev.filter((l) => l.id !== id))
    } catch (err) {
      alert(parseError(err))
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
                  aria-label="삭제"
                  className="text-gray-400 hover:text-red-600"
                >
                  <Trash2 className="h-5 w-5" />
                </button>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  )
}
