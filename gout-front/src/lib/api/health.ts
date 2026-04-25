import { apiFetch } from './client'

// ===== 건강 기록 타입 =====

export interface UricAcidLog {
  id: string
  value: number
  measuredAt: string
  memo?: string
  createdAt: string
}

export interface GoutAttackLog {
  id: string
  attackedAt: string
  painLevel?: number
  location?: string
  durationDays?: number
  suspectedCause?: string
  memo?: string
  createdAt: string
}

export interface MedicationLog {
  id: string
  medicationName: string
  dosage?: string
  takenAt: string
  createdAt: string
}

// ===== 건강 기록 API =====

// ===== 건강 기록 API =====

export const healthApi = {
  getUricAcidLogs: () =>
    apiFetch<UricAcidLog[]>('/api/health/uric-acid-logs'),
  createUricAcidLog: (body: {
    value: number
    measuredAt: string
    memo?: string
  }) =>
    apiFetch<UricAcidLog>('/api/health/uric-acid-logs', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  deleteUricAcidLog: (id: string) =>
    apiFetch<void>(`/api/health/uric-acid-logs/${id}`, { method: 'DELETE' }),

  getGoutAttackLogs: () =>
    apiFetch<GoutAttackLog[]>('/api/health/gout-attack-logs'),
  createGoutAttackLog: (
    body: Partial<GoutAttackLog> & { attackedAt: string },
  ) =>
    apiFetch<GoutAttackLog>('/api/health/gout-attack-logs', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  deleteGoutAttackLog: (id: string) =>
    apiFetch<void>(`/api/health/gout-attack-logs/${id}`, { method: 'DELETE' }),

  getMedicationLogs: () =>
    apiFetch<MedicationLog[]>('/api/health/medication-logs'),
  createMedicationLog: (body: {
    medicationName: string
    dosage?: string
    takenAt: string
  }) =>
    apiFetch<MedicationLog>('/api/health/medication-logs', {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  deleteMedicationLog: (id: string) =>
    apiFetch<void>(`/api/health/medication-logs/${id}`, { method: 'DELETE' }),
}
