import type { SystemStatus } from '@/types/systemStatus'

import { httpClient } from './httpClient'

/** Consultas del estado general del backend (`GET /api/status`). */
export const systemStatusService = {
  async getStatus(): Promise<SystemStatus> {
    const { data } = await httpClient.get<SystemStatus>('/status')
    return data
  },
}
