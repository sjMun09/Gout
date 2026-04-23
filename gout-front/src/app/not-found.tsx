import Link from 'next/link'
import { MapPinOff } from 'lucide-react'

export const metadata = {
  title: '페이지를 찾을 수 없어요',
}

export default function NotFound() {
  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center gap-4 px-5 py-10 text-center">
      <span
        className="inline-flex h-16 w-16 items-center justify-center rounded-full bg-gray-100 text-gray-500"
        aria-hidden="true"
      >
        <MapPinOff className="h-8 w-8" />
      </span>
      <h1 className="text-2xl font-bold text-gray-900">
        페이지를 찾을 수 없어요
      </h1>
      <p className="max-w-xs text-base text-gray-600">
        주소가 잘못됐거나, 페이지가 이동·삭제됐을 수 있어요.
      </p>
      <Link
        href="/home"
        className="mt-2 inline-flex min-h-[48px] items-center justify-center rounded-xl bg-blue-600 px-6 text-base font-semibold text-white hover:bg-blue-700"
      >
        홈으로 가기
      </Link>
    </div>
  )
}
