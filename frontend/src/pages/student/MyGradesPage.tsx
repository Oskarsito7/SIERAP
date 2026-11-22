import { Link, useSearchParams } from 'react-router'

import { LoadStatus } from '@/components/LoadStatus'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatDate } from '@/lib/dates'
import { formatScore } from '@/lib/scores'
import { useLoad } from '@/lib/useLoad'
import { momentService } from '@/services/api/momentService'
import { resultsService } from '@/services/api/resultsService'
import type { MomentSummary } from '@/types/moment'
import type { StudentOutcomeScore, StudentResult } from '@/types/results'

/**
 * "Mis notas" del estudiante (wireframes ES-04 y ES-05): su nota final, la del grupo,
 * el desempeño por RAP y los comentarios de los evaluadores (por tipo, sin nombre).
 * Las notas aparecen cuando cierra la calificación del momento; antes se muestra cuándo
 * estarán. El momento elegido queda en la dirección (`?momentId=`).
 */
export function MyGradesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const data = useLoad(async () => {
    const [results, moments] = await Promise.all([resultsService.mine(), momentService.list()])
    return { results, moments }
  }, [])

  if (data.state.kind !== 'success') {
    return (
      <div className="flex flex-col gap-6">
        <h1 className="text-2xl font-bold tracking-tight">Mis notas</h1>
        <LoadStatus state={data.state} onRetry={data.reload} label="Cargando tus notas…" />
      </div>
    )
  }

  const { results, moments } = data.state.data
  const requestedId = Number(searchParams.get('momentId'))
  const result = results.find((item) => item.momentId === requestedId) ?? results[0]
  const momentOf = (item: StudentResult) => moments.find((moment) => moment.id === item.momentId)

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Mis notas</h1>

      {!result && (
        <Card className="max-w-xl">
          <CardHeader>
            <CardTitle>Todavía no tienes notas</CardTitle>
            <CardDescription>Verás tus notas aquí cuando estés en un grupo y cierre su calificación.</CardDescription>
          </CardHeader>
          <CardContent>
            <Button asChild>
              <Link to="/moment-groups">Ver los grupos del momento</Link>
            </Button>
          </CardContent>
        </Card>
      )}

      {result && results.length > 1 && (
        <label className="flex max-w-sm flex-col gap-2 text-sm font-medium">
          Momento
          <select
            className="border-input bg-background h-9 rounded-md border px-3 text-sm font-normal"
            value={result.momentId}
            onChange={(event) => setSearchParams({ momentId: event.target.value })}
          >
            {results.map((item) => (
              <option key={item.momentId} value={item.momentId}>
                {momentLabel(item, momentOf(item))}
              </option>
            ))}
          </select>
        </label>
      )}

      {result && results.length === 1 && (
        <p className="text-muted-foreground text-sm">{momentLabel(result, momentOf(result))}</p>
      )}

      {result && (result.available ? <ResultView result={result} /> : <NotReady result={result} moment={momentOf(result)} />)}
    </div>
  )
}

function momentLabel(result: StudentResult, moment?: MomentSummary): string {
  return [result.momentName, moment?.subject, result.periodCode].filter(Boolean).join(' · ')
}

/** Notas todavía no disponibles (ES-05). */
function NotReady({ result, moment }: { result: StudentResult; moment?: MomentSummary }) {
  return (
    <Card className="bg-muted/40">
      <CardContent className="flex flex-col items-center gap-2 py-10 text-center">
        <h2 className="text-xl font-semibold">Tus notas del {result.momentName} aún no están listas</h2>
        <p className="text-muted-foreground max-w-xl text-sm">
          {moment?.gradingEnd
            ? `La calificación cierra el ${formatDate(moment.gradingEnd)}. Después verás`
            : 'Cuando cierre la calificación verás'}{' '}
          tu nota final, tu desempeño por RAP y los comentarios de los evaluadores.
        </p>
      </CardContent>
    </Card>
  )
}

/** Notas del momento (ES-04). */
function ResultView({ result }: { result: StudentResult }) {
  const finalGrade = result.finalGrade ?? 0
  const groupGrade = result.groupGrade ?? 0
  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
      <Card className="self-start">
        <CardContent className="flex flex-col gap-4">
          <div>
            <p className="text-4xl font-bold" aria-label="Mi nota final">
              {formatScore(finalGrade)}
            </p>
            <p className="text-muted-foreground text-sm">Mi nota final · de {formatScore(result.maxScore)}</p>
          </div>
          <div>
            <p className="text-2xl font-bold" aria-label="Nota del grupo">
              {formatScore(groupGrade)}
            </p>
            <p className="text-muted-foreground text-sm">
              Nota del {result.groupName}
              {result.projectTitle ? ` (${result.projectTitle})` : ''}
            </p>
          </div>
          {finalGrade !== groupGrade && (
            <p className="text-muted-foreground text-sm">
              Tu nota es distinta a la del grupo porque un evaluador ajustó tu calificación en algún criterio.
            </p>
          )}
        </CardContent>
      </Card>

      <div className="flex flex-col gap-6">
        <Card>
          <CardHeader>
            <CardTitle>Desempeño por RAP</CardTitle>
          </CardHeader>
          <CardContent>
            <ul className="divide-y">
              {result.learningOutcomes.map((outcome) => (
                <li key={outcome.code} className="grid grid-cols-[4rem_1fr_auto] items-center gap-4 py-3 text-sm">
                  <span className="font-semibold" title={outcome.description}>
                    {outcome.code}
                  </span>
                  <div
                    role="meter"
                    aria-label={`${outcome.code}: ${outcome.description}`}
                    aria-valuemin={0}
                    aria-valuemax={outcome.maxScore}
                    aria-valuenow={outcome.score ?? undefined}
                    className="bg-muted h-2 overflow-hidden rounded-full"
                  >
                    <div
                      className="bg-primary h-full rounded-full"
                      style={{ width: `${achievement(outcome)}%` }}
                    />
                  </div>
                  <span className="whitespace-nowrap">
                    {outcome.score === null ? '—' : formatScore(outcome.score)} / {formatScore(outcome.maxScore)}
                  </span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Comentarios de los evaluadores</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-col gap-3">
            {result.comments.length === 0 && (
              <p className="text-muted-foreground text-sm">Los evaluadores no dejaron comentarios.</p>
            )}
            {result.comments.map((comment, index) => (
              <div key={index} className="bg-muted/40 rounded-md border px-4 py-3 text-sm">
                <p className="font-semibold">{comment.evaluatorKind}</p>
                <p>{comment.comment}</p>
              </div>
            ))}
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

/** Ancho de la barra de un RAP: su puntaje sobre el máximo, o 0 si todavía no hay nota. */
function achievement(outcome: StudentOutcomeScore): number {
  if (outcome.score === null || outcome.maxScore <= 0) return 0
  return (outcome.score / outcome.maxScore) * 100
}
