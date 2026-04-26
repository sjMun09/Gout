export const URIC_ACID_POLICY = {
  unit: 'mg/dL',
  defaultTarget: 6,
  minValue: 0,
  minTarget: 1,
  maxValue: 20,
  step: 0.1,
  chartMaxFloor: 12,
  chartVisibleCount: 10,
} as const

export function roundUricAcidValue(value: number): number {
  return Math.round(value * 10) / 10
}

export function isValidUricAcidValue(value: number): boolean {
  return (
    Number.isFinite(value) &&
    value >= URIC_ACID_POLICY.minValue &&
    value <= URIC_ACID_POLICY.maxValue
  )
}

export function isValidUricAcidTarget(value: number): boolean {
  return (
    Number.isFinite(value) &&
    value >= URIC_ACID_POLICY.minTarget &&
    value <= URIC_ACID_POLICY.maxValue
  )
}

export function formatUricAcidValue(value: number): string {
  return value.toFixed(1)
}

export function getUricAcidStatus(
  value: number,
  target: number = URIC_ACID_POLICY.defaultTarget,
): 'target' | 'needs-care' {
  return value <= target ? 'target' : 'needs-care'
}
