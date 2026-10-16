import type { MomentDetail, MomentSummary } from '@/types/moment'

import { httpClient } from './httpClient'

/** Consulta de momentos (`/api/moments`, `docs/07-contrato-api.md`). */
export const momentService = {
  /**
   * Momentos de un periodo dentro del alcance del usuario. Sin `periodId`, los del
   * periodo en curso (lista vacía si no hay uno).
   */
  async list(periodId?: number): Promise<MomentSummary[]> {
    const { data } = await httpClient.get<MomentSummary[]>('/moments', {
      params: periodId === undefined ? undefined : { periodId },
    })
    return data
  },

  /** Momento con sus RAP, criterios y niveles. */
  async get(id: number): Promise<MomentDetail> {
    const { data } = await httpClient.get<MomentDetail>(`/moments/${id}`)
    return data
  },
}
