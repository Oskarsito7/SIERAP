import { LoadStatus } from '@/components/LoadStatus'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatScore } from '@/lib/scores'
import { useLoad } from '@/lib/useLoad'
import { reportService } from '@/services/api/reportService'
import type { MomentReportData } from '@/types/report'

import { clampPercentage, formatNullableScore, formatPercentage } from './reportFormat'

/**
 * Pestaña 3 — Reporte Consolidado por Momento y Programa
 * (`GET /api/reports/moments/{momentId}`): promedios por RAP, aprobación y distribución
 * de notas en los cuatro niveles de la rúbrica institucional.
 */
export function MomentReportTab({ momentId }: { momentId: number }) {
  const report = useLoad(() => reportService.moment(momentId), [momentId])

  if (report.state.kind !== 'success') {
    return <LoadStatus state={report.state} onRetry={report.reload} label="Generando el reporte consolidado…" />
  }

  return <MomentReportView data={report.state.data} />
}

/** Vista del reporte consolidado del momento con gráficas de RAP y distribución de notas. */
export function MomentReportView({ data }: { data: MomentReportData }) {
  return (
    <div className="flex flex-col gap-6">
      <Card className="bg-muted/40">
        <CardContent className="flex flex-wrap items-center justify-between gap-4 text-sm">
          <div className="flex flex-wrap items-center gap-3">
            <Badge variant={data.final ? 'default' : 'secondary'}>
              {data.final ? 'Resultados definitivos' : 'Vista previa'}
            </Badge>
            <span className="font-medium">
              {data.program.name} · {data.moment.name} ({data.moment.subject}) · {data.period.code}
            </span>
          </div>
          <span className="text-muted-foreground">
            Grupos calificados: {data.gradedGroups} de {data.totalGroups} · Estudiantes calificados:{' '}
            {data.gradedStudents} de {data.totalStudents}
          </span>
        </CardContent>
      </Card>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader>
            <CardDescription>Promedio general (estudiantes)</CardDescription>
            <div className="flex items-baseline gap-2">
              <p className="text-3xl font-bold" aria-label="Promedio general del momento">
                {formatNullableScore(data.averageGrade)}
              </p>
              <span className="text-muted-foreground text-sm">de {formatScore(data.moment.maxScore)}</span>
            </div>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">
            Promedio grupal: {formatNullableScore(data.averageGroupGrade)}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Tasa de aprobación (≥ 3,00)</CardDescription>
            <p className="text-3xl font-bold" aria-label="Tasa de aprobación">
              {formatPercentage(data.passingPercentage)}
            </p>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">
            {data.passingCount} de {data.gradedStudents} estudiantes calificados
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Cobertura de evaluación</CardDescription>
            <p className="text-3xl font-bold">
              {data.gradedGroups} / {data.totalGroups}
            </p>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">
            Grupos con nota ({data.gradedStudents} de {data.totalStudents} estudiantes)
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Rango de calificaciones</CardDescription>
            <p className="text-2xl font-bold">
              {formatNullableScore(data.minGrade)} – {formatNullableScore(data.maxGrade)}
            </p>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">Nota mínima y máxima registrada</CardContent>
        </Card>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Promedio de logro por RAP</CardTitle>
            <CardDescription>
              Promedio de las notas individuales de los estudiantes calificados en cada RAP.
            </CardDescription>
          </CardHeader>
          <CardContent>
            {data.learningOutcomes.length === 0 ? (
              <p className="text-muted-foreground text-sm">Este momento no tiene RAP configurados.</p>
            ) : (
              <ul className="divide-y" aria-label="Promedio por RAP del momento">
                {data.learningOutcomes.map((outcome) => {
                  const pct = clampPercentage(outcome.percentage)
                  return (
                    <li key={outcome.id} className="flex flex-col gap-2 py-3 text-sm">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <div>
                          <span className="font-semibold">{outcome.code}</span>{' '}
                          <span className="text-muted-foreground">{outcome.description}</span>
                        </div>
                        <span className="font-medium whitespace-nowrap">
                          {formatNullableScore(outcome.averageScore)} / {formatScore(outcome.maxScore)} (
                          {formatPercentage(outcome.percentage)})
                        </span>
                      </div>
                      <div
                        role="meter"
                        aria-label={`${outcome.code} promedio`}
                        aria-valuemin={0}
                        aria-valuemax={outcome.maxScore}
                        aria-valuenow={outcome.averageScore ?? 0}
                        className="bg-muted h-2.5 overflow-hidden rounded-full"
                      >
                        <div className="bg-primary h-full rounded-full transition-all" style={{ width: `${pct}%` }} />
                      </div>
                    </li>
                  )
                })}
              </ul>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Distribución de notas</CardTitle>
            <CardDescription>
              Clasificación de los estudiantes calificados según los niveles de desempeño de la rúbrica institucional.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <ul className="divide-y" aria-label="Distribución de notas del momento">
              {data.distribution.map((bucket) => {
                const pct = clampPercentage(bucket.percentage)
                return (
                  <li key={bucket.range} className="flex flex-col gap-2 py-3 text-sm">
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold">{bucket.label}</span>
                        <span className="text-muted-foreground text-xs">({bucket.range.replaceAll('.', ',')})</span>
                      </div>
                      <span className="font-medium">
                        {bucket.count} {bucket.count === 1 ? 'estudiante' : 'estudiantes'} (
                        {formatPercentage(bucket.percentage)})
                      </span>
                    </div>
                    <div
                      role="meter"
                      aria-label={`Nivel ${bucket.label}`}
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-valuenow={bucket.percentage ?? 0}
                      className="bg-muted h-2.5 overflow-hidden rounded-full"
                    >
                      <div className="bg-primary h-full rounded-full transition-all" style={{ width: `${pct}%` }} />
                    </div>
                  </li>
                )
              })}
            </ul>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Tabla consolidada por RAP</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-muted-foreground border-b">
              <tr>
                <th className="py-2 pr-4 font-medium">RAP</th>
                <th className="py-2 pr-4 font-medium">Descripción</th>
                <th className="py-2 pr-4 text-right font-medium">Promedio</th>
                <th className="py-2 pr-4 text-right font-medium">Máximo</th>
                <th className="py-2 text-right font-medium">% Logro</th>
              </tr>
            </thead>
            <tbody>
              {data.learningOutcomes.map((outcome) => (
                <tr key={outcome.id} className="border-b last:border-0">
                  <td className="py-3 pr-4 font-semibold">{outcome.code}</td>
                  <td className="py-3 pr-4">{outcome.description}</td>
                  <td className="py-3 pr-4 text-right font-semibold">{formatNullableScore(outcome.averageScore)}</td>
                  <td className="py-3 pr-4 text-right">{formatScore(outcome.maxScore)}</td>
                  <td className="py-3 text-right">{formatPercentage(outcome.percentage)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </CardContent>
      </Card>
    </div>
  )
}
