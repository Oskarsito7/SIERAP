/**
 * Porcentaje escrito por el admin, con coma o punto decimal ("33,33"). Devuelve `null`
 * si no es mayor que 0 y hasta 100, con hasta 2 decimales (docs/07, pesos del momento).
 */
export function parsePercent(text: string): number | null {
  const value = text.trim()
  if (!/^\d{1,3}([.,]\d{1,2})?$/.test(value)) return null
  const percent = Number(value.replace(',', '.'))
  return percent > 0 && percent <= 100 ? percent : null
}

/** Porcentaje para mostrar: sin decimales si es entero ("60"), si no con dos ("33,33"). */
export function formatPercent(percent: number): string {
  const rounded = Math.round(percent * 100) / 100
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(2).replace('.', ',')
}

/**
 * Ejemplo del reparto de ADR-018: si un grupo no tiene evaluación enviada de un tipo, su
 * peso se reparte entre los demás en proporción. Devuelve el peso nuevo de cada uno.
 */
export function redistribute(
  weights: { label: string; weight: number }[],
  missing: string,
): { label: string; before: number; after: number }[] {
  const missingWeight = weights.find((item) => item.label === missing)?.weight ?? 0
  const rest = weights.filter((item) => item.label !== missing)
  const restTotal = rest.reduce((sum, item) => sum + item.weight, 0)
  return rest.map((item) => ({
    label: item.label,
    before: item.weight,
    after: restTotal > 0 ? item.weight + (missingWeight * item.weight) / restTotal : item.weight,
  }))
}
