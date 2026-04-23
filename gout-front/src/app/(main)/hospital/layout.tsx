import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: '병원 찾기',
  description:
    '내 주변 통풍·류마티스 진료 병원을 검색하고 거리·연락처를 확인하세요.',
}

export default function HospitalLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return children
}
