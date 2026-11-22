import { PercentBars } from '@/components/charts/PercentBars'
import { StatTile } from '@/components/charts/StatTile'
import { LoadStatus } from '@/components/LoadStatus'
import { formatScore } from '@/lib/scores'
import { useLoad } from '@/lib/useLoad'
import { reportService } from '@/services/api/reportService'
import type { IndividualReportData } from '@/types/report'

import { ReportCard, ReportMeta, ReportTable } from './ReportParts'
import { formatGrade, formatPercentage } from './reportFormat'

/** Reporte individual: el desempeño de un estudiante en el momento, en total y por RAP. */
export function IndividualReportView({ momentId, studentId }: { momentId: number; studentId: number }) {
  const report = useLoad(() => reportService.individual(momentId, studentId), [momentId, studentId])
  return (
    <>
      <LoadStatus state={report.state} onRetry={report.reload} label="Generando el reporte…" />
      {report.state.kind === 'success' && <IndividualReport report={report.state.data} />}
    </>
  )
}

function IndividualReport({ report }: { report: IndividualReportData }) {
  const max = formatScore(report.moment.maxScore)
  return (
    <div className="flex flex-col gap-6">
      <ReportMeta {...report} />
      <p className="text-sm">
        <span className="font-semibold">{report.student.fullName}</span>
        <span className="text-muted-foreground">
          {' '}
          · Código {report.student.studentCode} · Semestre {report.student.currentSemester} · {report.group.name},
          docente {report.group.teacherName}
        </span>
      </p>

      <div className="grid gap-3 sm:grid-cols-3">
        <StatTile
          label="Nota individual"
          value={formatGrade(report.individualGrade)}
          detail={`de ${max} · ${formatPercentage(report.percentage)}`}
        />
        <StatTile label="Nota del grupo" value={formatGrade(report.groupGrade)} detail={`de ${max}`} />
        <StatTile
          label="Evaluaciones enviadas"
          value={String(report.submittedEvaluations)}
          detail={`${report.pendingEvaluations} pendientes`}
        />
      </div>

      {report.individualGrade === null ? (
        <p className="text-muted-foreground text-sm">
          El grupo todavía no tiene evaluaciones enviadas que cuenten en la nota.
        </p>
      ) : (
        <>
          <ReportCard title="Logro por RAP" description="Puntaje del estudiante sobre el máximo de cada RAP.">
            <PercentBars
              label="Logro por RAP"
              items={report.learningOutcomes.map((outcome) => ({
                key: outcome.id,
                label: outcome.code,
                percentage: outcome.percentage,
                value: formatPercentage(outcome.percentage),
                detail: `${formatScore(outcome.score)} de ${formatScore(outcome.maxScore)} (grupo: ${formatScore(
                  outcome.groupScore,
                )})`,
              }))}
            />
          </ReportCard>
          <ReportCard
            title="Detalle por RAP"
            description="Su puntaje frente al del grupo antes de los ajustes individuales."
          >
            <ReportTable
              label="Detalle por RAP"
              columns={[
                { header: 'RAP' },
                { header: 'Descripción' },
                { header: 'Puntaje', numeric: true },
                { header: 'Del grupo', numeric: true },
                { header: 'Máximo', numeric: true },
                { header: 'Logro', numeric: true },
              ]}
              rows={report.learningOutcomes.map((outcome) => ({
                key: outcome.id,
                cells: [
                  <span className="font-medium">{outcome.code}</span>,
                  outcome.description ?? '',
                  formatScore(outcome.score),
                  formatScore(outcome.groupScore),
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
