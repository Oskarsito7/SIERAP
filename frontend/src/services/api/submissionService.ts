import type { Submission, SubmissionHistory } from '@/types/submission'

import { httpClient } from './httpClient'

/** Entregas de los grupos (`docs/07-contrato-api.md`, "Submissions"). */
export const submissionService = {
  /**
   * Sube una versión nueva de la entrega. Sin límite de tiempo, porque un `.zip` de
   * 100 MB puede tardar más que el tiempo por defecto del cliente.
   *
   * @param onProgress recibe el porcentaje subido (0 a 100)
   */
  async upload(
    groupId: number,
    zip: File,
    presentation: File | null,
    onProgress?: (percent: number) => void,
  ): Promise<Submission> {
    const form = new FormData()
    form.append('zip', zip)
    if (presentation) form.append('presentation', presentation)
    const { data } = await httpClient.post<Submission>(`/groups/${groupId}/submissions`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 0,
      onUploadProgress: (event) => {
        if (onProgress && event.total) onProgress(Math.round((event.loaded * 100) / event.total))
      },
    })
    return data
  },

  async history(groupId: number): Promise<SubmissionHistory> {
    const { data } = await httpClient.get<SubmissionHistory>(`/groups/${groupId}/submissions`)
    return data
  },

  /** Descarga el `.zip` de una versión; se pide con el token, por eso llega como `Blob`. */
  async downloadZip(submissionId: number): Promise<Blob> {
    const { data } = await httpClient.get<Blob>(`/submissions/${submissionId}/zip`, {
      responseType: 'blob',
      timeout: 0,
    })
    return data
  },

  async downloadPresentation(submissionId: number): Promise<Blob> {
    const { data } = await httpClient.get<Blob>(`/submissions/${submissionId}/presentation`, {
      responseType: 'blob',
      timeout: 0,
    })
    return data
  },
}
