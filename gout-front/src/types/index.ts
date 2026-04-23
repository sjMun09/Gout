// 공용 타입 정의

// 퓨린 신호등 레벨 (프론트 표시용)
export type PurineLevel = 'low' | 'medium' | 'high'

// 음식 정보 (레거시)
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

// 페이지네이션 응답 (Spring Data Page 포맷)
export interface PagedResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}

// 병원
export interface Hospital {
  id: string
  name: string
  address?: string
  phone?: string
  departments?: string[]
  latitude?: number
  longitude?: number
  distanceMeters?: number
}

// 백엔드 API 퓨린 레벨
export type ApiPurineLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH'
export type ApiRecommendation = 'GOOD' | 'MODERATE' | 'BAD' | 'AVOID'

export interface FoodItem {
  id: string
  name: string
  nameEn?: string
  category?: string
  purineContent?: number
  purineLevel: ApiPurineLevel
  recommendation: ApiRecommendation
  description?: string
  caution?: string
}

export interface FoodDetail extends FoodItem {
  evidenceNotes?: string
}
