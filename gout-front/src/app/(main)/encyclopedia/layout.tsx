import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: '통풍 백과',
  description:
    '통풍의 원인, 증상, 관리 원칙과 식사 지침 등 근거 기반 정보를 정리했어요.',
}

export default function EncyclopediaLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return children
}
