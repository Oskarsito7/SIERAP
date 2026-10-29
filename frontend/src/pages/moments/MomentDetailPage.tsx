import { useState } from 'react'
import { ArrowLeft, Rocket, Trash2 } from 'lucide-react'
import { Link, NavLink, useNavigate, useParams } from 'react-router'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { todayIso } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { getErrorMessage } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import type { AcademicPeriod } from '@/types/catalog'
import type { MomentDetail } from '@/types/moment'

import { MomentForm } from './MomentForm'
import { toMomentData, toMomentForm } from './momentFormValues'
import { momentSteps } from './momentPhase'
import { MomentStatusBadge } from './MomentStatusBadge'
import { GroupsTab } from './GroupsTab'
import { LaunchPanel } from './LaunchPanel'
import { RubricTab } from './RubricTab'
import { WeightsTab } from './WeightsTab'

/** Pestañas de la configuración de un momento, cada una con su ruta. */
export type MomentTab = 'data' | 'rubric' | 'weights' | 'groups'

const TABS: { tab: MomentTab; label: string; path: string }[] = [
  { tab: 'data', label: 'Datos y ventanas', path: '' },
  { tab: 'rubric', label: 'RAP, criterios y niveles', path: '/rubric' },
  { tab: 'weights', label: 'Pesos de la nota', path: '/weights' },
  { tab: 'groups', label: 'Grupos', path: '/groups' },
]

/**
 * Configuración de un momento: el resumen, la línea de tiempo de sus fases, el botón
 * para lanzarlo y sus pestañas: "Datos y ventanas" (wireframe AD-02), "RAP, criterios y
 * niveles" (AD-03), "Pesos de la nota" (AD-04) y "Grupos" (AD-05). Mientras está en
 * configuración se edita; una vez lanzado, todo queda de solo lectura (ADR-012) y el
 * backend responde 422 a cualquier cambio.
 */
export function MomentDetailPage({ tab = 'data' }: { tab?: MomentTab }) {
  const id = Number(useParams().id)
  const data = useLoad(async () => {
    const [moment, periods] = await Promise.all([momentService.get(id), catalogService.periods()])
    return { moment, periods }
  }, [id])
  const [launching, setLaunching] = useState(false)
  const [launchedNotice, setLaunchedNotice] = useState(false)

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
  const refresh = async () => replace(await momentService.get(moment.id))

  return (
    <div className="flex flex-col gap-6">
      <BackLink periodId={moment.periodId} />
      <div className="flex flex-wrap items-start justify-between gap-4">
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
        {!launched && (
          <Button onClick={() => setLaunching((open) => !open)}>
            <Rocket aria-hidden />
            Lanzar momento
          </Button>
        )}
      </div>

      {launching && !launched && (
        <LaunchPanel
          moment={moment}
          onCancel={() => setLaunching(false)}
          onLaunched={(updated) => {
            setLaunching(false)
            setLaunchedNotice(true)
            replace(updated)
          }}
        />
      )}
      {launchedNotice && (
        <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
          Se lanzó el momento: la conformación de grupos quedó cerrada y su configuración ya no se puede modificar.
        </p>
      )}

      <Timeline moment={moment} />

      <nav aria-label="Secciones del momento" className="flex flex-wrap gap-1 border-b">
        {TABS.map((item) => (
          <NavLink
            key={item.tab}
            to={`/moments/${moment.id}${item.path}`}
            end
            className={({ isActive }) =>
              `-mb-px border-b-2 px-3 py-2 text-sm font-medium ${
                isActive ? 'border-primary text-foreground' : 'text-muted-foreground border-transparent'
              }`
            }
          >
            {item.label}
          </NavLink>
        ))}
      </nav>

      {launched && (
        <p role="note" className="bg-muted rounded-md px-3 py-2 text-sm">
          El momento ya fue lanzado: su configuración no se puede modificar.
        </p>
      )}

      {tab === 'data' && <DataTab moment={moment} periods={periods} onChange={replace} />}
      {tab === 'rubric' && <RubricTab moment={moment} onChange={refresh} />}
      {tab === 'weights' && <WeightsTab moment={moment} onChange={refresh} />}
      {tab === 'groups' && <GroupsTab moment={moment} />}
    </div>
  )
}

/** "Datos y ventanas" (AD-02): el formulario del momento y, sin lanzar, la opción de eliminarlo. */
function DataTab({
  moment,
  periods,
  onChange,
}: {
  moment: MomentDetail
  periods: AcademicPeriod[]
  onChange: (moment: MomentDetail) => void
}) {
  const navigate = useNavigate()
  const [notice, setNotice] = useState<string | null>(null)
  const launched = moment.status === 'LAUNCHED'

  return (
    <>
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
          onChange(await momentService.update(moment.id, toMomentData(values)))
          setNotice('Se guardaron los cambios del momento.')
        }}
      />

      {!launched && (
        <DeleteSection moment={moment} onDeleted={() => navigate(`/moments?periodId=${moment.periodId}`)} />
      )}
    </>
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
