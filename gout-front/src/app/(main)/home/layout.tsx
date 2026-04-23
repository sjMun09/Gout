import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: '홈',
  description:
    '요산수치·복약·커뮤니티 소식을 한 화면에서 확인하고 빠르게 기록하세요.',
}

export default function HomeLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return children
}
