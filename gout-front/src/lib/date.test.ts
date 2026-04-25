import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { formatDateKr, formatDateTimeKr, nowLocalDatetime, todayYmd } from './date'

beforeEach(() => {
  vi.useFakeTimers()
})

afterEach(() => {
  vi.useRealTimers()
})

describe('todayYmd', () => {
  it('returns YYYY-MM-DD for the current local date', () => {
    vi.setSystemTime(new Date(2026, 0, 5, 9, 30))
    expect(todayYmd()).toBe('2026-01-05')
  })

  it('zero-pads single-digit month and day', () => {
    vi.setSystemTime(new Date(2026, 8, 1, 0, 0))
    expect(todayYmd()).toBe('2026-09-01')
  })
})

describe('nowLocalDatetime', () => {
  it('returns YYYY-MM-DDTHH:mm for the current local time', () => {
    vi.setSystemTime(new Date(2026, 3, 25, 14, 49))
    expect(nowLocalDatetime()).toBe('2026-04-25T14:49')
  })

  it('zero-pads single-digit hour and minute', () => {
    vi.setSystemTime(new Date(2026, 0, 1, 3, 7))
    expect(nowLocalDatetime()).toBe('2026-01-01T03:07')
  })
})

describe('formatDateKr', () => {
  it('formats YYYY-MM-DD to YYYY.MM.DD', () => {
    expect(formatDateKr('2026-01-05')).toBe('2026.01.05')
  })

  it('returns the input verbatim when the date is invalid', () => {
    expect(formatDateKr('not-a-date')).toBe('not-a-date')
  })

  it('returns the input verbatim for empty string', () => {
    expect(formatDateKr('')).toBe('')
  })
})

describe('formatDateTimeKr', () => {
  it('formats ISO datetime to YYYY.MM.DD HH:mm in local TZ', () => {
    const iso = new Date(2026, 3, 25, 9, 5).toISOString()
    expect(formatDateTimeKr(iso)).toBe('2026.04.25 09:05')
  })

  it('returns the input verbatim when invalid', () => {
    expect(formatDateTimeKr('garbage')).toBe('garbage')
  })
})
