import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: '음식 검색',
  description:
    '통풍 환자를 위한 음식 퓨린 함량과 권장 여부를 확인하세요. 신호등으로 빠르게 판단할 수 있어요.',
}

export default function FoodLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return children
}
