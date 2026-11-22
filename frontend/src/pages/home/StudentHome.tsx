import { Upload } from 'lucide-react'
import { Link } from 'react-router'

import { LoadStatus } from '@/components/LoadStatus'
import { Button } from '@/components/ui/button'
import { formatDate } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'

import { Empty, HomeCard, HomeLink, PhaseBadge } from './homeParts'

/**
 * Inicio del estudiante: su grupo de cada momento del periodo actual con la fase en
 * que está, el aviso para subir la entrega mientras la ventana esté abierta y los
 * momentos en los que todavía puede inscribirse en un grupo.
 */
export function StudentHome() {
  const data = useLoad(async () => {
    const [groups, moments] = await Promise.all([groupService.myGroups(), momentService.list()])
    return { groups, moments }
  }, [])

  if (data.state.kind !== 'success') {
    return <LoadStatus state={data.state} onRetry={data.reload} label="Cargando tu resumen…" />
  }
  const { groups, moments } = data.state.data
  const current = moments.flatMap((moment) => {
    const group = groups.find((item) => item.momentId === moment.id)
    return group ? [{ moment, group }] : []
  })
  const toJoin = moments.filter(
    (moment) => moment.groupFormationOpen && !groups.some((group) => group.momentId === moment.id),
  )

  return (
    <div className="grid items-start gap-6 lg:grid-cols-2">
      <HomeCard title="Mis grupos de este periodo">
        {current.length === 0 && <Empty>Todavía no estás en un grupo de este periodo.</Empty>}
        {current.map(({ moment, group }) => (
          <div key={group.id} className="flex flex-col gap-3 rounded-md border p-3">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <span className="font-medium">
                {group.name} · {moment.name}
              </span>
              <PhaseBadge moment={moment} />
            </div>
            <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
              <dt className="text-muted-foreground">Proyecto</dt>
              <dd>{group.projectTitle ?? 'Sin título'}</dd>
              <dt className="text-muted-foreground">Docente</dt>
              <dd>{group.teacher.fullName}</dd>
              <dt className="text-muted-foreground">Entrega hasta</dt>
              <dd>{group.submissionDeadline ? formatDate(group.submissionDeadline) : 'Sin fijar'}</dd>
              <dt className="text-muted-foreground">Sustentación</dt>
              <dd>{group.evaluationDate ? formatDate(group.evaluationDate) : 'Sin fijar'}</dd>
            </dl>
            <div className="flex flex-wrap gap-2">
              {moment.submissionOpen && (
                <Button size="sm" asChild>
                  <Link to={`/my-group?groupId=${group.id}`}>
                    <Upload aria-hidden />
                    Subir entrega
                  </Link>
                </Button>
              )}
              <Button size="sm" variant="outline" asChild>
                <Link to={`/my-group?groupId=${group.id}`}>Ver mi grupo</Link>
              </Button>
            </div>
          </div>
        ))}
      </HomeCard>

      <div className="flex flex-col gap-6">
        <HomeCard title="Inscripción en grupos">
          {toJoin.length === 0 ? (
            <Empty>No hay momentos con la inscripción abierta en los que te falte grupo.</Empty>
          ) : (
            toJoin.map((moment) => (
              <HomeLink
                key={moment.id}
                to="/moment-groups"
                title={`Inscríbete en un grupo de ${moment.name}`}
                detail={moment.groupFormationEnd ? `Hasta el ${formatDate(moment.groupFormationEnd)}` : undefined}
              />
            ))
          )}
        </HomeCard>
        <HomeCard title="Mis notas">
          <HomeLink to="/my-grades" title="Ver mis notas por RAP" detail="Aparecen cuando cierra la calificación" />
        </HomeCard>
      </div>
    </div>
  )
}
