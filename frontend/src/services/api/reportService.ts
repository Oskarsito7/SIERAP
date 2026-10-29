import type {
  ComparativeReportData,
  ComparativeReportQuery,
  GroupReportData,
  IndividualReportData,
  MomentReportData,
} from '@/types/report'

import { httpClient } from './httpClient'
import {
  sampleComparativeReport,
  sampleGroupReport,
  sampleIndividualReport,
  sampleMomentReport,
} from './reportsMock'

/**
 * `true` cuando D2 publique las rutas de reportes (docs/06, Etapa 7). Mientras sea
 * `false`, la pantalla muestra datos de ejemplo con la forma del contrato y lo avisa.
 */
export const REPORTS_API_READY = false

/** Reportes de la Etapa 7 (`docs/07-contrato-api.md`, "Stage 7 — Reporting"). */
export const reportService = {
  /** Desempeño de un estudiante en un momento, en total y por RAP. */
  async individual(momentId: number, studentId: number): Promise<IndividualReportData> {
    if (!REPORTS_API_READY) return sampleIndividualReport(momentId, studentId)
    const { data } = await httpClient.get<IndividualReportData>('/reports/individual', {
      params: { momentId, studentId },
    })
    return data
  },

  /** Desempeño de un grupo por RAP y la nota individual de cada integrante. */
  async group(groupId: number): Promise<GroupReportData> {
    if (!REPORTS_API_READY) return sampleGroupReport(groupId)
    const { data } = await httpClient.get<GroupReportData>(`/reports/groups/${groupId}`)
    return data
  },

  /** Promedios por RAP y distribución de notas de un momento. */
  async moment(momentId: number): Promise<MomentReportData> {
    if (!REPORTS_API_READY) return sampleMomentReport(momentId)
    const { data } = await httpClient.get<MomentReportData>(`/reports/moments/${momentId}`)
    return data
  },

  /** Un periodo frente a otro, para el mismo programa y número de momento; solo cifras agregadas. */
  async comparative(query: ComparativeReportQuery): Promise<ComparativeReportData> {
    if (!REPORTS_API_READY) return sampleComparativeReport(query)
    const { data } = await httpClient.get<ComparativeReportData>('/reports/comparative', { params: query })
    return data
  },
}
