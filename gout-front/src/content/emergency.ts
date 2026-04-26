import type { GuidelineCategory } from '@/lib/api'

export interface EmergencyContentItem {
  title: string
  description?: string
}

export interface EmergencyContact {
  label: string
  phoneNumber: string
  href: `tel:${string}`
  variant: 'primary' | 'secondary'
}

export interface EmergencyContentResource {
  metadata: {
    version: string
    lastReviewedAt: string
    sourceSummary: string
    sourceReferences: string[]
  }
  header: {
    title: string
    description: string
  }
  sections: {
    doNow: {
      iconLabel: string
      title: string
      items: EmergencyContentItem[]
    }
    dontNow: {
      title: string
      items: EmergencyContentItem[]
    }
    hospitalAlert: {
      title: string
      items: EmergencyContentItem[]
      actionLabel: string
      actionHref: string
    }
    extraGuidelines: {
      apiCategory: GuidelineCategory
      title: string
    }
  }
  contacts: {
    title: string
    items: EmergencyContact[]
  }
}

export const emergencyContent: EmergencyContentResource = {
  metadata: {
    version: '2026-04-26',
    lastReviewedAt: '2026-04-26',
    sourceSummary:
      '응급 페이지의 고정 안내 문구. 기존 프론트엔드 하드코딩 문구를 보존해 구조화했으며, 추가 근거 기반 안내는 /api/guidelines?category=EMERGENCY에서 로드한다.',
    sourceReferences: [
      'gout-front/src/app/(main)/emergency/page.tsx 기존 문구',
      'gout-back/src/main/resources/db/migration/V11__seed_guidelines.sql',
      'gout-back/src/main/resources/db/migration/V14__seed_guidelines_expanded.sql',
    ],
  },
  header: {
    title: '통풍 발작 응급 가이드',
    description: '발작이 왔을 때 바로 따라하세요',
  },
  sections: {
    doNow: {
      iconLabel: '⚡',
      title: '즉시 할 일',
      items: [
        {
          title: '발작 부위 높이 올리기',
          description: '심장보다 높게 올리면 부기와 통증이 줄어듭니다.',
        },
        {
          title: '얼음 찜질',
          description: '수건으로 감싼 얼음팩을 20분간 대세요.',
        },
        {
          title: '물 충분히 마시기',
          description: '요산을 희석시켜 배출을 돕습니다.',
        },
        {
          title: '항염증 진통제 (NSAIDs)',
          description: '처방받은 경우에만 복용하세요.',
        },
      ],
    },
    dontNow: {
      title: '하지 말아야 할 것',
      items: [
        {
          title: '발작 부위 마사지 금지',
          description: '염증을 악화시킬 수 있어요.',
        },
        {
          title: '퓨린 높은 음식 섭취 금지',
          description: '내장, 붉은 고기, 등푸른 생선 등.',
        },
        {
          title: '음주 금지',
          description: '요산 수치를 급격히 높입니다.',
        },
      ],
    },
    hospitalAlert: {
      title: '병원 바로 가야 할 때',
      items: [
        { title: '고열(38도 이상) 동반' },
        { title: '여러 관절에서 동시 발작' },
        { title: '극심한 통증으로 움직임 불가' },
      ],
      actionLabel: '근처 병원 찾기',
      actionHref: '/hospital',
    },
    extraGuidelines: {
      apiCategory: 'EMERGENCY',
      title: '추가 안내',
    },
  },
  contacts: {
    title: '응급 연락처',
    items: [
      {
        label: '응급 전화',
        phoneNumber: '119',
        href: 'tel:119',
        variant: 'primary',
      },
      {
        label: '약사 상담',
        phoneNumber: '1644-2828',
        href: 'tel:16442828',
        variant: 'secondary',
      },
    ],
  },
}
