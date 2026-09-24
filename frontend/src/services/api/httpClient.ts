import axios from 'axios'

import type { ApiError } from '@/types/api'

/**
 * Cliente HTTP único de la aplicación: todas las llamadas al backend pasan por aquí.
 * En la Etapa 2 se le agrega un interceptor que adjunta el token JWT a cada petición.
 */
export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

/**
 * Extrae un mensaje legible de un error de la API. Si el backend respondió con su
 * formato uniforme de error se usa su `message`; si no hubo respuesta (backend
 * caído, sin red), se devuelve un mensaje genérico.
 */
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data?.message) {
    return error.response.data.message
  }
  return 'No fue posible comunicarse con el servidor'
}
