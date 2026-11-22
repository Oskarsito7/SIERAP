import type {
  ComparativeReportData,
  ComparativeReportQuery,
  GroupReportData,
  IndividualReportData,
  MomentReportData,
} from '@/types/report'

import { fileNameFrom, httpClient, type DownloadedFile } from './httpClient'

/** Pide un reporte como archivo; el nombre lo propone el servidor. */
async function download(url: string, fallback: string, params?: Record<string, number>): Promise<DownloadedFile> {
  const response = await httpClient.get<Blob>(url, { params, responseType: 'blob', timeout: 0 })
  return { blob: response.data, fileName: fileNameFrom(response.headers['content-disposition'], fallback) }
}

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

  /** Sábana de notas del momento en Excel (ADR-020). */
  momentWorkbook(momentId: number): Promise<DownloadedFile> {
    return download(`/reports/moments/${momentId}/xlsx`, 'sabana-notas.xlsx')
  },

  /** Informe del momento en PDF. */
  momentPdf(momentId: number): Promise<DownloadedFile> {
    return download(`/reports/moments/${momentId}/pdf`, 'informe-momento.pdf')
  },

  /** Acta de evaluación del grupo en PDF. */
  groupPdf(groupId: number): Promise<DownloadedFile> {
    return download(`/reports/groups/${groupId}/pdf`, 'acta-grupo.pdf')
  },

  /** Boletín individual en PDF; un estudiante omite `studentId` para pedir el suyo. */
  individualPdf(momentId: number, studentId?: number): Promise<DownloadedFile> {
    const params: Record<string, number> = { momentId }
    if (studentId !== undefined) params.studentId = studentId
    return download('/reports/individual/pdf', 'boletin.pdf', params)
  },

  /** Un periodo frente a otro, para el mismo programa y número de momento; solo cifras agregadas. */
  async comparative(query: ComparativeReportQuery): Promise<ComparativeReportData> {
    const { data } = await httpClient.get<ComparativeReportData>('/reports/comparative', { params: query })
    return data
  },
}
