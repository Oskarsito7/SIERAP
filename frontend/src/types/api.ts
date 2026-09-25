/** Formato uniforme de error de la API (espejo de `ErrorResponse` del backend). */
export interface ApiError {
  timestamp: string
  status: number
  error: string
  /** Mensaje para el usuario final, en español. */
  message: string
  path: string
  /** Errores de validación por campo; vacío si no aplica. */
  fieldErrors: Record<string, string>
}
