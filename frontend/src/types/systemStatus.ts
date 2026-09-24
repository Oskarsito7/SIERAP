/** Estado general de la API (espejo de `SystemStatusResponse` del backend). */
export interface SystemStatus {
  application: string
  version: string
  profiles: string[]
  databaseAvailable: boolean
  timestamp: string
}
