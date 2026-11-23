/**
 * Página de una lista con paginación opcional del servidor (ADR-024): el cuerpo es la
 * página pedida y el total sin paginar viene en la cabecera `X-Total-Count`.
 */
export interface Page<T> {
  items: T[]
  total: number
}

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
