'use client'

import Link from 'next/link'
import { useEffect, useState } from 'react'
import {
  AlertTriangle,
  ChevronRight,
  MapPin,
  Phone,
  Siren,
  X,
} from 'lucide-react'
import { contentApi, type Guideline } from '@/lib/api'

interface StaticItem {
  title: string
  description?: string
}

const doNow: StaticItem[] = [
  {
    title: '발작 부위 높이 올리기',
    description: '심장보다 높게 올리면 부기와 통증이 줄어듭니다.',
  },
  {
    title: '얼음 찜질',
    description: '수건으로 감싼 얼음팩을 20분간 대세요.',
  },
  {
    title: '물 충분히 마시기',
    description: '요산을 희석시켜 배출을 돕습니다.',
  },
  {
    title: '항염증 진통제 (NSAIDs)',
    description: '처방받은 경우에만 복용하세요.',
  },
]

const dontNow: StaticItem[] = [
  {
    title: '발작 부위 마사지 금지',
    description: '염증을 악화시킬 수 있어요.',
  },
  {
    title: '퓨린 높은 음식 섭취 금지',
    description: '내장, 붉은 고기, 등푸른 생선 등.',
  },
  {
    title: '음주 금지',
    description: '요산 수치를 급격히 높입니다.',
  },
]

const hospitalAlertItems: StaticItem[] = [
  { title: '고열(38도 이상) 동반' },
  { title: '여러 관절에서 동시 발작' },
  { title: '극심한 통증으로 움직임 불가' },
]

