import { useState } from 'react'
import { Download } from 'lucide-react'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatDateTime } from '@/lib/dates'
import { formatSize } from '@/lib/files'
import { downloadSubmissionFile } from '@/lib/submissionDownload'
import type { LoadState } from '@/lib/useLoad'
import { getErrorMessage } from '@/services/api/httpClient'
import type { Submission, SubmissionHistory } from '@/types/submission'

/**
 * Historial de versiones de la entrega de un grupo con sus descargas (wireframes ES-03 y
 * DO-04). Lo usan el estudiante, el docente y el evaluador externo.
 */
export function SubmissionHistoryCard({
  state,
  onRetry,
  emptyMessage,
}: {
  state: LoadState<SubmissionHistory>
  onRetry: () => void
  /** Texto cuando el grupo todavía no ha subido entregas. */
  emptyMessage: string
}) {
  const [error, setError] = useState<string | null>(null)

  const download = async (submission: Submission, kind: 'zip' | 'presentation') => {
    setError(null)
    try {
      await downloadSubmissionFile(submission, kind)
    } catch (downloadError) {
      setError(getErrorMessage(downloadError))
    }
  }

  const current = state.kind === 'success' ? state.data.currentPresentation : null
  const latest = state.kind === 'success' ? state.data.versions[0] : undefined

  return (
    <Card>
      <CardHeader>
        <CardTitle>Historial de entregas</CardTitle>
        {current && latest && current.id !== latest.id && (
          <CardDescription>
            La presentación vigente es {current.presentationFileName}, de la versión {current.versionNumber}.
          </CardDescription>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <LoadStatus state={state} onRetry={onRetry} label="Cargando las entregas…" />
        <FormError message={error} />
        {state.kind === 'success' && state.data.versions.length === 0 && (
          <p className="text-muted-foreground text-sm">{emptyMessage}</p>
        )}
        {state.kind === 'success' && state.data.versions.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground border-b">
                <tr>
                  <th className="py-2 pr-4 font-medium">Versión</th>
                  <th className="py-2 pr-4 font-medium">Subida</th>
                  <th className="py-2 pr-4 font-medium">Proyecto</th>
                  <th className="py-2 pr-4 font-medium">Presentación</th>
                  <th className="py-2 font-medium">Estado</th>
                </tr>
              </thead>
              <tbody>
                {state.data.versions.map((version) => (
                  <tr key={version.id} className="border-b align-top last:border-0">
                    <td className="py-3 pr-4 font-semibold">v{version.versionNumber}</td>
                    <td className="py-3 pr-4 whitespace-nowrap">{formatDateTime(version.submittedAt)}</td>
                    <td className="py-3 pr-4">
                      <Button
                        variant="link"
                        className="h-auto p-0 break-all whitespace-normal"
                        aria-label={`Descargar ${version.zipFileName}`}
                        onClick={() => void download(version, 'zip')}
                      >
                        <Download aria-hidden />
                        {version.zipFileName}
                      </Button>
                      <span className="text-muted-foreground block text-xs">{formatSize(version.zipSize)}</span>
                    </td>
                    <td className="py-3 pr-4">
                      {version.presentationFileName ? (
                        <Button
                          variant="link"
                          className="h-auto p-0 break-all whitespace-normal"
                          aria-label={`Descargar ${version.presentationFileName}`}
                          onClick={() => void download(version, 'presentation')}
                        >
                          <Download aria-hidden />
                          {version.presentationFileName}
                        </Button>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td className="py-3">
                      <div className="flex flex-wrap gap-1">
                        <Badge variant={version.late ? 'destructive' : 'outline'}>
                          {version.late ? 'Tardía' : 'A tiempo'}
                        </Badge>
                        {version.toGrade && <Badge>Se califica</Badge>}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
