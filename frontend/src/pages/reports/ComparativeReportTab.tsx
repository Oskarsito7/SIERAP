import { useState } from 'react'

import { LoadStatus } from '@/components/LoadStatus'
import { SelectField } from '@/components/forms/SelectField'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useLoad } from '@/lib/useLoad'
import { momentService } from '@/services/api/momentService'
import { reportService } from '@/services/api/reportService'
import type { AcademicPeriod, Program } from '@/types/catalog'
import type { ComparativeReportData } from '@/types/report'

import {
  clampPercentage,
  formatNullableScore,
  formatPercentage,
  formatPercentageDelta,
  formatScoreDelta,
} from './reportFormat'

interface ComparativeReportTabProps {
  periods: AcademicPeriod[]
  programs: Program[]
  initialProgramId: number
}

/**
 * Pestaña 4 — Reporte Comparativo Semestre X vs. Semestre Y (wireframe `AD-08`,
 * `GET /api/reports/comparative`): compara el mismo programa y número de momento entre
 * dos periodos académicos con datos exclusivamente agregados por cohorte.
 */
export function ComparativeReportTab({ periods, programs, initialProgramId }: ComparativeReportTabProps) {
  const [programId, setProgramId] = useState<number>(initialProgramId)
  // `periods` viene ordenado del más reciente al más antiguo; por defecto A = anterior y B = actual.
  const [periodAId, setPeriodAId] = useState<number>(periods[1]?.id ?? periods[0]?.id ?? 0)
  const [periodBId, setPeriodBId] = useState<number>(periods[0]?.id ?? 0)
  const [momentNumber, setMomentNumber] = useState<number>(1)

  const moments = useLoad(async () => {
    if (periods.length < 2) return [1]
    const [listA, listB] = await Promise.all([momentService.list(periodAId), momentService.list(periodBId)])
    const numbers = new Set<number>()
    for (const item of [...listA, ...listB]) {
      if (item.programId === programId) numbers.add(item.number)
    }
    if (numbers.size === 0) {
      return [1, 2, 3]
    }
    return [...numbers].sort((a, b) => a - b)
  }, [programId, periodAId, periodBId, periods.length])

  if (periods.length < 2) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Comparativo no disponible</CardTitle>
          <CardDescription>
            Se necesitan al menos dos periodos académicos registrados en el sistema para comparar cohortes.
          </CardDescription>
        </CardHeader>
      </Card>
    )
  }

  const availableNumbers = moments.state.kind === 'success' ? moments.state.data : [1, 2, 3]
  const activeMomentNumber = availableNumbers.includes(momentNumber) ? momentNumber : availableNumbers[0]
  const samePeriod = periodAId === periodBId

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardContent className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
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

          <SelectField
            label="Momento"
            value={String(activeMomentNumber)}
            onChange={(event) => setMomentNumber(Number(event.target.value))}
          >
            {availableNumbers.map((num) => (
              <option key={num} value={String(num)}>
                Momento {num}
              </option>
            ))}
          </SelectField>

          <SelectField
            label="Periodo base (A)"
            value={String(periodAId)}
            onChange={(event) => setPeriodAId(Number(event.target.value))}
          >
            {periods.map((period) => (
              <option key={period.id} value={String(period.id)}>
                {period.code}
              </option>
            ))}
          </SelectField>

          <SelectField
            label="Frente a periodo (B)"
            value={String(periodBId)}
            onChange={(event) => setPeriodBId(Number(event.target.value))}
          >
            {periods.map((period) => (
              <option key={period.id} value={String(period.id)}>
                {period.code}
              </option>
            ))}
          </SelectField>
        </CardContent>
      </Card>

      {samePeriod ? (
        <Card className="bg-muted/40">
          <CardContent className="py-6 text-sm">
            Debe elegir dos periodos académicos distintos para comparar.
          </CardContent>
        </Card>
      ) : (
        <ComparativeReportLoader
          programId={programId}
          momentNumber={activeMomentNumber}
          periodAId={periodAId}
          periodBId={periodBId}
        />
      )}
    </div>
  )
}

