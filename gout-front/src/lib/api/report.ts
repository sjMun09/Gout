import { apiFetch } from './client'

// ===== 신고 타입 =====

export type ReportTargetType = 'POST' | 'COMMENT'
export type ReportReason = 'SPAM' | 'ABUSE' | 'SEXUAL' | 'MISINFO' | 'ETC'

export const REPORT_REASON_LABELS: Record<ReportReason, string> = {
  SPAM: '스팸/광고',
  ABUSE: '욕설/비방',
  SEXUAL: '음란성',
  MISINFO: '허위 정보',
  ETC: '기타',
}

export interface ReportResponse {
  id: string
  targetType: ReportTargetType
  targetId: string
  reporterId: string
  reason: ReportReason
  detail?: string
  status: 'PENDING' | 'RESOLVED' | 'DISMISSED'
  createdAt: string
  resolvedAt?: string
}

// ===== 신고 API =====

export const reportApi = {
  create: (
    targetType: ReportTargetType,
    targetId: string,
    reason: ReportReason,
    detail?: string,
  ) =>
    apiFetch<ReportResponse>('/api/reports', {
      method: 'POST',
      body: JSON.stringify({ targetType, targetId, reason, detail }),
    }),
}
