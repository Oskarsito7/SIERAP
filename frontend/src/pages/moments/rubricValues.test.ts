import { describe, expect, it } from 'vitest'

import { parseScore } from './rubricValues'

describe('parseScore', () => {
  it('accepts a decimal comma or point with up to 2 decimals', () => {
    expect(parseScore('0,48')).toBe(0.48)
    expect(parseScore(' 0.6 ')).toBe(0.6)
    expect(parseScore('1')).toBe(1)
    expect(parseScore('0')).toBe(0)
  })

  it('rejects negative, empty or too precise scores', () => {
    expect(parseScore('-1')).toBeNull()
    expect(parseScore('')).toBeNull()
    expect(parseScore('0,485')).toBeNull()
    expect(parseScore('alto')).toBeNull()
  })
})
