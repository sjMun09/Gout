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
import { emergencyContent } from '@/content/emergency'

export default function EmergencyPage() {
  const [extraGuidelines, setExtraGuidelines] = useState<Guideline[]>([])
  const [loading, setLoading] = useState(false)
  const { header, sections, contacts } = emergencyContent

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true)
      try {
        const data = await contentApi.getGuidelines({
          category: sections.extraGuidelines.apiCategory,
        })
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
  }, [sections.extraGuidelines.apiCategory])

  return (
    <div className="flex flex-col">
      {/* 헤더 (빨간 배경) */}
      <header className="bg-red-600 px-5 py-6 text-white">
        <div className="flex items-start gap-3">
          <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-white/20">
            <Siren className="h-5 w-5" aria-hidden="true" />
          </span>
          <div>
            <h1 className="text-2xl font-bold">{header.title}</h1>
            <p className="mt-1 text-sm text-white/90">
              {header.description}
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
            <span aria-hidden="true">{sections.doNow.iconLabel}</span>
            {sections.doNow.title}
          </h2>
          <ol className="flex flex-col gap-2">
            {sections.doNow.items.map((item, idx) => (
              <li
                key={item.title}
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
            {sections.dontNow.title}
          </h2>
          <ul className="flex flex-col gap-2">
            {sections.dontNow.items.map((item) => (
              <li key={item.title} className="flex gap-3 rounded-xl bg-white p-3">
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
            {contacts.title}
          </h2>
          <div className="flex flex-col gap-2">
            {contacts.items.map((contact) => {
              const isPrimary = contact.variant === 'primary'
              return (
                <a
                  key={contact.phoneNumber}
                  href={contact.href}
                  className={
                    isPrimary
                      ? 'flex min-h-[56px] items-center justify-between rounded-2xl bg-red-600 px-4 text-white transition-colors hover:bg-red-700'
                      : 'flex min-h-[56px] items-center justify-between rounded-2xl border border-gray-200 bg-white px-4 text-gray-900 transition-colors hover:bg-gray-50'
                  }
                >
                  <div>
                    <p
                      className={
                        isPrimary
                          ? 'text-sm font-medium text-white/90'
                          : 'text-sm text-gray-600'
                      }
                    >
                      {contact.label}
                    </p>
                    <p className="text-lg font-bold">{contact.phoneNumber}</p>
                  </div>
                  <Phone
                    className={isPrimary ? 'h-5 w-5' : 'h-5 w-5 text-blue-600'}
                    aria-hidden="true"
                  />
                </a>
              )
            })}
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
              {sections.extraGuidelines.title}
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
            {sections.hospitalAlert.title}
          </h2>
          <ul className="mb-3 flex flex-col gap-1.5 text-sm text-amber-900">
            {sections.hospitalAlert.items.map((item) => (
              <li key={item.title} className="flex gap-2">
                <span aria-hidden="true">•</span>
                <span>{item.title}</span>
              </li>
            ))}
          </ul>
          <Link
            href={sections.hospitalAlert.actionHref}
            className="flex min-h-[48px] w-full items-center justify-center gap-2 rounded-2xl bg-amber-600 px-4 text-sm font-semibold text-white transition-colors hover:bg-amber-700"
          >
            <MapPin className="h-4 w-4" aria-hidden="true" />
            {sections.hospitalAlert.actionLabel}
            <ChevronRight className="h-4 w-4" aria-hidden="true" />
          </Link>
        </section>
      </div>
    </div>
  )
}
