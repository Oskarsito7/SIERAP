import { submissionService } from '@/services/api/submissionService'
import type { Submission } from '@/types/submission'

import { saveFile } from './files'

/**
 * Descarga un archivo de una versión de la entrega y lo guarda con su nombre original.
 * Se pide con el token por `httpClient`, por eso no basta con un enlace normal.
 */
export async function downloadSubmissionFile(submission: Submission, kind: 'zip' | 'presentation'): Promise<void> {
  if (kind === 'zip') {
    saveFile(await submissionService.downloadZip(submission.id), submission.zipFileName)
  } else {
    saveFile(await submissionService.downloadPresentation(submission.id), submission.presentationFileName ?? '')
  }
}