function ComparativeReportLoader({
  programId,
  momentNumber,
  periodAId,
  periodBId,
}: {
  programId: number
  momentNumber: number
  periodAId: number
  periodBId: number
}) {
  const report = useLoad(
    () => reportService.comparative({ programId, momentNumber, periodAId, periodBId }),
    [programId, momentNumber, periodAId, periodBId],
  )

  if (report.state.kind !== 'success') {
    return <LoadStatus state={report.state} onRetry={report.reload} label="Generando el reporte comparativo…" />
  }

  return <ComparativeReportView data={report.state.data} />
}

/**
 * Vista del reporte comparativo (wireframe `AD-08`): gráfica de barras pareadas por RAP,
 * tabla de variación porcentual y comparativa de distribución de notas.
 */
export function ComparativeReportView({ data }: { data: ComparativeReportData }) {
  const codeA = data.periodA.period.code
  const codeB = data.periodB.period.code

  return (
    <div className="flex flex-col gap-6">
      <Card className="bg-muted/40">
        <CardContent className="flex flex-wrap items-center justify-between gap-4 text-sm">
          <div className="flex flex-wrap items-center gap-3">
            <Badge variant="outline">Datos agregados por cohorte</Badge>
            <span className="font-medium">
              {data.program.name} · Momento {data.momentNumber} · {codeA} frente a {codeB}
            </span>
          </div>
          <span className="text-muted-foreground">
            {codeA}: {data.periodA.subject} · {codeB}: {data.periodB.subject}
          </span>
        </CardContent>
      </Card>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardHeader>
            <CardDescription>Promedio general ({codeA} vs. {codeB})</CardDescription>
            <div className="flex flex-wrap items-baseline gap-3">
              <p className="text-2xl font-bold" aria-label="Comparación de promedio general">
                {formatNullableScore(data.periodA.averageGrade)} → {formatNullableScore(data.periodB.averageGrade)}
              </p>
              <DeltaBadge delta={data.deltaAverageGrade} formatted={formatScoreDelta(data.deltaAverageGrade)} />
            </div>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">
            Variación en la nota individual media de la cohorte
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Tasa de aprobación ({codeA} vs. {codeB})</CardDescription>
            <div className="flex flex-wrap items-baseline gap-3">
              <p className="text-2xl font-bold" aria-label="Comparación de aprobación">
                {formatPercentage(data.periodA.passingPercentage)} → {formatPercentage(data.periodB.passingPercentage)}
              </p>
              <DeltaBadge
                delta={data.deltaPassingPercentage}
                formatted={formatPercentageDelta(data.deltaPassingPercentage)}
              />
            </div>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">
            Porcentaje de estudiantes calificados con nota ≥ 3,00
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Muestra evaluada por periodo</CardDescription>
            <p className="text-lg font-bold">
              {codeA}: {data.periodA.gradedStudents} est. · {codeB}: {data.periodB.gradedStudents} est.
            </p>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">
            {codeA}: {data.periodA.gradedGroups} grupos · {codeB}: {data.periodB.gradedGroups} grupos calificados
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_24rem]">
        <Card>
          <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-4">
            <div>
              <CardTitle>Desempeño promedio por RAP</CardTitle>
              <CardDescription>
                Porcentaje de logro respecto al máximo de cada RAP entre {codeA} y {codeB}.
              </CardDescription>
            </div>
            <div className="flex items-center gap-4 text-xs">
              <span className="flex items-center gap-1.5">
                <span className="bg-muted-foreground/45 inline-block size-3 rounded-xs" aria-hidden />
                {codeA}
              </span>
              <span className="flex items-center gap-1.5">
                <span className="bg-primary inline-block size-3 rounded-xs" aria-hidden />
                {codeB}
              </span>
            </div>
          </CardHeader>
          <CardContent>
            {data.learningOutcomes.length === 0 ? (
              <p className="text-muted-foreground text-sm">No hay RAP configurados para comparar.</p>
            ) : (
              <div className="flex flex-col gap-3" role="img" aria-label={`Gráfica comparativa por RAP: ${codeA} frente a ${codeB}`}>
                <div className="grid grid-cols-[3rem_1fr] gap-3">
                  <div className="text-muted-foreground flex h-52 flex-col justify-between text-right text-xs">
                    <span>100 %</span>
                    <span>75 %</span>
                    <span>50 %</span>
                    <span>25 %</span>
                    <span>0 %</span>
                  </div>
                  <div className="border-b border-l flex h-52 items-end justify-around gap-3 px-3 pt-2">
                    {data.learningOutcomes.map((outcome) => {
                      const pctA = clampPercentage(outcome.percentageA)
                      const pctB = clampPercentage(outcome.percentageB)
                      return (
                        <div
                          key={outcome.code}
                          className="flex h-full flex-1 flex-col items-center justify-end gap-1"
                          title={`${outcome.code}: ${codeA} ${formatPercentage(outcome.percentageA)} / ${codeB} ${formatPercentage(outcome.percentageB)}`}
                        >
                          <div className="flex h-full w-full max-w-14 items-end justify-center gap-1.5">
                            <div
                              className="bg-muted-foreground/45 w-1/2 rounded-t transition-all"
                              style={{ height: `${pctA}%` }}
                              aria-label={`${outcome.code} ${codeA}: ${formatPercentage(outcome.percentageA)}`}
                            />
                            <div
                              className="bg-primary w-1/2 rounded-t transition-all"
                              style={{ height: `${pctB}%` }}
                              aria-label={`${outcome.code} ${codeB}: ${formatPercentage(outcome.percentageB)}`}
                            />
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>
                <div className="grid grid-cols-[3rem_1fr] gap-3">
                  <div />
                  <div className="flex justify-around gap-3 px-3 text-center text-xs font-semibold">
                    {data.learningOutcomes.map((outcome) => (
                      <span key={outcome.code} className="flex-1" title={outcome.description}>
                        {outcome.code}
                      </span>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Variación por RAP</CardTitle>
            <CardDescription>Cambio en puntos porcentuales ({codeB} − {codeA}).</CardDescription>
          </CardHeader>
          <CardContent className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground border-b">
                <tr>
                  <th className="py-2 pr-3 font-medium">RAP</th>
                  <th className="py-2 pr-3 text-right font-medium">{codeA}</th>
                  <th className="py-2 pr-3 text-right font-medium">{codeB}</th>
                  <th className="py-2 text-right font-medium">Cambio</th>
                </tr>
              </thead>
              <tbody>
                {data.learningOutcomes.map((outcome) => (
                  <tr key={outcome.code} className="border-b last:border-0">
                    <td className="py-3 pr-3 font-semibold" title={outcome.description}>
                      {outcome.code}
                    </td>
                    <td className="py-3 pr-3 text-right">{formatPercentage(outcome.percentageA)}</td>
                    <td className="py-3 pr-3 text-right">{formatPercentage(outcome.percentageB)}</td>
                    <td className="py-3 text-right font-semibold">
                      {formatPercentageDelta(outcome.deltaPercentage)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Distribución comparada de calificaciones</CardTitle>
          <CardDescription>
            Proporción de estudiantes en cada nivel de desempeño entre {codeA} y {codeB}.
          </CardDescription>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-muted-foreground border-b">
              <tr>
                <th className="py-2 pr-4 font-medium">Nivel</th>
                <th className="py-2 pr-4 font-medium">Rango</th>
                <th className="py-2 pr-4 text-right font-medium">Estudiantes {codeA}</th>
                <th className="py-2 pr-4 text-right font-medium">% {codeA}</th>
                <th className="py-2 pr-4 text-right font-medium">Estudiantes {codeB}</th>
                <th className="py-2 text-right font-medium">% {codeB}</th>
              </tr>
            </thead>
            <tbody>
              {data.periodB.distribution.map((bucketB, index) => {
                const bucketA = data.periodA.distribution[index]
                return (
                  <tr key={bucketB.range} className="border-b last:border-0">
                    <td className="py-3 pr-4 font-semibold">{bucketB.label}</td>
                    <td className="text-muted-foreground py-3 pr-4">{bucketB.range.replaceAll('.', ',')}</td>
                    <td className="py-3 pr-4 text-right">{bucketA?.count ?? 0}</td>
                    <td className="py-3 pr-4 text-right">{formatPercentage(bucketA?.percentage ?? null)}</td>
                    <td className="py-3 pr-4 text-right">{bucketB.count}</td>
                    <td className="py-3 text-right">{formatPercentage(bucketB.percentage)}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  )
}

function DeltaBadge({ delta, formatted }: { delta: number | null; formatted: string }) {
  if (delta === null) return <Badge variant="outline">—</Badge>
  return <Badge variant={delta >= 0 ? 'default' : 'secondary'}>{formatted}</Badge>
}
