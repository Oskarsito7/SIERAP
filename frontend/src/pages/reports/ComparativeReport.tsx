import { useState } from 'react'

import { ComparisonColumns } from '@/components/charts/ComparisonColumns'
import { StatTile } from '@/components/charts/StatTile'
import { LoadStatus } from '@/components/LoadStatus'
import { SelectField } from '@/components/forms/SelectField'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { momentService } from '@/services/api/momentService'
import { reportService } from '@/services/api/reportService'
import type { AcademicPeriod, Program } from '@/types/catalog'
import type { ComparativeReportData, ComparativeReportQuery } from '@/types/report'

import { ReportCard, ReportTable } from './ReportParts'
import { formatDelta, formatGrade, formatPercentage } from './reportFormat'

/**
 * Reporte comparativo (wireframe AD-08): un periodo frente a otro, para el mismo
 * programa y número de momento. Solo muestra cifras agregadas, nunca estudiantes.
 */
export function ComparativeReport({
  periods,
  defaultPeriodId,
}: {
  periods: AcademicPeriod[]
  defaultPeriodId: number
}) {
  const programs = useLoad(() => catalogService.programs(), [])

  if (programs.state.kind !== 'success') {
    return <LoadStatus state={programs.state} onRetry={programs.reload} label="Cargando los programas…" />
  }
  if (programs.state.data.length === 0) {
    return <p className="text-muted-foreground text-sm">Tu facultad todavía no tiene programas.</p>
  }
  if (periods.length < 2) {
    return <p className="text-muted-foreground text-sm">Hacen falta al menos dos periodos académicos para comparar.</p>
  }
  return <ComparativeFilters periods={periods} programs={programs.state.data} defaultPeriodId={defaultPeriodId} />
}

function ComparativeFilters({
  periods,
  programs,
  defaultPeriodId,
}: {
  periods: AcademicPeriod[]
  programs: Program[]
  defaultPeriodId: number
}) {
  const [programId, setProgramId] = useState(programs[0].id)
  const [periodBId, setPeriodBId] = useState(defaultPeriodId)
  const [periodAId, setPeriodAId] = useState(() => previousPeriodId(periods, defaultPeriodId))
  const [momentNumber, setMomentNumber] = useState<number | null>(null)
  const moments = useLoad(
    () => Promise.all([momentService.list(periodAId), momentService.list(periodBId)]),
    [periodAId, periodBId],
  )

  // Números de momento del programa en cualquiera de los dos periodos, con su nombre.
  const options = new Map<number, string>()
  if (moments.state.kind === 'success') {
    for (const moment of moments.state.data.flat()) {
      if (moment.programId === programId && !options.has(moment.number)) options.set(moment.number, moment.name)
    }
  }
  const numbers = [...options.keys()].sort((a, b) => a - b)
  const selectedNumber = momentNumber !== null && options.has(momentNumber) ? momentNumber : numbers[0]
  const samePeriod = periodAId === periodBId

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end gap-4">
        <div className="w-64">
          <SelectField
            label="Programa"
            value={String(programId)}
            onChange={(event) => setProgramId(Number(event.target.value))}
          >
            {programs.map((program) => (
              <option key={program.id} value={String(program.id)}>
                {program.name}
              </option>
            ))}
          </SelectField>
        </div>
        <div className="w-48">
          <SelectField
            label="Momento"
            value={selectedNumber === undefined ? '' : String(selectedNumber)}
            disabled={numbers.length === 0}
            onChange={(event) => setMomentNumber(Number(event.target.value))}
          >
            {numbers.map((number) => (
              <option key={number} value={String(number)}>
                {options.get(number)}
              </option>
            ))}
          </SelectField>
        </div>
        <div className="w-36">
          <PeriodSelect label="Periodo base" periods={periods} value={periodAId} onChange={setPeriodAId} />
        </div>
        <div className="w-36">
          <PeriodSelect label="Frente a" periods={periods} value={periodBId} onChange={setPeriodBId} />
        </div>
      </div>

      <LoadStatus state={moments.state} onRetry={moments.reload} label="Cargando los momentos…" />
      {samePeriod && (
        <p role="alert" className="text-sm">
          Elige dos periodos distintos para comparar.
        </p>
      )}
      {!samePeriod && moments.state.kind === 'success' && selectedNumber === undefined && (
        <p className="text-muted-foreground text-sm">El programa no tiene momentos en estos dos periodos.</p>
      )}
      {!samePeriod && moments.state.kind === 'success' && selectedNumber !== undefined && (
        <ComparativeReportView query={{ programId, momentNumber: selectedNumber, periodAId, periodBId }} />
      )}
    </div>
  )
}

