export default function GlobalLoading() {
  return (
    <div
      className="flex min-h-[70vh] items-center justify-center px-5 py-10"
      role="status"
      aria-label="페이지 로딩 중"
    >
      <span className="inline-block h-10 w-10 animate-spin rounded-full border-4 border-blue-200 border-t-blue-600" />
      <span className="sr-only">로딩 중</span>
    </div>
  )
}
