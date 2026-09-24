import type { SystemStatus } from '@/types/systemStatus'

import { httpClient } from './httpClient'

/** Queries the overall backend status (`GET /api/status`). */
export const systemStatusService = {
  async getStatus(): Promise<SystemStatus> {
    const { data } = await httpClient.get<SystemStatus>('/status')
    return data
  },
}
