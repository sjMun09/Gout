import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: '건강 기록',
  description: '요산수치·발작·복약을 기록하고 추이를 확인하세요.',
}

export default function RecordLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return children
}
