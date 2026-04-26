import { describe, expect, it } from 'vitest'
import type { UricAcidLog } from '@/lib/api'
import {
  URIC_ACID_CHART,
  calculateUricAcidChart,
} from './uric-acid-chart-calculations'

function log(id: string, value: number, measuredAt: string): UricAcidLog {
  return {
    id,
    value,
    measuredAt,
    createdAt: measuredAt,
  }
}

describe('calculateUricAcidChart', () => {
  it('returns an empty model for empty logs', () => {
    const model = calculateUricAcidChart([], 6)

    expect(model.data).toEqual([])
    expect(model.points).toEqual([])
    expect(model.segments).toEqual([])
    expect(model.maxValue).toBe(12)
  })

  it('centers a single point and creates no segments', () => {
    const model = calculateUricAcidChart([log('a', 5.5, '2026-04-01')], 6)
    const plotW =
      URIC_ACID_CHART.width -
      URIC_ACID_CHART.padLeft -
      URIC_ACID_CHART.padRight

    expect(model.points).toHaveLength(1)
    expect(model.points[0].x).toBe(URIC_ACID_CHART.padLeft + plotW / 2)
    expect(model.points[0].overTarget).toBe(false)
    expect(model.segments).toEqual([])
  })

  it('colors threshold-crossing segments red and target segments blue', () => {
    const model = calculateUricAcidChart(
      [
        log('a', 5.8, '2026-04-01'),
        log('b', 6, '2026-04-02'),
        log('c', 6.4, '2026-04-03'),
        log('d', 5.9, '2026-04-04'),
      ],
      6,
    )

    expect(model.segments.map((segment) => segment.color)).toEqual([
      'blue',
      'red',
      'red',
    ])
    expect(model.points.map((point) => point.overTarget)).toEqual([
      false,
      false,
      true,
      false,
    ])
  })
})
