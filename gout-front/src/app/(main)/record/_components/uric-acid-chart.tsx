'use client'

import { useMemo } from 'react'
import type { UricAcidLog } from '@/lib/api'
import { formatUricAcidValue } from '@/lib/health-policy'
import {
  URIC_ACID_CHART,
  calculateUricAcidChart,
} from './uric-acid-chart-calculations'

export function UricAcidChart({
  logs,
  target,
}: {
  logs: UricAcidLog[]
  target: number
}) {
  const model = useMemo(
    () => calculateUricAcidChart(logs, target),
    [logs, target],
  )
  const { width, height, padLeft, padTop, padBottom } = URIC_ACID_CHART
  const plotW = width - padLeft - URIC_ACID_CHART.padRight
  const plotH = height - padTop - padBottom

  if (model.data.length === 0) {
    return (
      <div className="flex h-[120px] items-center justify-center text-sm text-gray-500">
        아직 기록이 없어요
      </div>
    )
  }

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      className="h-auto w-full"
      role="img"
      aria-label="요산수치 추이 그래프"
    >
      <line
        x1={padLeft}
        y1={padTop}
        x2={padLeft}
        y2={padTop + plotH}
        stroke="#e5e7eb"
        strokeWidth="1"
      />
      <line
        x1={padLeft}
        y1={padTop + plotH}
        x2={padLeft + plotW}
        y2={padTop + plotH}
        stroke="#e5e7eb"
        strokeWidth="1"
      />

      <line
        x1={padLeft}
        y1={model.thresholdY}
        x2={padLeft + plotW}
        y2={model.thresholdY}
        stroke="#f59e0b"
        strokeWidth="1"
        strokeDasharray="4 3"
      />
      <text x={padLeft + 2} y={model.thresholdY - 3} fontSize="9" fill="#f59e0b">
        {formatUricAcidValue(target)}
      </text>

      <text x="2" y={padTop + 4} fontSize="9" fill="#6b7280">
        {model.maxValue.toFixed(0)}
      </text>
      <text x="2" y={padTop + plotH + 4} fontSize="9" fill="#6b7280">
        0
      </text>

      {model.segments.map((seg, index) => (
        <polyline
          key={index}
          points={seg.points}
          fill="none"
          stroke={seg.color === 'blue' ? '#2563eb' : '#dc2626'}
          strokeWidth="2"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      ))}

      {model.points.map((point) => (
        <g key={point.id}>
          <circle
            cx={point.x}
            cy={point.y}
            r="3"
            fill={point.overTarget ? '#dc2626' : '#2563eb'}
          />
        </g>
      ))}

      <text
        x={model.points[0].x}
        y={height - 4}
        fontSize="9"
        fill="#6b7280"
        textAnchor="start"
      >
        {model.data[0].measuredAt.slice(5)}
      </text>
      {model.points.length > 1 && (
        <text
          x={model.points[model.points.length - 1].x}
          y={height - 4}
          fontSize="9"
          fill="#6b7280"
          textAnchor="end"
        >
          {model.data[model.data.length - 1].measuredAt.slice(5)}
        </text>
      )}
    </svg>
  )
}
