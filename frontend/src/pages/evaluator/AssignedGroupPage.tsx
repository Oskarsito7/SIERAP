import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router'

import { EvaluationEditor } from '@/components/evaluation/EvaluationEditor'
import { LoadStatus } from '@/components/LoadStatus'
import { useLoad } from '@/lib/useLoad'
import { groupService } from '@/services/api/groupService'

/**
 * Calificación de un grupo asignado al evaluador externo (wireframe EV-02): el mismo
 * formulario que usa el docente (DO-05 y DO-06), con la versión a calificar y sus
 * descargas. Una evaluación enviada se puede corregir hasta el cierre y sigue enviada.
 */
export function AssignedGroupPage() {
  const id = Number(useParams().id)
  const group = useLoad(() => groupService.get(id), [id])

  return (
    <div className="flex flex-col gap-6">
      <Link to="/assigned-groups" className="text-muted-foreground flex items-center gap-2 text-sm hover:underline">
        <ArrowLeft className="size-4" aria-hidden />
        Grupos asignados
      </Link>
      <LoadStatus state={group.state} onRetry={group.reload} label="Cargando el grupo…" />
      {group.state.kind === 'success' && (
        <>
          <div className="flex flex-col gap-1">
            <h1 className="text-2xl font-bold tracking-tight">
              {group.state.data.name}
              {group.state.data.projectTitle && ` · ${group.state.data.projectTitle}`}
            </h1>
            <p className="text-muted-foreground text-sm">
              {group.state.data.momentName} · Docente: {group.state.data.teacher.fullName}
            </p>
          </div>
          <EvaluationEditor groupId={id} />
        </>
      )}
    </div>
  )
}
