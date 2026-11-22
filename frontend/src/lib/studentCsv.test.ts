import { describe, expect, it } from 'vitest'

import { STUDENT_CSV_HEADER, studentCsvError, studentCsvTemplate } from './studentCsv'

describe('studentCsvTemplate', () => {
  it('has the exact header the backend requires and an example row', () => {
    const lines = studentCsvTemplate().replace(String.fromCharCode(0xfeff), '').trim().split('\r\n')

    expect(lines).toHaveLength(2)
    expect(lines[0]).toBe('nombres,apellidos,tipo_documento,numero_documento,correo,codigo_estudiantil,semestre')
    expect(lines[0].split(',')).toEqual(STUDENT_CSV_HEADER)
    expect(lines[1].split(',')).toHaveLength(STUDENT_CSV_HEADER.length)
  })

  it('starts with a BOM so Excel reads the accents', () => {
    expect(studentCsvTemplate().charCodeAt(0)).toBe(0xfeff)
  })
})

describe('studentCsvError', () => {
  const file = (name: string, content = 'x') => new File([content], name)

  it('accepts a non-empty .csv', () => {
    expect(studentCsvError(file('estudiantes.CSV'))).toBeNull()
  })

  it('rejects a missing, misnamed, empty or too large file', () => {
    expect(studentCsvError(null)).toBe('Elige el archivo CSV con los estudiantes.')
    expect(studentCsvError(file('estudiantes.xlsx'))).toBe('El archivo debe ser un .csv.')
    expect(studentCsvError(file('estudiantes.csv', ''))).toBe('El archivo está vacío.')
    expect(studentCsvError(file('estudiantes.csv', 'x'.repeat(1024 * 1024 + 1)))).toBe(
      'El archivo no puede pesar más de 1 MB.',
    )
  })
})
