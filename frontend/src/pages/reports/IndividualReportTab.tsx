import { useState } from 'react'

import { LoadStatus } from '@/components/LoadStatus'
import { SelectField } from '@/components/forms/SelectField'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatScore } from '@/lib/scores'
import { useLoad } from '@/lib/useLoad'
import { groupService } from '@/services/api/groupService'
import { reportService } from '@/services/api/reportService'
import type { Group } from '@/types/group'
import type { IndividualReportData } from '@/types/report'

import { clampPercentage, formatNullableScore, formatPercentage } from './reportFormat'

interface StudentOption {
  id: number
  fullName: string
  studentCode: string | null
  groupName: string
}

/**Extrae los estudiantes de los grupos del momento, ordenados alfabéticamente. */
function extractStudents(groups: Group[]): StudentOption[] {
  const map = new Map<number, StudentOption>()
  for (const group of groups) {
    for (const member of group.members) {
      if (!map.has(member.id)) {
        map.set(member.id, {
          id: member.id,
          fullName: member.fullName,
          studentCode: member.studentCode,
          groupName: group.name,
        })
      }
    }
  }
  return [...map.values()].sort((a, b) => a.fullName.localeCompare(b.fullName, 'es'))
}

/**
 * Pestaña 1 — Reporte Individual (`GET /api/reports/individual?momentId=&studentId=`):
 * desempeño de un estudiante por RAP comparado con la nota base de su grupo.
 */
export function IndividualReportTab({ momentId }: { momentId: number }) {
  const groups = useLoad(() => groupService.list(momentId), [momentId])

  if (groups.state.kind !== 'success') {
    return <LoadStatus state={groups.state} onRetry={groups.reload} label="Cargando los estudiantes del momento…" />
  }

  const students = extractStudents(groups.state.data)
  if (students.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Sin estudiantes en grupos</CardTitle>
          <CardDescription>
            No hay estudiantes inscritos en grupos de este momento dentro de tu alcance.
          </CardDescription>
        </CardHeader>
      </Card>
    )
  }

  return <IndividualStudentSelector key={momentId} momentId={momentId} students={students} />
}

function IndividualStudentSelector({ momentId, students }: { momentId: number; students: StudentOption[] }) {
  const [studentId, setStudentId] = useState<number>(students[0].id)
  const selectedId = students.some((student) => student.id === studentId) ? studentId : students[0].id
  const report = useLoad(() => reportService.individual(momentId, selectedId), [momentId, selectedId])

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardContent className="max-w-md">
          <SelectField
            label="Estudiante"
            value={String(selectedId)}
            onChange={(event) => setStudentId(Number(event.target.value))}
          >
            {students.map((student) => (
              <option key={student.id} value={String(student.id)}>
                {student.fullName} — {student.groupName}
                {student.studentCode ? ` (${student.studentCode})` : ''}
              </option>
            ))}
          </SelectField>
        </CardContent>
      </Card>

      {report.state.kind !== 'success' ? (
        <LoadStatus state={report.state} onRetry={report.reload} label="Generando el reporte individual…" />
      ) : (
        <IndividualReportView data={report.state.data} />
      )}
    </div>
  )
}

