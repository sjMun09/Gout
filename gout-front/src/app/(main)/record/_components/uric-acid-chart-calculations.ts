import type { UricAcidLog } from '@/lib/api'
import { URIC_ACID_POLICY } from '@/lib/health-policy'

export type SegmentColor = 'blue' | 'red'

export interface ChartPoint {
  id: string
  value: number
  measuredAt: string
  x: number
  y: number
  overTarget: boolean
}

export interface ChartSegment {
  color: SegmentColor
  points: string
}

export interface UricAcidChartModel {
  data: UricAcidLog[]
  maxValue: number
  thresholdY: number
  points: ChartPoint[]
  segments: ChartSegment[]
}

const CHART = {
  width: 300,
  height: 120,
  padLeft: 24,
  padRight: 8,
  padTop: 10,
  padBottom: 20,
} as const

export const URIC_ACID_CHART = CHART

export function getVisibleUricAcidLogs(logs: UricAcidLog[]): UricAcidLog[] {
  return [...logs]
    .sort(
      (a, b) =>
        new Date(a.measuredAt).getTime() - new Date(b.measuredAt).getTime(),
    )
    .slice(-URIC_ACID_POLICY.chartVisibleCount)
}

export function calculateUricAcidChart(
  logs: UricAcidLog[],
  target: number,
): UricAcidChartModel {
  const data = getVisibleUricAcidLogs(logs)
  const plotW = CHART.width - CHART.padLeft - CHART.padRight
  const plotH = CHART.height - CHART.padTop - CHART.padBottom
  const minValue = URIC_ACID_POLICY.minValue
  const maxValue = Math.max(
    URIC_ACID_POLICY.chartMaxFloor,
    ...data.map((d) => d.value + 1),
    target + 1,
  )

  const x = (index: number) => {
    if (data.length === 1) return CHART.padLeft + plotW / 2
    return CHART.padLeft + (index / (data.length - 1)) * plotW
  }

  const y = (value: number) => {
    const ratio = (value - minValue) / (maxValue - minValue)
    return CHART.padTop + plotH - ratio * plotH
  }

  const points = data.map((d, index) => ({
    id: d.id,
    value: d.value,
    measuredAt: d.measuredAt,
    x: x(index),
    y: y(d.value),
    overTarget: d.value > target,
  }))

  const segments: ChartSegment[] = []
  for (let index = 0; index < points.length - 1; index += 1) {
    const a = points[index]
    const b = points[index + 1]
    segments.push({
      color: a.overTarget || b.overTarget ? 'red' : 'blue',
      points: `${a.x},${a.y} ${b.x},${b.y}`,
    })
  }

  return {
    data,
    maxValue,
    thresholdY: y(target),
    points,
    segments,
  }
}
