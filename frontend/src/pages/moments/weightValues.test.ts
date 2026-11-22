import { describe, expect, it } from 'vitest'

import { formatPercent, parsePercent, redistribute } from './weightValues'

describe('parsePercent', () => {
  it('accepts percentages greater than 0 and up to 100 with up to 2 decimals', () => {
    expect(parsePercent('60')).toBe(60)
    expect(parsePercent('33,33')).toBe(33.33)
    expect(parsePercent('100')).toBe(100)
  })

  it('rejects 0, more than 100 and too precise values', () => {
    expect(parsePercent('0')).toBeNull()
    expect(parsePercent('100,5')).toBeNull()
    expect(parsePercent('10,555')).toBeNull()
    expect(parsePercent('')).toBeNull()
  })
})

describe('formatPercent', () => {
  it('drops the decimals of whole percentages', () => {
    expect(formatPercent(75)).toBe('75')
    expect(formatPercent(33.333)).toBe('33,33')
  })
})

describe('redistribute', () => {
  it('spreads the weight of the missing kind among the others in proportion', () => {
    const weights = [
      { label: 'Docente', weight: 60 },
      { label: 'Empresario', weight: 20 },
      { label: 'Egresado', weight: 20 },
    ]
    expect(redistribute(weights, 'Egresado')).toEqual([
      { label: 'Docente', before: 60, after: 75 },
      { label: 'Empresario', before: 20, after: 25 },
    ])
  })
})
