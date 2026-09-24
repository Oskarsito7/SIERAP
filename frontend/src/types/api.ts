/** Uniform API error format (mirror of the backend `ErrorResponse`). */
export interface ApiError {
  timestamp: string
  status: number
  error: string
  /** Message for the end user, in Spanish. */
  message: string
  path: string
  /** Validation errors per field; empty when not applicable. */
  fieldErrors: Record<string, string>
}
