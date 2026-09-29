/** Fecha de hoy en formato `YYYY-MM-DD`, en la zona horaria del navegador. */
export function todayIso(): string {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

/** Convierte una fecha `YYYY-MM-DD` de la API a `DD/MM/YYYY` para mostrarla. */
export function formatDate(isoDate: string): string {
  const [year, month, day] = isoDate.split('-')
  return `${day}/${month}/${year}`
}
