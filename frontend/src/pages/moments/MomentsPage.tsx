import { useState } from 'react'
import { Copy, Plus } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError } from '@/components/forms/FormField'
import { SelectField } from '@/components/forms/SelectField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { todayIso } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { getErrorMessage } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import type { AcademicPeriod, Program } from '@/types/catalog'

import { momentPhase } from './momentPhase'
import { MomentStatusBadge } from './MomentStatusBadge'

/**
 * Momentos de los programas de la facultad del admin en un periodo (wireframe AD-01),
 * con su estado y su fase de hoy. Desde aquí se crea un momento nuevo o se copia la
 * configuración de otro periodo (ADR-010). El periodo y el programa elegidos quedan en
 * la dirección (`?periodId=&programId=`).
 */
export function MomentsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const catalogs = useLoad(async () => {
    const [periods, programs, faculties] = await Promise.all([
      catalogService.periods(),
      catalogService.programs(),
      catalogService.faculties(),
    ])
    return { periods, programs, facultyName: faculties.length === 1 ? faculties[0].name : null }
  }, [])

  if (catalogs.state.kind !== 'success') {
    return (
      <div className="flex flex-col gap-6">
        <h1 className="text-2xl font-bold tracking-tight">Momentos</h1>
        <LoadStatus state={catalogs.state} onRetry={catalogs.reload} label="Cargando los periodos…" />
      </div>
    )
  }

  const { periods, programs, facultyName } = catalogs.state.data
  const requested = periods.find((period) => String(period.id) === searchParams.get('periodId'))
  const period = requested ?? periods.find((item) => item.status === 'CURRENT') ?? periods[0]
  const programId = searchParams.get('programId') ?? ''

  const select = (changes: { periodId?: number; programId?: string }) => {
    const next = new URLSearchParams()
    next.set('periodId', String(changes.periodId ?? period.id))
    const program = changes.programId ?? programId
    if (program) next.set('programId', program)
    setSearchParams(next)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">Momentos</h1>
        {facultyName && <p className="text-muted-foreground text-sm">{facultyName}</p>}
      </div>
      {!period ? (
        <p className="text-muted-foreground text-sm">Todavía no hay periodos académicos. Los crea el super_admin.</p>
      ) : (
        <PeriodMoments
          key={period.id}
          period={period}
          periods={periods}
          programs={programs}
          programId={programId}
          onSelect={select}
        />
      )}
    </div>
  )
}

