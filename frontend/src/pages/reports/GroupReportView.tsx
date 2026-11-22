import { PercentBars } from '@/components/charts/PercentBars'
import { StatTile } from '@/components/charts/StatTile'
import { LoadStatus } from '@/components/LoadStatus'
import { Badge } from '@/components/ui/badge'
import { formatScore } from '@/lib/scores'
import { useLoad } from '@/lib/useLoad'
import { reportService } from '@/services/api/reportService'
import type { GroupReportData } from '@/types/report'

import { ReportCard, ReportMeta, ReportTable } from './ReportParts'
import { formatGrade, formatPercentage } from './reportFormat'

/** Reporte grupal: el logro del grupo por RAP y la nota individual de cada integrante. */
export function GroupReportView({ groupId }: { groupId: number }) {
  const report = useLoad(() => reportService.group(groupId), [groupId])
  return (
    <>
      <LoadStatus state={report.state} onRetry={report.reload} label="Generando el reporte…" />
      {report.state.kind === 'success' && <GroupReport report={report.state.data} />}
    </>
  )
}

function GroupReport({ report }: { report: GroupReportData }) {
  const adjusted = report.members.filter((member) => member.adjusted).length
  return (
    <div className="flex flex-col gap-6">
      <ReportMeta {...report} />
      <p className="text-sm">
        <span className="font-semibold">{report.group.name}</span>
        {report.group.projectTitle && <> · {report.group.projectTitle}</>}
        <span className="text-muted-foreground"> · Docente: {report.group.teacherName}</span>
      </p>

      <div className="grid gap-3 sm:grid-cols-3">
        <StatTile
          label="Nota del grupo"
          value={formatGrade(report.groupGrade)}
          detail={`de ${formatScore(report.moment.maxScore)} · ${formatPercentage(report.percentage)}`}
        />
        <StatTile
          label="Evaluaciones enviadas"
          value={String(report.submittedEvaluations)}
          detail={`${report.pendingEvaluations} pendientes`}
        />
        <StatTile
          label="Integrantes"
          value={String(report.members.length)}
          detail={`${adjusted} con ajuste individual`}
        />
      </div>

      {report.groupGrade === null ? (
        <p className="text-muted-foreground text-sm">
          El grupo todavía no tiene evaluaciones enviadas que cuenten en la nota.
        </p>
      ) : (
        <>
          <ReportCard title="Logro del grupo por RAP" description="Puntaje del grupo sobre el máximo de cada RAP.">
            <PercentBars
              label="Logro del grupo por RAP"
              items={report.learningOutcomes.map((outcome) => ({
                key: outcome.id,
                label: outcome.code,
                percentage: outcome.percentage,
                value: formatPercentage(outcome.percentage),
                detail: `${formatScore(outcome.score)} de ${formatScore(outcome.maxScore)}`,
              }))}
            />
          </ReportCard>

          <ReportCard title="Integrantes" description="Nota individual y logro de cada integrante en cada RAP.">
            <ReportTable
              label="Integrantes"
              columns={[
                { header: 'Código' },
                { header: 'Integrante' },
                { header: 'Nota', numeric: true },
                ...report.learningOutcomes.map((outcome) => ({ header: outcome.code, numeric: true })),
                { header: 'Ajuste' },
              ]}
              rows={report.members.map((member) => ({
                key: member.studentId,
                cells: [
                  member.studentCode,
                  member.fullName,
                  formatGrade(member.individualGrade),
                  ...report.learningOutcomes.map((outcome) => {
                    const achieved = member.learningOutcomes.find((item) => item.id === outcome.id)
                    return formatPercentage(achieved?.percentage ?? null)
                  }),
                  member.adjusted ? <Badge variant="secondary">Con ajuste</Badge> : '',
                ],
              }))}
            />
          </ReportCard>
        </>
      )}
    </div>
  )
}
