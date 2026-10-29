/**
 * Puntaje de un nivel escrito por el admin, con coma o punto decimal ("0,48" o "0.48").
 * Devuelve `null` si no es un número desde 0 con hasta 2 decimales (ADR-013).
 */
export function parseScore(text: string): number | null {
  const value = text.trim()
  if (!/^\d+([.,]\d{1,2})?$/.test(value)) return null
  return Number(value.replace(',', '.'))
}
