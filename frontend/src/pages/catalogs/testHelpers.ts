import { AxiosError, type AxiosResponse } from 'axios'

import type { ApiError } from '@/types/api'

/** Error de la API como lo devuelve axios, para probar los mensajes que muestran las pantallas. */
export function apiError(status: number, message: string): AxiosError {
  const data: ApiError = { timestamp: '', status, error: '', message, path: '', fieldErrors: {} }
  return new AxiosError(message, 'ERR_BAD_REQUEST', undefined, null, { status, data } as AxiosResponse)
}
