'use client'

import { useEffect } from 'react'
import Link from 'next/link'
import { AlertTriangle, RotateCcw } from 'lucide-react'

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string }
  reset: () => void
}) {
  useEffect(() => {
    // 운영 로깅 연동 필요 시 여기에 (Sentry 등)
    console.error('[GlobalError]', error)
  }, [error])

  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center gap-4 px-5 py-10 text-center">
      <span
        className="inline-flex h-16 w-16 items-center justify-center rounded-full bg-red-50 text-red-600"
        aria-hidden="true"
      >
        <AlertTriangle className="h-8 w-8" />
      </span>
      <h1 className="text-2xl font-bold text-gray-900">
        문제가 발생했어요
      </h1>
      <p className="max-w-xs text-base text-gray-600">
        잠시 후 다시 시도해주세요. 문제가 계속되면 앱을 다시 실행해보세요.
      </p>
      {error.digest && (
        <p className="text-xs text-gray-600">오류 코드: {error.digest}</p>
      )}
      <div className="mt-2 flex flex-col gap-2 sm:flex-row">
        <button
          type="button"
          onClick={() => reset()}
          className="inline-flex min-h-[48px] items-center justify-center gap-2 rounded-xl bg-blue-600 px-6 text-base font-semibold text-white hover:bg-blue-700"
        >
          <RotateCcw className="h-5 w-5" aria-hidden="true" />
          다시 시도
        </button>
        <Link
          href="/home"
          className="inline-flex min-h-[48px] items-center justify-center rounded-xl border border-gray-300 bg-white px-6 text-base font-semibold text-gray-700 hover:bg-gray-50"
        >
          홈으로 가기
        </Link>
      </div>
    </div>
  )
}
