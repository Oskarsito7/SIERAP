import { describe, expect, it } from 'vitest'

import { formatSize, presentationError, zipError } from './files'

const MB = 1024 * 1024

/** Archivo de prueba con el tamaño indicado, sin reservar esa memoria. */
function file(name: string, size: number): File {
  const created = new File(['x'], name)
  Object.defineProperty(created, 'size', { value: size })
  return created
}

describe('submission files', () => {
  it('accepts a .zip up to 100 MB and rejects the rest', () => {
    expect(zipError(file('proyecto.ZIP', 100 * MB))).toBeNull()
    expect(zipError(null)).toBe('Adjunta el archivo .zip con el proyecto.')
    expect(zipError(file('proyecto.rar', MB))).toBe('El proyecto debe ser un archivo .zip.')
    expect(zipError(file('proyecto.zip', 0))).toBe('El archivo .zip está vacío.')
    expect(zipError(file('proyecto.zip', 100 * MB + 1))).toBe('El .zip pesa 100 MB: el máximo es 100 MB.')
    expect(zipError(file('proyecto.zip', 250 * MB))).toBe('El .zip pesa 250 MB: el máximo es 100 MB.')
  })

  it('accepts an optional .pdf, .docx or .pptx presentation up to 30 MB', () => {
    expect(presentationError(null)).toBeNull()
    expect(presentationError(file('sustentacion.pdf', 30 * MB))).toBeNull()
    expect(presentationError(file('sustentacion.pptx', MB))).toBeNull()
    expect(presentationError(file('notas.txt', MB))).toBe('La presentación debe ser un archivo .pdf, .docx o .pptx.')
    expect(presentationError(file('sustentacion.pdf', 31 * MB))).toBe(
      'La presentación pesa 31 MB: el máximo es 30 MB.',
    )
  })

  it('formats sizes for people', () => {
    expect(formatSize(41 * MB)).toBe('41 MB')
    expect(formatSize(1.5 * MB)).toBe('1,5 MB')
    expect(formatSize(820 * 1024)).toBe('820 KB')
    expect(formatSize(10)).toBe('1 KB')
  })
})
