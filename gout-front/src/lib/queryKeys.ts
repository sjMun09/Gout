import type { PostSort } from '@/lib/api'
import type { PostCategoryFilterKey } from '@/constants'

export const queryKeys = {
  health: {
    uricAcidLogs: ['health', 'uric-acid-logs'] as const,
    medicationLogs: ['health', 'medication-logs'] as const,
  },
  community: {
    posts: (params: {
      category?: PostCategoryFilterKey | string
      keyword?: string
      sort?: PostSort
      tag?: string
      size?: number
    }) => ['community', 'posts', params] as const,
    latest: (size: number) => ['community', 'latest', size] as const,
    trending: (params: { days: number; limit: number }) =>
      ['community', 'trending', params] as const,
  },
  research: {
    papers: (params: { category?: string; size?: number }) =>
      ['research', 'papers', params] as const,
    similarPapers: (id: string, limit: number) =>
      ['research', 'similar-papers', id, limit] as const,
  },
  notifications: {
    list: (params: { page: number; size: number }) =>
      ['notifications', 'list', params] as const,
  },
}
