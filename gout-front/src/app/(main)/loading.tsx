export default function MainLoading() {
  return (
    <div className="flex flex-col gap-4 px-5 py-6">
      <div className="h-8 w-40 animate-pulse rounded bg-gray-200" />
      <div className="h-4 w-56 animate-pulse rounded bg-gray-100" />
      <div className="mt-4 grid grid-cols-2 gap-3">
        <div className="h-28 animate-pulse rounded-2xl bg-gray-100" />
        <div className="h-28 animate-pulse rounded-2xl bg-gray-100" />
        <div className="h-28 animate-pulse rounded-2xl bg-gray-100" />
        <div className="h-28 animate-pulse rounded-2xl bg-gray-100" />
      </div>
      <div className="mt-4 h-20 animate-pulse rounded-2xl bg-gray-100" />
      <span className="sr-only" role="status">
        로딩 중
      </span>
    </div>
  )
}
