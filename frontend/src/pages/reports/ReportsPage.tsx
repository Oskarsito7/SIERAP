import { useState } from 'react'
import { useSearchParams } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { LoadStatus } from '@/components/LoadStatus'
import { SelectField } from '@/components/forms/SelectField'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { momentService } from '@/services/api/momentService'
import type { AcademicPeriod, Program } from '@/types/catalog'
import type { MomentSummary } from '@/types/moment'

import { ComparativeReportTab } from './ComparativeReportTab'
import { GroupReportTab } from './GroupReportTab'
import { IndividualReportTab } from './IndividualReportTab'
import { MomentReportTab } from './MomentReportTab'

/** Pestañas disponibles en el módulo de reportes (Etapa 7, RF 12). */
export type ReportTab = 'individual' | 'group' | 'moment' | 'comparative'

const REPORT_TABS: { tab: ReportTab; label: string }[] = [
  { tab: 'individual', label: 'Individual' },
  { tab: 'group', label: 'Grupal' },
  { tab: 'moment', label: 'Consolidado' },
  { tab: 'comparative', label: 'Comparativo' },
]

function isValidTab(value: string | null): value is ReportTab {
  return value === 'individual' || value === 'group' || value === 'moment' || value === 'comparative'
}

/**
 * Pantalla principal de Reportes (`/reports`, Etapa 7, RF 12, wireframe `AD-08`):
 * - Individual: desempeño de un estudiante por RAP;
 * - Grupal: nota del grupo, desempeño por RAP e integrantes con ajustes individuales;
 * - Consolidado: promedios por RAP, aprobación y distribución del momento;
 * - Comparativo: semestre X vs. semestre Y (exclusivamente agregado por cohorte).
 */
export function ReportsPage() {
  const { user } = useAuth()
  const catalogs = useLoad(async () => {
    const [periods, programs] = await Promise.all([catalogService.periods(), catalogService.programs()])
    return { periods, programs }
  }, [])

  const subtitle =
    user?.role === 'TEACHER'
      ? 'Consulta el desempeño individual y por grupo de tus estudiantes, el consolidado del momento y el comparativo entre semestres.'
      : 'Analiza el desempeño individual, grupal, consolidado por momento y comparativo histórico entre periodos académicos.'

  if (catalogs.state.kind !== 'success') {
    return (
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">Reportes</h1>
          <p className="text-muted-foreground max-w-2xl text-sm">{subtitle}</p>
        </div>
        <LoadStatus state={catalogs.state} onRetry={catalogs.reload} label="Cargando los catálogos académicos…" />
      </div>
    )
  }

  const { periods, programs } = catalogs.state.data

  if (periods.length === 0 || programs.length === 0) {
    return (
      <div className="flex flex-col gap-6">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">Reportes</h1>
          <p className="text-muted-foreground max-w-2xl text-sm">{subtitle}</p>
        </div>
        <Card>
          <CardHeader>
            <CardTitle>Catálogos incompletos</CardTitle>
            <CardDescription>
              {periods.length === 0
                ? 'Todavía no hay periodos académicos registrados en el sistema.'
                : 'Tu facultad todavía no tiene programas académicos registrados.'}
            </CardDescription>
          </CardHeader>
        </Card>
      </div>
    )
  }

  return <ReportsContent periods={periods} programs={programs} subtitle={subtitle} />
}

