import { Link } from 'react-router'

import { LoadStatus } from '@/components/LoadStatus'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { formatDate, todayIso } from '@/lib/dates'
import { formatScore } from '@/lib/scores'
import { useLoad } from '@/lib/useLoad'
import { evaluationService } from '@/services/api/evaluationService'
import { groupService } from '@/services/api/groupService'
import type { EvaluationSummary } from '@/types/evaluation'
import type { Group } from '@/types/group'

interface AssignedGroup {
  group: Group
  /** Evaluación propia del grupo; `undefined` si todavía no la empezó. */
  evaluation: EvaluationSummary | undefined
}

/**
 * "Grupos asignados" del evaluador externo (wireframe EV-01): solo los grupos a los que
 * un docente lo asignó, que pueden ser de varios docentes y programas, con el estado de
 * su propia evaluación. Califica cada grupo desde su fecha de evaluación hasta que
 * cierra la calificación del momento; no ve las evaluaciones de los demás.
 */
export function AssignedGroupsPage() {
  const data = useLoad<AssignedGroup[]>(async () => {
    const groups = await groupService.list()
    return Promise.all(
      groups.map(async (group) => ({ group, evaluation: (await evaluationService.list(group.id))[0] })),
    )
  }, [])
  const today = todayIso()

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">Grupos asignados</h1>
        <p className="text-muted-foreground max-w-2xl text-sm">
          Puedes calificar cada grupo desde su fecha de evaluación hasta que cierre la calificación de su momento.
        </p>
      </div>
      <LoadStatus state={data.state} onRetry={data.reload} label="Cargando tus grupos…" />
      {data.state.kind === 'success' && data.state.data.length === 0 && (
        <p className="text-muted-foreground text-sm">Todavía no te han asignado grupos para calificar.</p>
      )}
      {data.state.kind === 'success' && data.state.data.length > 0 && (
        <Card>
          <CardContent className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground border-b">
                <tr>
                  <th className="py-2 pr-4 font-medium">Grupo</th>
                  <th className="py-2 pr-4 font-medium">Momento</th>
                  <th className="py-2 pr-4 font-medium">Docente</th>
                  <th className="py-2 pr-4 font-medium">Evaluación</th>
                  <th className="py-2 pr-4 font-medium">Mi evaluación</th>
                  <th className="py-2 font-medium">
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.state.data.map(({ group, evaluation }) => {
                  const notYet = group.evaluationDate !== null && today < group.evaluationDate
                  return (
                    <tr key={group.id} className="border-b last:border-0">
                      <td className="py-3 pr-4">
                        <span className="font-medium">{group.name}</span>
                        {group.projectTitle && (
                          <span className="text-muted-foreground block text-xs">{group.projectTitle}</span>
                        )}
                      </td>
                      <td className="py-3 pr-4">{group.momentName}</td>
                      <td className="py-3 pr-4">{group.teacher.fullName}</td>
                      <td className="py-3 pr-4 whitespace-nowrap">
                        {group.evaluationDate ? formatDate(group.evaluationDate) : 'Sin fijar'}
                      </td>
                      <td className="py-3 pr-4">
                        {evaluation ? (
                          <Badge variant={evaluation.status === 'SUBMITTED' ? 'default' : 'outline'}>
                            {evaluation.status === 'SUBMITTED' ? 'Enviada' : 'Borrador'} ·{' '}
                            {formatScore(evaluation.totalScore)}
                          </Badge>
                        ) : (
                          <span className="text-muted-foreground">Sin empezar</span>
                        )}
                      </td>
                      <td className="py-3 text-right">
                        {notYet && !evaluation ? (
                          <span className="text-muted-foreground text-xs whitespace-nowrap">
                            Desde el {formatDate(group.evaluationDate as string)}
                          </span>
                        ) : (
                          <Button variant="outline" size="sm" asChild>
                            <Link
                              to={`/assigned-groups/${group.id}`}
                              aria-label={`${actionLabel(evaluation)} ${group.name}`}
                            >
                              {actionLabel(evaluation)}
                            </Link>
                          </Button>
                        )}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

function actionLabel(evaluation: EvaluationSummary | undefined): string {
  if (!evaluation) return 'Calificar'
  return evaluation.status === 'SUBMITTED' ? 'Ver o corregir' : 'Continuar'
}
