import { useState, type FormEvent } from 'react'
import { CalendarPlus } from 'lucide-react'

import { FormError, FormField } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { formatDate } from '@/lib/dates'
import { useConfirm } from '@/lib/useConfirm'
import { getErrorMessage } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import type { AcademicPeriod } from '@/types/catalog'
import type { MomentDetail } from '@/types/moment'

const MIN_REASON = 10
const MAX_REASON = 500

/**
 * "Prórroga excepcional" de un momento lanzado (ADR-023, excepción acotada al ADR-012):
 * muestra las fechas actuales y permite ampliar el fin de entregas y/o el de
 * calificación, con un motivo obligatorio. Las fechas solo se amplían (hasta el límite
 * de la UPC del periodo) y el cambio queda en la bitácora; el backend valida todo y
 * responde 422 con el mensaje si algo no cumple.
 */
export function ExtensionSection({
  moment,
  periods,
  onExtended,
}: {
  moment: MomentDetail
  periods: AcademicPeriod[]
  onExtended: (moment: MomentDetail) => void
}) {
  const [submissionEnd, setSubmissionEnd] = useState('')
  const [gradingEnd, setGradingEnd] = useState('')
  const [reason, setReason] = useState('')
  const [errors, setErrors] = useState<{ dates?: string; reason?: string }>({})
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [confirm, confirmDialog] = useConfirm()
  const deadline = periods.find((period) => period.id === moment.periodId)?.upcEvaluationDeadline

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setError(null)
    setNotice(null)
    const found: { dates?: string; reason?: string } = {}
    if (!submissionEnd && !gradingEnd) found.dates = 'Indica al menos una fecha nueva'
    else if (
      (submissionEnd && moment.submissionEnd && submissionEnd <= moment.submissionEnd) ||
      (gradingEnd && moment.gradingEnd && gradingEnd <= moment.gradingEnd)
    ) {
      found.dates = 'Las fechas solo se pueden ampliar: deben ser posteriores a las actuales'
    } else if (gradingEnd && deadline && gradingEnd > deadline) {
      found.dates = `La calificación no puede terminar después del límite de la UPC (${formatDate(deadline)})`
    }
    const trimmed = reason.trim()
    if (trimmed.length < MIN_REASON || trimmed.length > MAX_REASON) {
      found.reason = `El motivo debe tener entre ${MIN_REASON} y ${MAX_REASON} caracteres`
    }
    setErrors(found)
    if (found.dates || found.reason) return

    const confirmed = await confirm({
      title: '¿Prorrogar las fechas del momento?',
      description: 'Las fechas solo se pueden ampliar y quedará registrado en la bitácora.',
      confirmLabel: 'Prorrogar',
    })
    if (!confirmed) return
    setBusy(true)
    try {
      const updated = await momentService.extend(moment.id, {
        submissionEnd: submissionEnd || undefined,
        gradingEnd: gradingEnd || undefined,
        reason: trimmed,
      })
      onExtended(updated)
      setSubmissionEnd('')
      setGradingEnd('')
      setReason('')
      setNotice('Se prorrogaron las fechas del momento y quedó registrado en la bitácora.')
    } catch (extendError) {
      setError(getErrorMessage(extendError))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card aria-label="Prórroga excepcional" role="region">
      {confirmDialog}
      <CardHeader>
        <CardTitle>Prórroga excepcional</CardTitle>
        <CardDescription>
          Excepción para un momento ya lanzado: solo se puede ampliar el fin de las entregas y el de la calificación,
          sin pasar del límite de la UPC{deadline ? ` (${formatDate(deadline)})` : ''}. Queda en la bitácora con el
          motivo.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={(event) => void submit(event)} className="flex flex-col gap-4" noValidate>
          <dl className="grid gap-2 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted-foreground">Fin de entregas actual</dt>
              <dd className="font-medium">{moment.submissionEnd ? formatDate(moment.submissionEnd) : 'Sin definir'}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Fin de calificación actual</dt>
              <dd className="font-medium">{moment.gradingEnd ? formatDate(moment.gradingEnd) : 'Sin definir'}</dd>
            </div>
          </dl>
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField
              label="Nuevo fin de entregas"
              type="date"
              value={submissionEnd}
              min={moment.submissionEnd ?? undefined}
              max={deadline}
              onChange={(event) => setSubmissionEnd(event.target.value)}
            />
            <FormField
              label="Nuevo fin de calificación"
              type="date"
              value={gradingEnd}
              min={moment.gradingEnd ?? undefined}
              max={deadline}
              onChange={(event) => setGradingEnd(event.target.value)}
            />
          </div>
          {errors.dates && <p className="text-destructive text-sm">{errors.dates}</p>}
          <div className="flex flex-col gap-2">
            <Label htmlFor="extension-reason">Motivo</Label>
            <textarea
              id="extension-reason"
              className="border-input bg-background min-h-20 rounded-md border px-3 py-2 text-sm shadow-xs"
              value={reason}
              maxLength={MAX_REASON}
              aria-invalid={errors.reason ? true : undefined}
              onChange={(event) => setReason(event.target.value)}
            />
            {errors.reason && <p className="text-destructive text-sm">{errors.reason}</p>}
          </div>
          <FormError message={error} />
          {notice && (
            <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
              {notice}
            </p>
          )}
          <div>
            <Button type="submit" disabled={busy}>
              <CalendarPlus aria-hidden />
              {busy ? 'Prorrogando…' : 'Prorrogar fechas'}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