export default function EmergencyPage() {
  const [extraGuidelines, setExtraGuidelines] = useState<Guideline[]>([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true)
      try {
        const data = await contentApi.getGuidelines({ category: 'EMERGENCY' })
        if (!cancelled) setExtraGuidelines(data ?? [])
      } catch {
        // 오프라인 또는 서버 에러 — 정적 콘텐츠는 그대로 노출
        if (!cancelled) setExtraGuidelines([])
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="flex flex-col">
      {/* 헤더 (빨간 배경) */}
      <header className="bg-red-600 px-5 py-6 text-white">
        <div className="flex items-start gap-3">
          <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white/20">
            <Siren className="h-5 w-5" aria-hidden="true" />
          </span>
          <div>
            <h1 className="text-2xl font-bold">통풍 발작 응급 가이드</h1>
            <p className="mt-1 text-sm text-white/90">
              발작이 왔을 때 바로 따라하세요
            </p>
          </div>
        </div>
      </header>

      <div className="flex flex-col gap-5 px-5 py-6">
        {/* 즉시 할 일 */}
        <section
          aria-labelledby="emergency-do-title"
          className="rounded-2xl border border-green-100 bg-green-50 p-4"
        >
          <h2
            id="emergency-do-title"
            className="mb-3 flex items-center gap-2 text-lg font-bold text-green-900"
          >
            <span aria-hidden="true">⚡</span>
            즉시 할 일
          </h2>
          <ol className="flex flex-col gap-2">
            {doNow.map((item, idx) => (
              <li
                key={idx}
                className="flex gap-3 rounded-xl bg-white p-3"
              >
                <span
                  className="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-green-600 text-xs font-bold text-white"
                  aria-hidden="true"
                >
                  {idx + 1}
                </span>
                <div className="flex-1">
                  <p className="text-sm font-semibold text-gray-900">
                    {item.title}
                  </p>
                  {item.description && (
                    <p className="mt-0.5 text-xs text-gray-600">
                      {item.description}
                    </p>
                  )}
                </div>
              </li>
            ))}
          </ol>
        </section>

        {/* 하지 말아야 할 것 */}
        <section
          aria-labelledby="emergency-dont-title"
          className="rounded-2xl border border-red-100 bg-red-50 p-4"
        >
          <h2
            id="emergency-dont-title"
            className="mb-3 flex items-center gap-2 text-lg font-bold text-red-900"
          >
            <X className="h-5 w-5" aria-hidden="true" />
            하지 말아야 할 것
          </h2>
          <ul className="flex flex-col gap-2">
            {dontNow.map((item, idx) => (
              <li key={idx} className="flex gap-3 rounded-xl bg-white p-3">
                <span
                  className="inline-flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-red-100 text-red-700"
                  aria-hidden="true"
                >
                  <X className="h-3.5 w-3.5" />
                </span>
                <div className="flex-1">
                  <p className="text-sm font-semibold text-gray-900">
                    {item.title}
                  </p>
                  {item.description && (
                    <p className="mt-0.5 text-xs text-gray-600">
                      {item.description}
                    </p>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </section>

        {/* 응급 연락처 */}
        <section
          aria-labelledby="emergency-phone-title"
          className="rounded-2xl border border-gray-200 bg-white p-4"
        >
          <h2
            id="emergency-phone-title"
            className="mb-3 flex items-center gap-2 text-lg font-bold text-gray-900"
          >
            <Phone className="h-5 w-5 text-blue-600" aria-hidden="true" />
            응급 연락처
          </h2>
          <div className="flex flex-col gap-2">
            <a
              href="tel:119"
              className="flex min-h-[56px] items-center justify-between rounded-2xl bg-red-600 px-4 text-white transition-colors hover:bg-red-700"
            >
              <div>
                <p className="text-sm font-medium text-white/90">응급 전화</p>
                <p className="text-lg font-bold">119</p>
              </div>
              <Phone className="h-5 w-5" aria-hidden="true" />
            </a>
            <a
              href="tel:16442828"
              className="flex min-h-[56px] items-center justify-between rounded-2xl border border-gray-200 bg-white px-4 text-gray-900 transition-colors hover:bg-gray-50"
            >
              <div>
                <p className="text-sm text-gray-600">약사 상담</p>
                <p className="text-lg font-bold">1644-2828</p>
              </div>
              <Phone className="h-5 w-5 text-blue-600" aria-hidden="true" />
            </a>
          </div>
        </section>

        {/* API에서 추가 가이드라인 */}
        {loading && extraGuidelines.length === 0 ? null : extraGuidelines.length >
          0 ? (
          <section
            aria-labelledby="emergency-extra-title"
            className="rounded-2xl border border-gray-200 bg-white p-4"
          >
            <h2
              id="emergency-extra-title"
              className="mb-3 text-lg font-bold text-gray-900"
            >
              추가 안내
            </h2>
            <ul className="flex flex-col gap-2">
              {extraGuidelines.map((g) => (
                <li
                  key={g.id}
                  className="rounded-xl border border-gray-100 bg-gray-50 p-3"
                >
                  <p className="text-sm font-semibold text-gray-900">
                    {g.title}
                  </p>
                  <p className="mt-1 whitespace-pre-line text-xs leading-relaxed text-gray-700">
                    {g.content}
                  </p>
                </li>
              ))}
            </ul>
          </section>
        ) : null}

        {/* 병원 바로 가야 할 때 */}
        <section
          aria-labelledby="emergency-hospital-title"
          className="rounded-2xl border border-amber-200 bg-amber-50 p-4"
        >
          <h2
            id="emergency-hospital-title"
            className="mb-2 flex items-center gap-2 text-base font-bold text-amber-900"
          >
            <AlertTriangle className="h-5 w-5" aria-hidden="true" />
            병원 바로 가야 할 때
          </h2>
          <ul className="mb-3 flex flex-col gap-1.5 text-sm text-amber-900">
            {hospitalAlertItems.map((item, idx) => (
              <li key={idx} className="flex gap-2">
                <span aria-hidden="true">•</span>
                <span>{item.title}</span>
              </li>
            ))}
          </ul>
          <Link
            href="/hospital"
            className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl bg-amber-600 px-4 text-sm font-semibold text-white transition-colors hover:bg-amber-700"
          >
            <MapPin className="h-4 w-4" aria-hidden="true" />
            근처 병원 찾기
            <ChevronRight className="h-4 w-4" aria-hidden="true" />
          </Link>
        </section>
      </div>
    </div>
  )
}
