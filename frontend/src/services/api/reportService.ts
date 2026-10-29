import type {
  ComparativeReportData,
  ComparativeReportParams,
  GroupReportData,
  IndividualReportData,
  MomentReportData,
} from '@/types/report'

import { httpClient } from './httpClient'

/**
 * Servicio de reportes académicos (`/api/reports`, `docs/07-contrato-api.md` sección
 * "Stage 7 — Reporting"). Consulta los cuatro reportes en pantalla generados de forma
 * polimórfica por el modelo de dominio.
 */
export const reportService = {
  /**
   * 1. Reporte individual del desempeño de un estudiante por RAP en un momento.
   * Para un estudiante `studentId` es opcional (consulta el suyo propio).
   */
  async individual(momentId: number, studentId?: number): Promise<IndividualReportData> {
    const { data } = await httpClient.get<IndividualReportData>('/reports/individual', {
      params: studentId === undefined ? { momentId } : { momentId, studentId },
    })
    return data
  },

  /** 2. Reporte de un grupo de proyecto de aula con notas individuales y ajustes. */
  async group(groupId: number): Promise<GroupReportData> {
    const { data } = await httpClient.get<GroupReportData>(`/reports/groups/${groupId}`)
    return data
  },

  /** 3. Reporte consolidado por momento y programa: promedios por RAP y distribución. */
  async moment(momentId: number): Promise<MomentReportData> {
    const { data } = await httpClient.get<MomentReportData>(`/reports/moments/${momentId}`)
    return data
  },

  /**
   * 4. Reporte comparativo semestre X vs. semestre Y para el mismo programa y número de
   * momento (datos exclusivamente agregados por cohorte).
   */
  async comparative(params: ComparativeReportParams): Promise<ComparativeReportData> {
    const { data } = await httpClient.get<ComparativeReportData>('/reports/comparative', {
      params,
    })
    return data
  },
}
