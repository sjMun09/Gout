// Keep keys in sync with gout-back Post.PostCategory.
export const POST_CATEGORIES = [
  { key: 'FREE', shortLabel: '자유', fullLabel: '자유' },
  { key: 'QUESTION', shortLabel: '질문', fullLabel: '질문' },
  { key: 'FOOD_EXPERIENCE', shortLabel: '식단', fullLabel: '식단 경험' },
  { key: 'EXERCISE', shortLabel: '운동', fullLabel: '운동' },
  { key: 'MEDICATION', shortLabel: '약물', fullLabel: '약물' },
  { key: 'SUCCESS_STORY', shortLabel: '성공담', fullLabel: '관리 성공담' },
  { key: 'HOSPITAL_REVIEW', shortLabel: '병원', fullLabel: '병원 경험' },
] as const

export type PostCategoryKey = (typeof POST_CATEGORIES)[number]['key']
export type PostCategoryFilterKey = 'ALL' | PostCategoryKey

export const POST_CATEGORY_LABELS: Record<PostCategoryKey, string> =
  Object.fromEntries(
    POST_CATEGORIES.map(({ key, fullLabel }) => [key, fullLabel]),
  ) as Record<PostCategoryKey, string>

export const CATEGORY_LABELS: Readonly<Record<string, string>> =
  POST_CATEGORY_LABELS

export const POST_CATEGORY_TABS: {
  key: PostCategoryFilterKey
  label: string
}[] = [
  { key: 'ALL', label: '전체' },
  ...POST_CATEGORIES.map(({ key, shortLabel }) => ({
    key,
    label: shortLabel,
  })),
]

export const POST_CATEGORY_SELECT_OPTIONS = POST_CATEGORIES.map(
  ({ key, fullLabel }) => ({
    key,
    label: fullLabel,
  }),
)
