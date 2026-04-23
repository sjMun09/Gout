// 공용 타입 정의

// 퓨린 신호등 레벨
export type PurineLevel = 'low' | 'medium' | 'high'

// 음식 정보
export interface Food {
  id: string
  name: string
  purineLevel: PurineLevel
  purineMg?: number
  category?: string
}

// 요산 수치 기록
export interface UricAcidRecord {
  id: string
  value: number // mg/dL
  measuredAt: string // ISO date
  memo?: string
}

// 발작 일지
export interface AttackLog {
  id: string
  startedAt: string
  endedAt?: string
  severity: 1 | 2 | 3 | 4 | 5
  location: string
  memo?: string
}

// 복약 기록
export interface MedicationLog {
  id: string
  name: string
  dose: string
  takenAt: string
}

// 하단 네비 아이템
export interface NavItem {
  href: string
  label: string
}
