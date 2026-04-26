import { apiFetch } from './client'

// ===== 사용자 타입 =====

export type UserAgeGroup =
  | 'TWENTIES'
  | 'THIRTIES'
  | 'FORTIES'
  | 'FIFTIES'
  | 'SIXTIES'
  | 'SEVENTIES_PLUS'

export interface UserProfile {
  id?: string
  nickname?: string
  ageGroup?: UserAgeGroup
  goutDiagnosedAt?: string // ISO date (YYYY-MM-DD)
  targetUricAcid?: number // mg/dL
  email?: string
}

// ===== 사용자 API =====
// 주의: 백엔드 /api/users/me 는 프로필 로컬 캐시용 레거시 — 서버 미구현이어도 무방.
// Agent-H 가 실제 계정(/api/me) 엔드포인트를 추가했다.

export type UserRole = 'USER' | 'ADMIN'
export type UserGender = 'MALE' | 'FEMALE' | 'OTHER'

/** GET /api/me 응답 — 실제 계정 프로필 */
export interface AccountProfile {
  id: string
  email: string
  nickname: string
  role: UserRole
  birthYear?: number | null
  gender?: UserGender | null
  createdAt?: string | null
  consentSensitiveAt?: string | null
}

export interface EditProfilePayload {
  nickname?: string
  birthYear?: number | null
  gender?: UserGender | null
}

export interface ChangePasswordPayload {
  currentPassword: string
  newPassword: string
}

export const userApi = {
  // 레거시: 로컬 저장 프로필(/api/users/me) — 백엔드 미구현 시 호출측에서 폴백 처리.
  me: () => apiFetch<UserProfile>('/api/users/me'),
  updateMe: (body: Partial<UserProfile>) =>
    apiFetch<UserProfile>('/api/users/me', {
      method: 'PUT',
      body: JSON.stringify(body),
    }),

  // 실제 계정 엔드포인트 (/api/me) — Agent-H 신규.
  getAccount: () => apiFetch<AccountProfile>('/api/me'),
  updateProfile: (body: EditProfilePayload) =>
    apiFetch<AccountProfile>('/api/me', {
      method: 'PATCH',
      body: JSON.stringify(body),
    }),
  changePassword: (body: ChangePasswordPayload) =>
    apiFetch<void>('/api/me/password', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  consentSensitiveData: () =>
    apiFetch<AccountProfile>('/api/me/sensitive-consent', { method: 'POST' }),
  withdrawSensitiveDataConsent: () =>
    apiFetch<AccountProfile>('/api/me/sensitive-consent', { method: 'DELETE' }),
  withdraw: () => apiFetch<void>('/api/me', { method: 'DELETE' }),
}
