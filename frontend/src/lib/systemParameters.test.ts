import { describe, expect, it } from 'vitest'

import { SYSTEM_PARAMETERS } from './systemParameters'

const validate = (key: string, value: string) => SYSTEM_PARAMETERS[key].validate(value)

describe('SYSTEM_PARAMETERS', () => {
  it('accepts a passing percentage between 0 and 100', () => {
    expect(validate('report.passing-percentage', '60')).toBeNull()
    expect(validate('report.passing-percentage', ' 62.5 ')).toBeNull()
    expect(validate('report.passing-percentage', '120')).toBe('El porcentaje para aprobar debe estar entre 0 y 100.')
    expect(validate('report.passing-percentage', '60,5')).toMatch(/debe ser un número/)
  })

  it('checks every band of the grade scale', () => {
    expect(validate('report.grade-bands', 'Bajo:0;Básico:60;Alto:76;Sobresaliente:90;')).toBeNull()
    expect(validate('report.grade-bands', ';;')).toMatch(/al menos un nivel/)
    expect(validate('report.grade-bands', 'Bajo:0;Alto')).toBe('"Alto" no tiene la forma Nombre:porcentaje.')
    expect(validate('report.grade-bands', ':10')).toBe('":10" no tiene la forma Nombre:porcentaje.')
    expect(validate('report.grade-bands', 'Bajo:0;Alto:150')).toBe('El porcentaje de "Alto" debe estar entre 0 y 100.')
  })

  it('requires a positive whole number of versions', () => {
    expect(validate('submission.max-versions', '20')).toBeNull()
    expect(validate('submission.max-versions', '0')).toMatch(/mayor que cero/)
    expect(validate('submission.max-versions', '2.5')).toMatch(/entero/)
  })
})
