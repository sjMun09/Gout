import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: '커뮤니티',
  description:
    '같은 고민을 가진 사람들과 관리 경험, 식단, 운동 팁을 공유하세요.',
}

export default function CommunityLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return children
}