function PeriodMoments({
  period,
  periods,
  programs,
  programId,
  onSelect,
}: {
  period: AcademicPeriod
  periods: AcademicPeriod[]
  programs: Program[]
  programId: string
  onSelect: (changes: { periodId?: number; programId?: string }) => void
}) {
  const moments = useLoad(() => momentService.list(period.id), [period.id])
  const [copying, setCopying] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const today = todayIso()

  const all = moments.state.kind === 'success' ? moments.state.data : []
  const visible = programId ? all.filter((moment) => String(moment.programId) === programId) : all

  return (
    <>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="flex flex-wrap gap-4">
          <SelectField
            label="Periodo"
            value={period.id}
            onChange={(event) => onSelect({ periodId: Number(event.target.value) })}
          >
            {periods.map((item) => (
              <option key={item.id} value={item.id}>
                {item.code}
              </option>
            ))}
          </SelectField>
          <SelectField
            label="Programa"
            value={programId}
            onChange={(event) => onSelect({ programId: event.target.value })}
          >
            <option value="">Todos los programas</option>
            {programs.map((program) => (
              <option key={program.id} value={program.id}>
                {program.name}
              </option>
            ))}
          </SelectField>
        </div>
        <div className="flex flex-wrap gap-3">
          <Button variant="outline" onClick={() => setCopying((open) => !open)}>
            <Copy aria-hidden />
            Copiar de otro periodo
          </Button>
          <Button asChild>
            <Link to={`/moments/new?periodId=${period.id}`}>
              <Plus aria-hidden />
              Nuevo momento
            </Link>
          </Button>
        </div>
      </div>

      {copying && (
        <CopyPanel
          target={period}
          periods={periods}
          programs={programs}
          onCancel={() => setCopying(false)}
          onCopied={(count) => {
            setCopying(false)
            setNotice(
              `Se ${count === 1 ? 'copió 1 momento' : `copiaron ${count} momentos`} al periodo ${period.code}. ` +
                'Las ventanas de fechas quedan vacías: defínelas en cada momento.',
            )
            moments.reload()
          }}
        />
      )}

      {notice && (
        <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
          {notice}
        </p>
      )}

      <Card>
        <CardContent className="flex flex-col gap-4">
          <LoadStatus state={moments.state} onRetry={moments.reload} label="Cargando los momentos…" />
          {moments.state.kind === 'success' && visible.length === 0 && (
            <p className="text-muted-foreground text-sm">
              No hay momentos {programId ? 'de este programa ' : ''}en el periodo {period.code}.
            </p>
          )}
          {visible.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground border-b">
                  <tr>
                    <th className="py-2 pr-4 font-medium">Programa</th>
                    <th className="py-2 pr-4 font-medium">Momento</th>
                    <th className="py-2 pr-4 font-medium">Semestres</th>
                    <th className="py-2 pr-4 font-medium">Estado</th>
                    <th className="py-2 pr-4 font-medium">Fase de hoy</th>
                    <th className="py-2 pr-4 text-right font-medium">Grupos</th>
                    <th className="py-2 font-medium">
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((moment) => {
                    const phase = momentPhase(moment, today)
                    const action = moment.status === 'CONFIGURATION' ? 'Configurar' : 'Ver'
                    return (
                      <tr key={moment.id} className="border-b last:border-0">
                        <td className="py-3 pr-4">{moment.programName}</td>
                        <td className="py-3 pr-4">
                          <span className="font-medium">{moment.name}</span>
                          <span className="text-muted-foreground block text-xs">{moment.subject}</span>
                        </td>
                        <td className="py-3 pr-4 whitespace-nowrap">
                          {moment.semesterFrom} a {moment.semesterTo}
                        </td>
                        <td className="py-3 pr-4">
                          <MomentStatusBadge status={moment.status} />
                        </td>
                        <td className="py-3 pr-4">
                          {phase.label}
                          {phase.detail && <span className="text-muted-foreground block text-xs">{phase.detail}</span>}
                        </td>
                        <td className="py-3 pr-4 text-right">{moment.groupCount}</td>
                        <td className="py-3 text-right">
                          <Button variant="outline" size="sm" asChild>
                            <Link
                              to={`/moments/${moment.id}`}
                              aria-label={`${action} ${moment.name} de ${moment.programName}`}
                            >
                              {action}
                            </Link>
                          </Button>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </>
  )
}

/**
 * Copiar la configuración de otro periodo al periodo elegido (ADR-010): RAP, criterios,
 * niveles y pesos, sin las ventanas de fechas ni los grupos. Es todo o nada: si el
 * destino ya tiene un momento con el mismo programa y número, no copia ninguno.
 */
function CopyPanel({
  target,
  periods,
  programs,
  onCancel,
  onCopied,
}: {
  target: AcademicPeriod
  periods: AcademicPeriod[]
  programs: Program[]
  onCancel: () => void
  onCopied: (count: number) => void
}) {
  const sources = periods.filter((period) => period.id !== target.id)
  const [fromPeriodId, setFromPeriodId] = useState(sources[0] ? String(sources[0].id) : '')
  const [programId, setProgramId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const copy = async () => {
    setError(null)
    setBusy(true)
    try {
      const copied = await momentService.copy({
        fromPeriodId: Number(fromPeriodId),
        toPeriodId: target.id,
        programId: programId ? Number(programId) : undefined,
      })
      onCopied(copied.length)
    } catch (copyError) {
      setError(getErrorMessage(copyError))
      setBusy(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Copiar la configuración al periodo {target.code}</CardTitle>
        <CardDescription>
          Se copian los momentos con sus RAP, criterios, niveles y pesos. Las ventanas de fechas y los grupos no se
          copian. Si el periodo {target.code} ya tiene un momento con el mismo programa y número, no se copia nada.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {sources.length === 0 ? (
          <p className="text-muted-foreground text-sm">No hay otro periodo del cual copiar.</p>
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            <SelectField
              label="Copiar del periodo"
              value={fromPeriodId}
              onChange={(event) => setFromPeriodId(event.target.value)}
            >
              {sources.map((period) => (
                <option key={period.id} value={period.id}>
                  {period.code}
                </option>
              ))}
            </SelectField>
            <SelectField label="Programas" value={programId} onChange={(event) => setProgramId(event.target.value)}>
              <option value="">Todos los programas</option>
              {programs.map((program) => (
                <option key={program.id} value={program.id}>
                  {program.name}
                </option>
              ))}
            </SelectField>
          </div>
        )}
        <FormError message={error} />
        <div className="flex gap-3">
          <Button disabled={busy || sources.length === 0} onClick={() => void copy()}>
            {busy ? 'Copiando…' : 'Copiar'}
          </Button>
          <Button variant="outline" onClick={onCancel}>
            Cancelar
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
