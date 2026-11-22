import { StatTile } from '@/components/charts/StatTile'
import { LoadStatus } from '@/components/LoadStatus'
import { formatDate } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { evaluationService } from '@/services/api/evaluationService'
import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'

import { Empty, HomeCard, HomeLink, PhaseBadge } from './homeParts'
import { plural } from './momentPhase'

/**
 * Inicio del docente: sus grupos del periodo actual, los que todavía no tienen fecha
 * de entrega o de sustentación y los que puede calificar y aún no tienen su
 * evaluación enviada. Solo ve sus propios grupos (lo garantiza la API).
 */
export function TeacherHome() {
  const data = useLoad(async () => {
    const [groups, moments, evaluations] = await Promise.all([
      groupService.list(),
      momentService.list(),
      evaluationService.mine(),
    ])
    return { groups, moments, evaluations }
  }, [])

  if (data.state.kind !== 'success') {
    return <LoadStatus state={data.state} onRetry={data.reload} label="Cargando tu resumen…" />
  }
  const { groups, moments, evaluations } = data.state.data
  const momentOf = new Map(moments.map((moment) => [moment.id, moment]))
  const current = groups.filter((group) => momentOf.has(group.momentId))
  const submitted = new Set(evaluations.filter((item) => item.status === 'SUBMITTED').map((item) => item.groupId))
  const withoutDates = current.filter((group) => !group.submissionDeadline || !group.evaluationDate)
  const toGrade = current.filter((group) => momentOf.get(group.momentId)?.gradingOpen && !submitted.has(group.id))
  const sent = current.filter((group) => submitted.has(group.id)).length

  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-3 sm:grid-cols-3">
        <StatTile label="Mis grupos este periodo" value={String(current.length)} />
        <StatTile label="Sin fechas completas" value={String(withoutDates.length)} detail="Entrega o sustentación" />
        <StatTile
          label="Mis evaluaciones enviadas"
          value={`${sent} de ${current.length}`}
          detail={toGrade.length > 0 ? `${toGrade.length} por calificar ahora` : undefined}
        />
      </div>

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <HomeCard title="Por calificar" description="Grupos con la calificación abierta y tu evaluación sin enviar.">
          {toGrade.length === 0 && <Empty>No tienes calificaciones pendientes.</Empty>}
          {toGrade.map((group) => (
            <HomeLink
              key={group.id}
              to={`/groups/${group.id}/evaluation`}
              title={`${group.name} · ${group.momentName}`}
              detail={group.evaluationDate ? `Sustentación el ${formatDate(group.evaluationDate)}` : undefined}
            />
          ))}
        </HomeCard>

        <HomeCard title="Requieren fechas" description="Fíjalas para que los estudiantes sepan cuándo entregar y sustentar.">
          {withoutDates.length === 0 && <Empty>Todos tus grupos tienen sus fechas.</Empty>}
          {withoutDates.map((group) => (
            <HomeLink
              key={group.id}
              to={`/groups/${group.id}`}
              title={`${group.name} · ${group.momentName}`}
              detail={[!group.submissionDeadline && 'Sin fecha de entrega', !group.evaluationDate && 'Sin sustentación']
                .filter(Boolean)
                .join(' · ')}
            />
          ))}
        </HomeCard>
      </div>

      <HomeCard title="Momentos de este periodo">
        {moments.length === 0 && <Empty>No hay momentos en el periodo actual.</Empty>}
        {moments.map((moment) => (
          <HomeLink
            key={moment.id}
            to="/groups"
            title={`${moment.name} · ${moment.programName}`}
            detail={`${plural(current.filter((group) => group.momentId === moment.id).length, 'grupo tuyo', 'grupos tuyos')}`}
            aside={<PhaseBadge moment={moment} />}
          />
        ))}
      </HomeCard>
    </div>
  )
}
