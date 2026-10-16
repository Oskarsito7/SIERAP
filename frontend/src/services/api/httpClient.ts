import axios from 'axios'

import type { ApiError } from '@/types/api'
import type { TokenResponse } from '@/types/auth'

declare module 'axios' {
  interface AxiosRequestConfig {
    /** No intentar renovar la sesión si esta llamada responde 401 (login, refresh, logout). */
    skipAuthRefresh?: boolean
    /** Marca interna: la llamada ya se reintentó una vez tras renovar la sesión. */
    retried?: boolean
  }
}

/**
 * Cliente HTTP único de la aplicación: todas las llamadas al backend pasan por aquí.
 * Adjunta el token de acceso a cada petición y, si una llamada responde 401, renueva
 * la sesión una sola vez y la reintenta (contrato en `docs/07-contrato-api.md`).
 */
export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

type SessionListener = (session: TokenResponse | null) => void

/** El token de acceso vive solo en memoria; la cookie de renovación la maneja el navegador. */
let accessToken: string | null = null
let refreshInFlight: Promise<TokenResponse> | null = null
const sessionListeners = new Set<SessionListener>()

/**
 * Guarda la sesión recibida del backend (o la borra con `null`) y avisa a quienes
 * la escuchan, como el `AuthProvider`.
 */
export function setSession(session: TokenResponse | null): void {
  accessToken = session?.accessToken ?? null
  sessionListeners.forEach((listener) => listener(session))
}

/** Suscribe una función a los cambios de sesión. Devuelve la función para cancelar. */
export function onSessionChange(listener: SessionListener): () => void {
  sessionListeners.add(listener)
  return () => {
    sessionListeners.delete(listener)
  }
}

/**
 * Renueva la sesión con la cookie `sierap_refresh`. Solo hay un refresh a la vez:
 * cada cookie sirve una sola vez y presentar una ya usada cierra todas las sesiones
 * del usuario, así que las llamadas simultáneas (el interceptor del 401, la carga de
 * la app y el doble efecto de `StrictMode`) reúsan la misma promesa.
 */
export function refreshSession(): Promise<TokenResponse> {
  refreshInFlight ??= httpClient
    .post<TokenResponse>('/auth/refresh', undefined, { skipAuthRefresh: true })
    .then(({ data }) => {
      setSession(data)
      return data
    })
    .catch((error: unknown) => {
      setSession(null)
      throw error
    })
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

httpClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

httpClient.interceptors.response.use(undefined, async (error: unknown) => {
  const config = axios.isAxiosError(error) ? error.config : undefined
  const unauthorized = axios.isAxiosError(error) && error.response?.status === 401
  if (!unauthorized || !config || config.skipAuthRefresh || config.retried) {
    throw error
  }
  try {
    await refreshSession()
  } catch {
    // La sesión no se pudo renovar: se propaga el 401 original y el AuthProvider,
    // que ya recibió la sesión vacía, lleva al inicio de sesión.
    throw error
  }
  return httpClient.request({ ...config, retried: true })
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

/** Errores de validación por campo (respuesta 400); vacío si no aplica. */
export function getFieldErrors(error: unknown): Record<string, string> {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data?.fieldErrors) {
    return error.response.data.fieldErrors
  }
  return {}
}
