import axios from 'axios'

import type { ApiError } from '@/types/api'

/**
 * Single HTTP client of the application: every call to the backend goes through here.
 * Stage 2 adds an interceptor that attaches the JWT token to each request.
 */
export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15_000,
})

/**
 * Extracts a user-facing message from an API error. If the backend answered with
 * its uniform error format, its `message` is used; if there was no response
 * (backend down, no network), a generic message is returned.
 */
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiError>(error) && error.response?.data?.message) {
    return error.response.data.message
  }
  return 'No fue posible comunicarse con el servidor'
}