function ReportsContent({
  periods,
  programs,
  subtitle,
}: {
  periods: AcademicPeriod[]
  programs: Program[]
  subtitle: string
}) {
  const [searchParams, setSearchParams] = useSearchParams()
  const rawTab = searchParams.get('tab')
  const activeTab: ReportTab = isValidTab(rawTab) ? rawTab : 'individual'

  const [periodId, setPeriodId] = useState<number>(periods[0].id)
  const [programId, setProgramId] = useState<number>(programs[0].id)
  const [momentId, setMomentId] = useState<number | null>(null)

  const moments = useLoad(() => momentService.list(periodId), [periodId])

  const selectTab = (tab: ReportTab) => {
    setSearchParams({ tab })
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">Reportes</h1>
        <p className="text-muted-foreground max-w-2xl text-sm">{subtitle}</p>
      </div>

      <div role="tablist" aria-label="Tipo de reporte" className="flex flex-wrap gap-1 border-b">
        {REPORT_TABS.map((item) => {
          const selected = activeTab === item.tab
          return (
            <button
              key={item.tab}
              type="button"
              role="tab"
              aria-selected={selected}
              onClick={() => selectTab(item.tab)}
              className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium transition-colors ${
                selected
                  ? 'border-primary text-foreground'
                  : 'text-muted-foreground hover:text-foreground border-transparent'
              }`}
            >
              {item.label}
            </button>
          )
        })}
      </div>

      {activeTab === 'comparative' ? (
        <ComparativeReportTab periods={periods} programs={programs} initialProgramId={programId} />
      ) : (
        <MomentFilteredReports
          activeTab={activeTab}
          periods={periods}
          programs={programs}
          periodId={periodId}
          programId={programId}
          selectedMomentId={momentId}
          momentsState={moments.state}
          onReloadMoments={moments.reload}
          onPeriodChange={(nextPeriodId) => {
            setPeriodId(nextPeriodId)
            setMomentId(null)
          }}
          onProgramChange={(nextProgramId) => {
            setProgramId(nextProgramId)
            setMomentId(null)
          }}
          onMomentChange={setMomentId}
        />
      )}
    </div>
  )
}

function MomentFilteredReports({
  activeTab,
  periods,
  programs,
  periodId,
  programId,
  selectedMomentId,
  momentsState,
  onReloadMoments,
  onPeriodChange,
  onProgramChange,
  onMomentChange,
}: {
  activeTab: Exclude<ReportTab, 'comparative'>
  periods: AcademicPeriod[]
  programs: Program[]
  periodId: number
  programId: number
  selectedMomentId: number | null
  momentsState: ReturnType<typeof useLoad<MomentSummary[]>>['state']
  onReloadMoments: () => void
  onPeriodChange: (id: number) => void
  onProgramChange: (id: number) => void
  onMomentChange: (id: number) => void
}) {
  const programMoments =
    momentsState.kind === 'success'
      ? momentsState.data
          .filter((moment) => moment.programId === programId)
          .sort((a, b) => a.number - b.number)
      : []

  const activeMoment =
    programMoments.find((moment) => moment.id === selectedMomentId) ?? programMoments[0] ?? null

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardContent className="grid gap-4 sm:grid-cols-3">
          <SelectField
            label="Periodo"
            value={String(periodId)}
            onChange={(event) => onPeriodChange(Number(event.target.value))}
          >
            {periods.map((period) => (
              <option key={period.id} value={String(period.id)}>
                {period.code}
              </option>
            ))}
          </SelectField>

          <SelectField
            label="Programa"
            value={String(programId)}
            onChange={(event) => onProgramChange(Number(event.target.value))}
          >
            {programs.map((program) => (
              <option key={program.id} value={String(program.id)}>
                {program.name}
              </option>
            ))}
          </SelectField>

          {momentsState.kind === 'success' && programMoments.length > 0 && activeMoment && (
            <SelectField
              label="Momento"
              value={String(activeMoment.id)}
              onChange={(event) => onMomentChange(Number(event.target.value))}
            >
              {programMoments.map((moment) => (
                <option key={moment.id} value={String(moment.id)}>
                  {moment.name} · {moment.subject}
                </option>
              ))}
            </SelectField>
          )}
        </CardContent>
      </Card>

      {momentsState.kind !== 'success' && (
        <LoadStatus state={momentsState} onRetry={onReloadMoments} label="Cargando los momentos del periodo…" />
      )}

      {momentsState.kind === 'success' && programMoments.length === 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Sin momentos configurados</CardTitle>
            <CardDescription>
              El programa seleccionado no tiene momentos en este periodo académico.
            </CardDescription>
          </CardHeader>
        </Card>
      )}

      {momentsState.kind === 'success' && activeMoment && (
        <>
          {activeTab === 'individual' && <IndividualReportTab momentId={activeMoment.id} />}
          {activeTab === 'group' && <GroupReportTab momentId={activeMoment.id} />}
          {activeTab === 'moment' && <MomentReportTab momentId={activeMoment.id} />}
        </>
      )}
    </div>
  )
}
