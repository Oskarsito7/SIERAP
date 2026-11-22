import { StatTile } from '@/components/charts/StatTile'
import { LoadStatus } from '@/components/LoadStatus'
import { formatDate } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { evaluationService } from '@/services/api/evaluationService'
import { groupService } from '@/services/api/groupService'

import { Empty, HomeCard, HomeLink } from './homeParts'

/**
 * Inicio del evaluador externo: cuántos grupos le asignaron, cuántos le faltan por
 * calificar y la lista de pendientes por fecha de sustentación. Dos peticiones, sin
 * importar cuántos grupos tenga.
 */
export function EvaluatorHome() {
  const data = useLoad(async () => {
    const [groups, evaluations] = await Promise.all([groupService.list(), evaluationService.mine()])
    return { groups, evaluations }
  }, [])

  if (data.state.kind !== 'success') {
    return <LoadStatus state={data.state} onRetry={data.reload} label="Cargando tu resumen…" />
  }
  const { groups, evaluations } = data.state.data
  const statusOf = new Map(evaluations.map((evaluation) => [evaluation.groupId, evaluation.status]))
  const pending = groups
    .filter((group) => statusOf.get(group.id) !== 'SUBMITTED')
    .sort((a, b) => (a.evaluationDate ?? '9999').localeCompare(b.evaluationDate ?? '9999'))

  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-3 sm:grid-cols-3">
        <StatTile label="Grupos asignados" value={String(groups.length)} />
        <StatTile label="Por calificar" value={String(pending.length)} />
        <StatTile label="Evaluaciones enviadas" value={String(groups.length - pending.length)} />
      </div>
      <HomeCard
        title="Pendientes por calificar"
        description="Ordenados por fecha de sustentación. Puedes calificar desde esa fecha hasta que cierre la calificación."
      >
        {pending.length === 0 && <Empty>No tienes evaluaciones pendientes. ¡Gracias por tu apoyo!</Empty>}
        {pending.map((group) => (
          <HomeLink
            key={group.id}
            to={`/assigned-groups/${group.id}`}
            title={`${group.name}${group.projectTitle ? ` · ${group.projectTitle}` : ''}`}
            detail={`${group.momentName} · Docente: ${group.teacher.fullName} · ${
              group.evaluationDate ? `Sustentación el ${formatDate(group.evaluationDate)}` : 'Sustentación sin fijar'
            }${statusOf.get(group.id) === 'DRAFT' ? ' · Borrador guardado' : ''}`}
          />
        ))}
      </HomeCard>
    </div>
  )
}