/** Vista del reporte individual con resumen, gráfica de barras por RAP y tabla detallada. */
export function IndividualReportView({ data }: { data: IndividualReportData }) {
  const hasAdjustedGrade =
    data.individualGrade !== null && data.groupGrade !== null && data.individualGrade !== data.groupGrade

  return (
    <div className="flex flex-col gap-6">
      <Card className="bg-muted/40">
        <CardContent className="flex flex-wrap items-center justify-between gap-4 text-sm">
          <div className="flex flex-wrap items-center gap-3">
            <Badge variant={data.final ? 'default' : 'secondary'}>
              {data.final ? 'Notas definitivas' : 'Vista previa'}
            </Badge>
            <span className="font-medium">
              {data.program.name} · {data.moment.name} ({data.moment.subject}) · {data.period.code}
            </span>
          </div>
          <span className="text-muted-foreground">
            Evaluaciones: {data.submittedEvaluations} {data.submittedEvaluations === 1 ? 'enviada' : 'enviadas'} ·{' '}
            {data.pendingEvaluations} {data.pendingEvaluations === 1 ? 'pendiente' : 'pendientes'}
          </span>
        </CardContent>
      </Card>

      <div className="grid gap-4 sm:grid-cols-3">
        <Card>
          <CardHeader>
            <CardDescription>Nota individual definitiva</CardDescription>
            <div className="flex items-baseline gap-3">
              <p className="text-3xl font-bold" aria-label="Nota individual">
                {formatNullableScore(data.individualGrade)}
              </p>
              <span className="text-muted-foreground text-sm">de {formatScore(data.moment.maxScore)}</span>
            </div>
          </CardHeader>
          <CardContent className="flex flex-wrap items-center gap-2 text-sm">
            <Badge variant="outline">Logro: {formatPercentage(data.percentage)}</Badge>
            {hasAdjustedGrade && <Badge variant="secondary">Ajuste individual</Badge>}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Nota base del grupo</CardDescription>
            <div className="flex items-baseline gap-3">
              <p className="text-3xl font-bold" aria-label="Nota del grupo">
                {formatNullableScore(data.groupGrade)}
              </p>
              <span className="text-muted-foreground text-sm">de {formatScore(data.moment.maxScore)}</span>
            </div>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">
            <p className="text-foreground font-medium">
              {data.group.name}
              {data.group.projectTitle ? ` · ${data.group.projectTitle}` : ''}
            </p>
            {data.group.teacherName && <p>Docente: {data.group.teacherName}</p>}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Datos del estudiante</CardDescription>
            <CardTitle className="text-lg">{data.student.fullName}</CardTitle>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">
            <p>Código: {data.student.studentCode}</p>
            <p>Semestre actual: {data.student.currentSemester}</p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex flex-row flex-wrap items-center justify-between gap-4">
          <div>
            <CardTitle>Gráfica de desempeño por RAP</CardTitle>
            <CardDescription>
              Comparación del logro individual del estudiante frente al puntaje de su grupo en cada RAP.
            </CardDescription>
          </div>
          <div className="flex items-center gap-4 text-xs">
            <span className="flex items-center gap-1.5">
              <span className="bg-primary inline-block size-3 rounded-xs" aria-hidden />
              Individual
            </span>
            <span className="flex items-center gap-1.5">
              <span className="bg-muted-foreground/40 inline-block size-3 rounded-xs" aria-hidden />
              Grupo
            </span>
          </div>
        </CardHeader>
        <CardContent>
          {data.learningOutcomes.length === 0 ? (
            <p className="text-muted-foreground text-sm">Este momento no tiene RAP configurados.</p>
          ) : (
            <ul className="divide-y" aria-label="Barras de desempeño por RAP">
              {data.learningOutcomes.map((outcome) => {
                const indPct = clampPercentage(outcome.percentage)
                const grpPct =
                  outcome.groupScore !== null && outcome.maxScore > 0
                    ? clampPercentage((outcome.groupScore / outcome.maxScore) * 100)
                    : 0
                const adjusted =
                  outcome.score !== null && outcome.groupScore !== null && outcome.score !== outcome.groupScore
                return (
                  <li key={outcome.id} className="flex flex-col gap-2 py-3 text-sm">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="flex items-center gap-2">
                        <span className="font-semibold">{outcome.code}</span>
                        <span className="text-muted-foreground">{outcome.description}</span>
                        {adjusted && <Badge variant="secondary">Ajustado</Badge>}
                      </div>
                      <span className="font-medium whitespace-nowrap">
                        {formatNullableScore(outcome.score)} / {formatScore(outcome.maxScore)} (
                        {formatPercentage(outcome.percentage)})
                      </span>
                    </div>
                    <div className="flex flex-col gap-1.5">
                      <div
                        role="meter"
                        aria-label={`${outcome.code} individual`}
                        aria-valuemin={0}
                        aria-valuemax={outcome.maxScore}
                        aria-valuenow={outcome.score ?? 0}
                        className="bg-muted h-2.5 overflow-hidden rounded-full"
                      >
                        <div className="bg-primary h-full rounded-full transition-all" style={{ width: `${indPct}%` }} />
                      </div>
                      <div
                        className="bg-muted h-1.5 overflow-hidden rounded-full"
                        title={`Grupo: ${formatNullableScore(outcome.groupScore)} / ${formatScore(outcome.maxScore)}`}
                      >
                        <div
                          className="bg-muted-foreground/40 h-full rounded-full transition-all"
                          style={{ width: `${grpPct}%` }}
                        />
                      </div>
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
          <CardTitle>Detalle numérico por RAP</CardTitle>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="text-muted-foreground border-b">
              <tr>
                <th className="py-2 pr-4 font-medium">RAP</th>
                <th className="py-2 pr-4 font-medium">Descripción</th>
                <th className="py-2 pr-4 text-right font-medium">Nota grupal</th>
                <th className="py-2 pr-4 text-right font-medium">Nota individual</th>
                <th className="py-2 pr-4 text-right font-medium">Máximo</th>
                <th className="py-2 text-right font-medium">% Logro</th>
              </tr>
            </thead>
            <tbody>
              {data.learningOutcomes.map((outcome) => (
                <tr key={outcome.id} className="border-b last:border-0">
                  <td className="py-3 pr-4 font-semibold">{outcome.code}</td>
                  <td className="py-3 pr-4">{outcome.description}</td>
                  <td className="py-3 pr-4 text-right">{formatNullableScore(outcome.groupScore)}</td>
                  <td className="py-3 pr-4 text-right font-semibold">{formatNullableScore(outcome.score)}</td>
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
