import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: '내 정보',
  description: '닉네임, 연령대, 목표 요산수치 등 나의 정보를 관리해요.',
}

export default function ProfileLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return children
}
