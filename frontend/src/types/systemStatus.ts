/** Overall API status (mirror of the backend `SystemStatusResponse`). */
export interface SystemStatus {
  application: string
  version: string
  profiles: string[]
  databaseAvailable: boolean
  timestamp: string
}