function PeriodSelect({
  label,
  periods,
  value,
  onChange,
}: {
  label: string
  periods: AcademicPeriod[]
  value: number
  onChange: (id: number) => void
}) {
  return (
    <SelectField label={label} value={String(value)} onChange={(event) => onChange(Number(event.target.value))}>
      {periods.map((period) => (
        <option key={period.id} value={String(period.id)}>
          {period.code}
        </option>
      ))}
    </SelectField>
  )
}

function ComparativeReportView({ query }: { query: ComparativeReportQuery }) {
  const report = useLoad(
    () => reportService.comparative(query),
    [query.programId, query.momentNumber, query.periodAId, query.periodBId],
  )
  return (
    <>
      <LoadStatus state={report.state} onRetry={report.reload} label="Generando el reporte…" />
      {report.state.kind === 'success' && <Comparative report={report.state.data} />}
    </>
  )
}

function Comparative({ report }: { report: ComparativeReportData }) {
  const { periodA, periodB } = report
  const codeA = periodA.period.code
  const codeB = periodB.period.code
  return (
    <div className="flex flex-col gap-6">
      <p className="text-muted-foreground text-sm">
        {report.program.name} · {periodB.momentName} · {codeA} frente a {codeB}. Solo cifras agregadas: el
        comparativo no muestra estudiantes.
      </p>

      <div className="grid gap-3 sm:grid-cols-3">
        <StatTile
          label={`Nota promedio en ${codeB}`}
          value={formatGrade(periodB.averageGrade)}
          detail={`${formatDelta(report.deltaAverageGrade)} frente a ${codeA} (${formatGrade(periodA.averageGrade)})`}
        />
        <StatTile
          label={`Aprobados en ${codeB}`}
          value={formatPercentage(periodB.passingPercentage)}
          detail={`${formatDelta(report.deltaPassingPercentage)} puntos frente a ${codeA} (${formatPercentage(
            periodA.passingPercentage,
          )})`}
        />
        <StatTile
          label={`Estudiantes calificados en ${codeB}`}
          value={String(periodB.gradedStudents)}
          detail={`${periodA.gradedStudents} en ${codeA}`}
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
        <ReportCard title="Desempeño promedio por RAP" description="Logro promedio de cada RAP sobre su máximo.">
          <ComparisonColumns
            label="Desempeño promedio por RAP"
            labelA={codeA}
            labelB={codeB}
            format={formatPercentage}
            items={report.learningOutcomes.map((outcome) => ({
              key: outcome.code,
              label: outcome.code,
              a: outcome.percentageA,
              b: outcome.percentageB,
            }))}
          />
        </ReportCard>
        <ReportCard title="Cambio por RAP" description="En puntos porcentuales.">
          <ReportTable
            label="Cambio por RAP"
            columns={[
              { header: 'RAP' },
              { header: codeA, numeric: true },
              { header: codeB, numeric: true },
              { header: 'Cambio', numeric: true },
            ]}
            rows={report.learningOutcomes.map((outcome) => ({
              key: outcome.code,
              cells: [
                <span className="font-medium" title={outcome.description}>
                  {outcome.code}
                </span>,
                formatPercentage(outcome.percentageA),
                formatPercentage(outcome.percentageB),
                formatDelta(outcome.deltaPercentage),
              ],
            }))}
          />
        </ReportCard>
      </div>

      <ReportCard title="Distribución de notas" description="Parte de los estudiantes calificados en cada franja.">
        <ComparisonColumns
          label="Distribución de notas"
          labelA={codeA}
          labelB={codeB}
          format={formatPercentage}
          items={periodB.distribution.map((bucket) => ({
            key: bucket.label,
            label: bucket.label,
            // Las franjas se emparejan por nombre: sus límites pueden cambiar entre periodos.
            a: periodA.distribution.find((item) => item.label === bucket.label)?.percentage ?? null,
            b: bucket.percentage,
          }))}
        />
      </ReportCard>
    </div>
  )
}

/** El periodo que empezó justo antes del elegido; si no hay, cualquier otro. */
function previousPeriodId(periods: AcademicPeriod[], periodId: number): number {
  const current = periods.find((period) => period.id === periodId)
  const earlier = periods
    .filter((period) => current && period.startDate < current.startDate)
    .sort((a, b) => b.startDate.localeCompare(a.startDate))
  return (earlier[0] ?? periods.find((period) => period.id !== periodId) ?? periods[0]).id
}
