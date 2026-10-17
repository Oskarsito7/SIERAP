import { AxiosError, type AxiosResponse } from 'axios'

import type { ApiError } from '@/types/api'

/** Error con la misma forma que los de la API, para que las pantallas muestren su mensaje. */
export function mockApiError(status: number, message: string): AxiosError<ApiError> {
  const data: ApiError = { timestamp: new Date().toISOString(), status, error: '', message, path: '', fieldErrors: {} }
  return new AxiosError(message, 'ERR_BAD_REQUEST', undefined, null, { status, data } as AxiosResponse<ApiError>)
}
