/** Formato uniforme de error de la API (espejo de `ErrorResponse` del backend). */
export interface ErrorApi {
  fechaHora: string
  estado: number
  error: string
  mensaje: string
  ruta: string
  /** Errores de validación por campo; vacío si no aplica. */
  errores: Record<string, string>
}
