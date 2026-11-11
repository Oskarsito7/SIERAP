import { formatScore } from '@/lib/scores'

/** Nota con coma decimal, o "—" si todavía no hay. */
export function formatGrade(value: number | null): string {
  return value === null ? '—' : formatScore(value)
}

/** Porcentaje con coma decimal y sin ",00" si es entero (p. ej. "78 %" o "78,33 %"). */
export function formatPercentage(value: number | null): string {
  return value === null ? '—' : `${decimal(value)} %`
}

/** Cambio entre dos periodos con su signo (p. ej. "+6" o "−1,5"); "—" si falta un dato. */
export function formatDelta(value: number | null): string {
  if (value === null) return '—'
  if (value === 0) return '0'
  return `${value > 0 ? '+' : '−'}${decimal(Math.abs(value))}`
}

function decimal(value: number): string {
  const rounded = Math.round(value * 100) / 100
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(2).replace('.', ',')
}
