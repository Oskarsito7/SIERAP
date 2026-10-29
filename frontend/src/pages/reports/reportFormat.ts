import { formatScore } from '@/lib/scores'

/** Nota con dos decimales y coma decimal, o `—` cuando aún no hay calificación. */
export function formatNullableScore(score: number | null | undefined): string {
  if (score === null || score === undefined) return '—'
  return formatScore(score)
}

/** Porcentaje con dos decimales y coma decimal (p. ej. `88,00 %`), o `—` si es `null`. */
export function formatPercentage(percentage: number | null | undefined): string {
  if (percentage === null || percentage === undefined) return '—'
  return `${formatScore(percentage)} %`
}

/**
 * Variación de nota entre dos periodos con signo explícito (p. ej. `+0,34` o `−0,81`),
 * usando el signo menos tipográfico `−` como en el wireframe `AD-08`.
 */
export function formatScoreDelta(delta: number | null | undefined): string {
  if (delta === null || delta === undefined) return '—'
  if (delta > 0) return `+${formatScore(delta)}`
  if (delta < 0) return `−${formatScore(Math.abs(delta))}`
  return formatScore(0)
}

/**
 * Variación en puntos porcentuales entre dos periodos (p. ej. `+6,66 pp` o `−1,00 pp`),
 * o `—` si el RAP solo existe o fue evaluado en uno de los periodos.
 */
export function formatPercentageDelta(delta: number | null | undefined): string {
  if (delta === null || delta === undefined) return '—'
  if (delta > 0) return `+${formatScore(delta)} pp`
  if (delta < 0) return `−${formatScore(Math.abs(delta))}`
  return '0,00 pp'
}

/** Acota un porcentaje al intervalo `[0, 100]` para el ancho o alto visual de barras. */
export function clampPercentage(value: number | null | undefined): number {
  if (value === null || value === undefined || Number.isNaN(value)) return 0
  return Math.max(0, Math.min(100, value))
}
