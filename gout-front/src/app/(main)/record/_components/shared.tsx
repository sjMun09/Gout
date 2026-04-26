export function HeaderBlock() {
  return (
    <header>
      <h1 className="text-2xl font-bold text-gray-900">건강 기록</h1>
      <p className="mt-1 text-base text-gray-600">
        꾸준한 기록이 건강 관리의 시작입니다
      </p>
    </header>
  )
}

export function Skeleton() {
  return (
    <div className="flex flex-col gap-3">
      <div className="h-32 animate-pulse rounded-2xl bg-gray-100" />
      <div className="h-20 animate-pulse rounded-2xl bg-gray-100" />
      <div className="h-20 animate-pulse rounded-2xl bg-gray-100" />
    </div>
  )
}

export function ErrorBanner({ message }: { message: string }) {
  return (
    <div className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-700">
      {message}
    </div>
  )
}

export function parseError(e: unknown): string {
  const msg = e instanceof Error ? e.message : String(e)
  if (msg.includes('401')) return '세션이 만료됐어요, 다시 로그인해주세요'
  return '요청 처리 중 오류가 발생했어요'
}
