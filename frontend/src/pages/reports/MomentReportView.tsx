import { PercentBars } from '@/components/charts/PercentBars'
import { StatTile } from '@/components/charts/StatTile'
import { LoadStatus } from '@/components/LoadStatus'
import { formatScore } from '@/lib/scores'
import { useLoad } from '@/lib/useLoad'
import { reportService } from '@/services/api/reportService'
import type { MomentReportData } from '@/types/report'

import { ReportCard, ReportMeta, ReportTable } from './ReportParts'
import { formatGrade, formatPercentage } from './reportFormat'

/** Reporte consolidado de un momento: promedios por RAP y distribución de notas. */
export function MomentReportView({ momentId }: { momentId: number }) {
  const report = useLoad(() => reportService.moment(momentId), [momentId])
  return (
    <>
      <LoadStatus state={report.state} onRetry={report.reload} label="Generando el reporte…" />
      {report.state.kind === 'success' && <MomentReport report={report.state.data} />}
    </>
  )
}

function MomentReport({ report }: { report: MomentReportData }) {
  return (
    <div className="flex flex-col gap-6">
      <ReportMeta {...report} />
      {report.gradedStudents === 0 ? (
        <p className="text-muted-foreground text-sm">
          Todavía no hay estudiantes calificados en este momento ({report.totalStudents} en {report.totalGroups}{' '}
          grupos).
        </p>
      ) : (
        <>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <StatTile
              label="Nota promedio"
              value={formatGrade(report.averageGrade)}
              detail={`de ${formatScore(report.moment.maxScore)} · grupos: ${formatGrade(report.averageGroupGrade)}`}
            />
            <StatTile
              label="Aprobados"
              value={formatPercentage(report.passingPercentage)}
              detail={`${report.passingCount} de ${report.gradedStudents} estudiantes calificados`}
            />
            <StatTile
              label="Estudiantes calificados"
              value={`${report.gradedStudents} de ${report.totalStudents}`}
              detail={`${report.gradedGroups} de ${report.totalGroups} grupos con nota`}
            />
            <StatTile
              label="Nota mínima y máxima"
              value={`${formatGrade(report.minGrade)} – ${formatGrade(report.maxGrade)}`}
            />
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <ReportCard title="Logro promedio por RAP" description="Puntaje promedio sobre el máximo de cada RAP.">
              <PercentBars
                label="Logro promedio por RAP"
                items={report.learningOutcomes.map((outcome) => ({
                  key: outcome.id,
                  label: outcome.code,
                  percentage: outcome.percentage,
                  value: formatPercentage(outcome.percentage),
                  detail: `${formatScore(outcome.averageScore)} de ${formatScore(outcome.maxScore)}`,
                }))}
              />
            </ReportCard>
            <ReportCard title="Distribución de notas" description="Estudiantes calificados en cada franja de la nota.">
              <PercentBars
                label="Distribución de notas"
                items={report.distribution.map((bucket) => ({
                  key: bucket.range,
                  label: bucket.label,
                  sublabel: `${formatScore(bucket.min)} – ${formatScore(bucket.max)}`,
                  percentage: bucket.percentage,
                  value: `${bucket.count} · ${formatPercentage(bucket.percentage)}`,
                }))}
              />
            </ReportCard>
          </div>

          <ReportCard title="Detalle por RAP">
            <ReportTable
              label="Detalle por RAP"
              columns={[
                { header: 'RAP' },
                { header: 'Descripción' },
                { header: 'Promedio', numeric: true },
                { header: 'Máximo', numeric: true },
                { header: 'Logro', numeric: true },
              ]}
              rows={report.learningOutcomes.map((outcome) => ({
                key: outcome.id,
                cells: [
                  <span className="font-medium">{outcome.code}</span>,
                  outcome.description,
                  formatScore(outcome.averageScore),
                  formatScore(outcome.maxScore),
                  formatPercentage(outcome.percentage),
                ],
              }))}
            />
          </ReportCard>
        </>
      )}
    </div>
  )
}
