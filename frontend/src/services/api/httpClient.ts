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

/** Archivo descargado de la API con el nombre que propone el servidor. */
export interface DownloadedFile {
  blob: Blob
  fileName: string
}

/**
 * Nombre del archivo según la cabecera `Content-Disposition` (prefiere `filename*`, que
 * admite tildes); si no viene, usa `fallback`.
 */
export function fileNameFrom(disposition: string | undefined, fallback: string): string {
  if (!disposition) return fallback
  const encoded = /filename\*=UTF-8''([^;]+)/i.exec(disposition)
  if (encoded) {
    try {
      return decodeURIComponent(encoded[1].trim())
    } catch {
      // Si viene mal codificado, se intenta con `filename`.
    }
  }
  const plain = /filename="?([^";]+)"?/i.exec(disposition)
  return plain ? plain[1].trim() : fallback
}

/**
 * Mensaje de error de una descarga. Con `responseType: 'blob'` el cuerpo del error
 * también llega como `Blob`, así que hay que leerlo para mostrar el `message` de la API
 * (p. ej. "Los resultados de este momento aún no están disponibles.").
 */
export async function getDownloadErrorMessage(error: unknown): Promise<string> {
  if (axios.isAxiosError(error) && error.response?.data instanceof Blob) {
    try {
      const body = JSON.parse(await error.response.data.text()) as Partial<ApiError>
      if (body.message) return body.message
    } catch {
      // El cuerpo no era JSON: se usa el mensaje genérico.
    }
  }
  return getErrorMessage(error)
}

/** Indica si el error viene de una petición cancelada a propósito (`AbortController`). */
export function isCanceled(error: unknown): boolean {
  return axios.isCancel(error)
}

/** Errores de validación por campo (respuesta 400); vacío si no aplica. */
export function getFieldErrors(error: unknown): Record<string, string> {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data?.fieldErrors) {
    return error.response.data.fieldErrors
  }
  return {}
}
