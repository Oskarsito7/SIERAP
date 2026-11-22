import { useState } from 'react'
import { Rocket } from 'lucide-react'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useLoad } from '@/lib/useLoad'
import { getErrorMessage } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import type { LaunchCheck, MomentDetail } from '@/types/moment'

/**
 * Lanzar un momento (wireframe AD-05, ADR-012). Antes muestra todo lo que lo impide
 * (ventanas, RAP, criterios, niveles, pesos que no suman 100 %), los grupos por debajo
 * del mínimo, que piden confirmación, y los tipos de evaluador sin peso, que solo se
 * avisan. Al lanzar se bloquean la configuración y los grupos, y se cierra la conformación.
 */
export function LaunchPanel({
  moment,
  onCancel,
  onLaunched,
}: {
  moment: MomentDetail
  onCancel: () => void
  onLaunched: (moment: MomentDetail) => void
}) {
  const check = useLoad(() => momentService.launchCheck(moment.id), [moment.id])

  return (
    <Card aria-label={`Lanzar ${moment.name}`} role="region">
      <CardHeader>
        <CardTitle>Lanzar {moment.name}</CardTitle>
        <CardDescription>
          Se bloquean la configuración y los integrantes de los grupos, y se cierra la conformación.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <LoadStatus state={check.state} onRetry={check.reload} label="Revisando el momento…" />
        {check.state.kind === 'success' && (
          <LaunchBody moment={moment} check={check.state.data} onCancel={onCancel} onLaunched={onLaunched} />
        )}
      </CardContent>
    </Card>
  )
}

function LaunchBody({
  moment,
  check,
  onCancel,
  onLaunched,
}: {
  moment: MomentDetail
  check: LaunchCheck
  onCancel: () => void
  onLaunched: (moment: MomentDetail) => void
}) {
  const [confirmed, setConfirmed] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const belowMinimum = check.groupsBelowMinimum
  const needsConfirmation = belowMinimum.length > 0
  const canSubmit = check.canLaunch && (!needsConfirmation || confirmed) && !busy

  const launch = async () => {
    setError(null)
    setBusy(true)
    try {
      onLaunched(await momentService.launch(moment.id, needsConfirmation))
    } catch (launchError) {
      setError(getErrorMessage(launchError))
      setBusy(false)
    }
  }

  return (
    <>
      {check.problems.length > 0 && (
        <div role="alert" className="border-destructive/50 rounded-md border px-4 py-3 text-sm">
          <p className="font-semibold">Todavía no se puede lanzar:</p>
          <ul className="mt-1 list-disc pl-5">
            {check.problems.map((problem) => (
              <li key={problem}>{problem}</li>
            ))}
          </ul>
        </div>
      )}

      {needsConfirmation && (
        <div className="rounded-md border-2 px-4 py-3 text-sm">
          <p className="font-semibold">
            {belowMinimum.length === 1
              ? `1 grupo está por debajo del mínimo de ${check.minMembers} integrantes`
              : `${belowMinimum.length} grupos están por debajo del mínimo de ${check.minMembers} integrantes`}
          </p>
          <p className="text-muted-foreground">
            {belowMinimum.map((group) => `${group.name} (${members(group.memberCount)})`).join(', ')}
            . Quedarán como están.
          </p>
        </div>
      )}

      {check.evaluatorTypesWithoutWeight.length > 0 && (
        <div className="bg-muted/40 rounded-md border px-4 py-3 text-sm">
          <p className="font-semibold">Hay evaluadores cuya evaluación no contará en la nota</p>
          <p className="text-muted-foreground">
            {check.evaluatorTypesWithoutWeight
              .map((type) => `${type.evaluatorTypeName} (${type.evaluatorCount})`)
              .join(', ')}
            : su tipo no tiene peso en el momento.
          </p>
        </div>
      )}

      {check.canLaunch && needsConfirmation && (
        <label className="flex items-center gap-2 text-sm">
          <input type="checkbox" checked={confirmed} onChange={(event) => setConfirmed(event.target.checked)} />
          Entiendo y quiero lanzar el momento.
        </label>
      )}

      <FormError message={error} />
      <div className="flex gap-3">
        <Button disabled={!canSubmit} onClick={() => void launch()}>
          <Rocket aria-hidden />
          {busy ? 'Lanzando…' : 'Lanzar momento'}
        </Button>
        <Button variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
      </div>
    </>
  )
}

function members(count: number): string {
  return count === 1 ? '1 integrante' : `${count} integrantes`
}
