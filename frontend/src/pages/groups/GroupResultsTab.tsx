import { LoadStatus } from '@/components/LoadStatus'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { formatDate } from '@/lib/dates'
import { formatScore } from '@/lib/scores'
import { useLoad } from '@/lib/useLoad'
import { evaluationService } from '@/services/api/evaluationService'
import { resultsService } from '@/services/api/resultsService'
import type { EvaluationSummary } from '@/types/evaluation'
import type { MomentSummary } from '@/types/moment'
import type { GroupResults } from '@/types/results'

/**
 * Resultados del grupo para su docente (wireframe DO-07): la nota grupal por RAP, las
 * evaluaciones de cada evaluador y la nota individual de cada integrante. Mientras la
 * calificación está abierta es una vista previa con las evaluaciones enviadas hasta hoy.
 */
export function GroupResultsTab({ groupId, moment }: { groupId: number; moment: MomentSummary }) {
  const data = useLoad(async () => {
    const [results, evaluations] = await Promise.all([resultsService.group(groupId), evaluationService.list(groupId)])
    return { results, evaluations }
  }, [groupId])

  if (data.state.kind !== 'success') {
    return <LoadStatus state={data.state} onRetry={data.reload} label="Cargando los resultados…" />
  }

  const { results, evaluations } = data.state.data
  return (
    <div className="flex flex-col gap-6">
      <Card className="bg-muted/40">
        <CardContent className="text-sm">
          {results.final ? (
            <p className="font-semibold">Notas definitivas</p>
          ) : (
            <>
              <p className="font-semibold">Vista previa</p>
              <p className="text-muted-foreground">
                Las notas son definitivas al cerrar la calificación
                {moment.gradingEnd ? `, el ${formatDate(moment.gradingEnd)}` : ''}.
              </p>
            </>
          )}
        </CardContent>
      </Card>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
        <GroupGradeCard results={results} />
        <div className="flex flex-col gap-6">
          <EvaluationsCard results={results} evaluations={evaluations} />
          <MembersCard results={results} />
        </div>
      </div>
    </div>
  )
}

/** Nota grupal y su desglose por RAP. */
function GroupGradeCard({ results }: { results: GroupResults }) {
  return (
    <Card className="self-start">
      <CardHeader>
        <p className="text-4xl font-bold" aria-label="Nota grupal">
          {results.groupGrade === null ? '—' : formatScore(results.groupGrade)}
        </p>
        <p className="text-muted-foreground text-sm">Nota grupal · de {formatScore(results.maxScore)}</p>
      </CardHeader>
      <CardContent>
        {results.groupGrade === null && (
          <p className="text-muted-foreground mb-3 text-sm">Todavía no hay evaluaciones enviadas.</p>
        )}
        <ul className="divide-y text-sm">
          {results.groupLearningOutcomes.map((outcome) => (
            <li key={outcome.id} className="flex justify-between py-2">
              <span>{outcome.code}</span>
              <span>
                {outcome.score === null ? '—' : formatScore(outcome.score)} / {formatScore(outcome.maxScore)}
              </span>
            </li>
          ))}
        </ul>
      </CardContent>
    </Card>
  )
}

/** Evaluaciones del grupo: solo cuentan las enviadas; un borrador no muestra nota. */
function EvaluationsCard({ results, evaluations }: { results: GroupResults; evaluations: EvaluationSummary[] }) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-4">
        <CardTitle>Evaluaciones</CardTitle>
        <span className="text-muted-foreground text-sm">
          {results.submittedEvaluations} {results.submittedEvaluations === 1 ? 'enviada' : 'enviadas'} ·{' '}
          {results.pendingEvaluations} {results.pendingEvaluations === 1 ? 'pendiente' : 'pendientes'}
        </span>
      </CardHeader>
      <CardContent className="overflow-x-auto">
        {evaluations.length === 0 ? (
          <p className="text-muted-foreground text-sm">Nadie ha empezado a calificar este grupo.</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead className="text-muted-foreground border-b">
              <tr>
                <th className="py-2 pr-4 font-medium">Evaluador</th>
                <th className="py-2 pr-4 font-medium">Tipo</th>
                <th className="py-2 pr-4 font-medium">Estado</th>
                <th className="py-2 text-right font-medium">Nota del grupo</th>
              </tr>
            </thead>
            <tbody>
              {evaluations.map((evaluation) => {
                const submitted = evaluation.status === 'SUBMITTED'
                return (
                  <tr key={evaluation.id} className="border-b last:border-0">
                    <td className="py-3 pr-4">{evaluation.evaluator.fullName}</td>
                    <td className="py-3 pr-4">{evaluation.evaluator.kind}</td>
                    <td className="py-3 pr-4">
                      <Badge variant={submitted ? 'default' : 'outline'}>{submitted ? 'Enviada' : 'Borrador'}</Badge>
                    </td>
                    <td className="py-3 text-right">{submitted ? formatScore(evaluation.totalScore) : '—'}</td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </CardContent>
    </Card>
  )
}

/** Nota individual de cada integrante por RAP; "Ajustado" si difiere de la del grupo. */
function MembersCard({ results }: { results: GroupResults }) {
  const codes = results.groupLearningOutcomes.map((outcome) => outcome.code)
  return (
    <Card>
      <CardHeader>
        <CardTitle>Notas individuales</CardTitle>
      </CardHeader>
      <CardContent className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="text-muted-foreground border-b">
            <tr>
              <th className="py-2 pr-4 font-medium">Estudiante</th>
              {codes.map((code) => (
                <th key={code} className="py-2 pr-4 text-right font-medium">
                  {code}
                </th>
              ))}
              <th className="py-2 text-right font-medium">Nota final</th>
            </tr>
          </thead>
          <tbody>
            {results.members.map((member) => {
              const graded = member.individualGrade !== null
              const adjusted = graded && member.individualGrade !== results.groupGrade
              return (
                <tr key={member.studentId} className="border-b last:border-0">
                  <td className="py-3 pr-4">
                    <span className="flex flex-wrap items-center gap-2">
                      {member.fullName}
                      {adjusted && <Badge variant="secondary">Ajustado</Badge>}
                    </span>
                  </td>
                  {member.learningOutcomes.map((outcome) => (
                    <td key={outcome.id} className="py-3 pr-4 text-right">
                      {graded && outcome.score !== null ? formatScore(outcome.score) : '—'}
                    </td>
                  ))}
                  <td className="py-3 text-right font-semibold">
                    {graded ? formatScore(member.individualGrade as number) : '—'}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </CardContent>
    </Card>
  )
}
