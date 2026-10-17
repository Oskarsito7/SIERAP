import { useState } from 'react'
import { ArrowLeft, Trash2 } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { todayIso } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { getErrorMessage } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import type { MomentDetail } from '@/types/moment'

import { MomentForm } from './MomentForm'
import { toMomentData, toMomentForm } from './momentFormValues'
import { momentSteps } from './momentPhase'
import { MomentStatusBadge } from './MomentStatusBadge'

/**
 * Configuración de un momento (wireframe AD-02): el resumen, la línea de tiempo de sus
 * fases y la pestaña "Datos y ventanas". Mientras está en configuración se edita y se
 * puede eliminar (si no tiene grupos); una vez lanzado, todo queda de solo lectura
 * (ADR-012) y el backend responde 422 a cualquier cambio.
 */
export function MomentDetailPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const data = useLoad(async () => {
    const [moment, periods] = await Promise.all([momentService.get(id), catalogService.periods()])
    return { moment, periods }
  }, [id])
  const [notice, setNotice] = useState<string | null>(null)

  if (data.state.kind !== 'success') {
    return (
      <div className="flex flex-col gap-6">
        <BackLink />
        <LoadStatus state={data.state} onRetry={data.reload} label="Cargando el momento…" />
      </div>
    )
  }

  const { moment, periods } = data.state.data
  const launched = moment.status === 'LAUNCHED'
  const replace = (updated: MomentDetail) => data.setData((current) => ({ ...current, moment: updated }))

  return (
    <div className="flex flex-col gap-6">
      <BackLink periodId={moment.periodId} />
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-bold tracking-tight">
          {moment.name} · {moment.programName}
        </h1>
        <div className="flex flex-wrap items-center gap-3 text-sm">
          <MomentStatusBadge status={moment.status} />
          <span className="text-muted-foreground">
            {moment.subject} · {moment.periodCode} · semestres {moment.semesterFrom} a {moment.semesterTo} ·
            grupos de {moment.minMembers} a {moment.maxMembers}
          </span>
        </div>
      </div>

      <Timeline moment={moment} />

      <nav aria-label="Secciones del momento" className="flex gap-1 border-b">
        <span className="border-primary -mb-px border-b-2 px-3 py-2 text-sm font-medium" aria-current="page">
          Datos y ventanas
        </span>
      </nav>

      {launched && (
        <p role="note" className="bg-muted rounded-md px-3 py-2 text-sm">
          El momento ya fue lanzado: su configuración no se puede modificar.
        </p>
      )}
      {notice && (
        <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
          {notice}
        </p>
      )}

      <MomentForm
        key={`${moment.id}-${launched}`}
        initial={toMomentForm(moment)}
        periods={periods}
        readOnly={launched}
        submitLabel="Guardar cambios"
        onSubmit={async (values) => {
          setNotice(null)
          replace(await momentService.update(moment.id, toMomentData(values)))
          setNotice('Se guardaron los cambios del momento.')
        }}
      />

      {!launched && (
        <DeleteSection
          moment={moment}
          onDeleted={() => navigate(`/moments?periodId=${moment.periodId}`)}
        />
      )}
    </div>
  )
}

function BackLink({ periodId }: { periodId?: number }) {
  return (
    <Link
      to={periodId ? `/moments?periodId=${periodId}` : '/moments'}
      className="text-muted-foreground flex w-fit items-center gap-1 text-sm hover:underline"
    >
      <ArrowLeft className="size-4" aria-hidden />
      Momentos
    </Link>
  )
}

/** Fases del momento con la de hoy resaltada (ADR-012). */
function Timeline({ moment }: { moment: MomentDetail }) {
  const steps = momentSteps(moment, todayIso())
  return (
    <ol aria-label="Fases del momento" className="grid overflow-hidden rounded-md border sm:grid-cols-3 lg:grid-cols-6">
      {steps.map((step) => (
        <li
          key={step.label}
          aria-current={step.current ? 'step' : undefined}
          className={`border-b px-4 py-3 text-sm last:border-0 sm:border-r ${
            step.current ? 'bg-primary text-primary-foreground' : ''
          }`}
        >
          <span className="block font-semibold">{step.label}</span>
          <span className={step.current ? 'text-primary-foreground/80' : 'text-muted-foreground'}>{step.detail}</span>
        </li>
      ))}
    </ol>
  )
}

/** Eliminar el momento: solo sin lanzar y sin grupos; la API responde 409 si ya tiene grupos. */
function DeleteSection({ moment, onDeleted }: { moment: MomentDetail; onDeleted: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const remove = async () => {
    const question = `¿Eliminar ${moment.name} de ${moment.programName} con sus RAP, criterios, niveles y pesos?`
    if (!window.confirm(question)) return
    setError(null)
    setBusy(true)
    try {
      await momentService.remove(moment.id)
      onDeleted()
    } catch (removeError) {
      setError(getErrorMessage(removeError))
      setBusy(false)
    }
  }

  return (
    <Card className="border-destructive/40">
      <CardContent className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <p className="text-sm">
            {moment.groupCount > 0
              ? `No se puede eliminar: los docentes ya habilitaron ${groupsLabel(moment.groupCount)}.`
              : 'Eliminar el momento borra también sus RAP, criterios, niveles y pesos.'}
          </p>
          <Button variant="destructive" disabled={busy || moment.groupCount > 0} onClick={() => void remove()}>
            <Trash2 aria-hidden />
            Eliminar momento
          </Button>
        </div>
        <FormError message={error} />
      </CardContent>
    </Card>
  )
}

function groupsLabel(count: number): string {
  return count === 1 ? '1 grupo' : `${count} grupos`
}
