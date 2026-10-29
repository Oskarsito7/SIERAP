import { describe, expect, it } from 'vitest'

import { formatDelta, formatGrade, formatPercentage } from './reportFormat'

describe('reportFormat', () => {
  it('writes grades and percentages with a decimal comma, or a dash without data', () => {
    expect(formatGrade(4.5)).toBe('4,50')
    expect(formatGrade(null)).toBe('—')
    expect(formatPercentage(78)).toBe('78 %')
    expect(formatPercentage(78.333)).toBe('78,33 %')
    expect(formatPercentage(null)).toBe('—')
  })

  it('writes the change between two periods with its sign', () => {
    expect(formatDelta(6)).toBe('+6')
    expect(formatDelta(-1.5)).toBe('−1,50')
    expect(formatDelta(0)).toBe('0')
    expect(formatDelta(null)).toBe('—')
  })
})
