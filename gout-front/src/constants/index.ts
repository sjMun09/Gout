// 앱 전역 상수

export const APP_NAME = '통풍케어'
export const APP_NAME_EN = 'Gout Care'

// 퓨린 신호등 색상 (Tailwind 클래스 기준)
export const PURINE_COLORS = {
  low: 'bg-green-500 text-white', // 좋음
  medium: 'bg-yellow-400 text-gray-900', // 주의
  high: 'bg-red-500 text-white', // 피해야 함
} as const

export const PURINE_LABELS = {
  low: '좋음',
  medium: '주의',
  high: '피해야 함',
} as const

// 요산 정상 수치 (mg/dL)
export const URIC_ACID_NORMAL = {
  maleMax: 7.0,
  femaleMax: 6.0,
  goutTarget: 6.0,
}

// 하단 네비게이션 높이 (px)
export const BOTTOM_NAV_HEIGHT = 64
