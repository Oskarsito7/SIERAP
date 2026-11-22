import type {
  ComparativeReportData,
  ComparativeReportQuery,
  GroupReportData,
  IndividualReportData,
  MomentReportData,
} from '@/types/report'

import { httpClient } from './httpClient'

/** Reportes de la Etapa 7 (`docs/07-contrato-api.md`, "Stage 7 — Reporting"). */
export const reportService = {
  /** Desempeño de un estudiante en un momento, en total y por RAP. */
  async individual(momentId: number, studentId: number): Promise<IndividualReportData> {
    const { data } = await httpClient.get<IndividualReportData>('/reports/individual', {
      params: { momentId, studentId },
    })
    return data
  },

  /** Desempeño de un grupo por RAP y la nota individual de cada integrante. */
  async group(groupId: number): Promise<GroupReportData> {
    const { data } = await httpClient.get<GroupReportData>(`/reports/groups/${groupId}`)
    return data
  },

  /** Promedios por RAP y distribución de notas de un momento. */
  async moment(momentId: number): Promise<MomentReportData> {
    const { data } = await httpClient.get<MomentReportData>(`/reports/moments/${momentId}`)
    return data
  },

  /** Un periodo frente a otro, para el mismo programa y número de momento; solo cifras agregadas. */
  async comparative(query: ComparativeReportQuery): Promise<ComparativeReportData> {
    const { data } = await httpClient.get<ComparativeReportData>('/reports/comparative', { params: query })
    return data
  },
}
