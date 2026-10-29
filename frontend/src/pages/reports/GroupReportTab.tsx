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
import type { GroupReportData } from '@/types/report'

import { clampPercentage, formatNullableScore, formatPercentage } from './reportFormat'

/**
 * Pestaña 2 — Reporte por Grupo (`GET /api/reports/groups/{groupId}`):
 * desempeño del proyecto de aula por RAP y notas individuales de sus integrantes.
 */
export function GroupReportTab({ momentId }: { momentId: number }) {
  const groups = useLoad(() => groupService.list(momentId), [momentId])

  if (groups.state.kind !== 'success') {
    return <LoadStatus state={groups.state} onRetry={groups.reload} label="Cargando los grupos del momento…" />
  }

  if (groups.state.data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Sin grupos en este momento</CardTitle>
          <CardDescription>No hay grupos habilitados en este momento dentro de tu alcance.</CardDescription>
        </CardHeader>
      </Card>
    )
  }

  return <GroupReportSelector key={momentId} groups={groups.state.data} />
}

function GroupReportSelector({ groups }: { groups: Group[] }) {
  const [groupId, setGroupId] = useState<number>(groups[0].id)
  const selectedId = groups.some((group) => group.id === groupId) ? groupId : groups[0].id
  const report = useLoad(() => reportService.group(selectedId), [selectedId])

  return (
    <div className="flex flex-col gap-6">
      <Card>
        <CardContent className="max-w-md">
          <SelectField
            label="Grupo"
            value={String(selectedId)}
            onChange={(event) => setGroupId(Number(event.target.value))}
          >
            {groups.map((group) => (
              <option key={group.id} value={String(group.id)}>
                {group.name}
                {group.projectTitle ? ` — ${group.projectTitle}` : ''} ({group.teacher.fullName})
              </option>
            ))}
          </SelectField>
        </CardContent>
      </Card>

      {report.state.kind !== 'success' ? (
        <LoadStatus state={report.state} onRetry={report.reload} label="Generando el reporte del grupo…" />
      ) : (
        <GroupReportView data={report.state.data} />
      )}
    </div>
  )
}

/** Vista del reporte de un grupo con gráfica de logro por RAP y matriz de integrantes. */
export function GroupReportView({ data }: { data: GroupReportData }) {
  const adjustedCount = data.members.filter((member) => member.adjusted).length
  const rapCodes = data.learningOutcomes.map((outcome) => outcome.code)

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
            <CardDescription>Nota grupal ponderada</CardDescription>
            <div className="flex items-baseline gap-3">
              <p className="text-3xl font-bold" aria-label="Nota grupal del reporte">
                {formatNullableScore(data.groupGrade)}
              </p>
              <span className="text-muted-foreground text-sm">de {formatScore(data.moment.maxScore)}</span>
            </div>
          </CardHeader>
          <CardContent>
            <Badge variant="outline">Logro: {formatPercentage(data.percentage)}</Badge>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Proyecto de aula</CardDescription>
            <CardTitle className="text-lg">{data.group.name}</CardTitle>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">
            <p className="text-foreground font-medium">{data.group.projectTitle ?? 'Sin título registrado'}</p>
            {data.group.teacherName && <p>Docente: {data.group.teacherName}</p>}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardDescription>Integrantes del grupo</CardDescription>
            <p className="text-3xl font-bold">{data.members.length}</p>
          </CardHeader>
          <CardContent className="text-muted-foreground text-sm">
            <p>
              {adjustedCount === 0
                ? 'Todos conservan la nota base del grupo'
                : `${adjustedCount} ${adjustedCount === 1 ? 'integrante con ajuste' : 'integrantes con ajuste'} individual`}
            </p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Desempeño del grupo por RAP</CardTitle>
          <CardDescription>Porcentaje de logro alcanzado por el grupo en cada resultado de aprendizaje.</CardDescription>
        </CardHeader>
        <CardContent>
          {data.learningOutcomes.length === 0 ? (
            <p className="text-muted-foreground text-sm">Este momento no tiene RAP configurados.</p>
          ) : (
            <ul className="divide-y" aria-label="Desempeño grupal por RAP">
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
                        {formatNullableScore(outcome.score)} / {formatScore(outcome.maxScore)} (
                        {formatPercentage(outcome.percentage)})
                      </span>
                    </div>
                    <div
                      role="meter"
                      aria-label={`${outcome.code} grupal`}
                      aria-valuemin={0}
                      aria-valuemax={outcome.maxScore}
                      aria-valuenow={outcome.score ?? 0}
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
          <CardTitle>Notas individuales de los integrantes</CardTitle>
          <CardDescription>
            Desglose por RAP de cada integrante. Se marca «Ajustado» cuando un evaluador asignó un nivel individual en
            algún criterio (ADR-018).
          </CardDescription>
        </CardHeader>
        <CardContent className="overflow-x-auto">
          {data.members.length === 0 ? (
            <p className="text-muted-foreground text-sm">Este grupo aún no tiene estudiantes inscritos.</p>
          ) : (
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground border-b">
                <tr>
                  <th className="py-2 pr-4 font-medium">Estudiante</th>
                  <th className="py-2 pr-4 font-medium">Código</th>
                  {rapCodes.map((code) => (
                    <th key={code} className="py-2 pr-4 text-right font-medium">
                      {code}
                    </th>
                  ))}
                  <th className="py-2 pr-4 font-medium">Estado</th>
                  <th className="py-2 text-right font-medium">Nota final</th>
                </tr>
              </thead>
              <tbody>
                {data.members.map((member) => (
                  <tr key={member.studentId} className="border-b last:border-0">
                    <td className="py-3 pr-4 font-medium">{member.fullName}</td>
                    <td className="text-muted-foreground py-3 pr-4">{member.studentCode}</td>
                    {member.learningOutcomes.map((outcome) => (
                      <td key={outcome.id} className="py-3 pr-4 text-right">
                        {formatNullableScore(outcome.score)}
                      </td>
                    ))}
                    <td className="py-3 pr-4">
                      {member.adjusted ? (
                        <Badge variant="secondary">Ajustado</Badge>
                      ) : (
                        <span className="text-muted-foreground text-xs">Base grupal</span>
                      )}
                    </td>
                    <td className="py-3 text-right font-semibold">{formatNullableScore(member.individualGrade)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
