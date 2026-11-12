import { useState } from 'react'

import { FormError } from '@/components/forms/FormField'
import { LoadStatus } from '@/components/LoadStatus'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { groupService } from '@/services/api/groupService'
import { getErrorMessage } from '@/services/api/httpClient'
import { teacherService } from '@/services/api/teacherService'
import type { Group } from '@/types/group'
import type { MomentDetail } from '@/types/moment'

import { groupStatus } from '../groups/groupDisplay'

/**
 * Grupos que los docentes habilitaron en el momento (wireframe AD-05), con sus
 * integrantes frente al mínimo y el máximo. Los grupos por debajo del mínimo no impiden
 * lanzar: se pide confirmación y quedan como están (`docs/04`).
 *
 * El admin puede pasar un grupo a otro docente de la facultad hasta que cierre la
 * calificación, mientras el docente actual no haya enviado su evaluación (decisión del
 * 2026-11-11); la API valida esas reglas y aquí se muestra su mensaje.
 */
export function GroupsTab({ moment }: { moment: MomentDetail }) {
  const groups = useLoad(() => groupService.list(moment.id), [moment.id])
  const [reassigning, setReassigning] = useState<Group | null>(null)

  const replaceGroup = (updated: Group) => {
    groups.setData((current) => current.map((group) => (group.id === updated.id ? updated : group)))
    setReassigning(null)
  }

  return (
    <Card>
      <CardContent className="flex flex-col gap-4">
        <LoadStatus state={groups.state} onRetry={groups.reload} label="Cargando los grupos…" />
        {groups.state.kind === 'success' && groups.state.data.length === 0 && (
          <p className="text-muted-foreground text-sm">
            Los docentes todavía no han habilitado grupos en este momento.
          </p>
        )}
        {groups.state.kind === 'success' && groups.state.data.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground border-b">
                <tr>
                  <th className="py-2 pr-4 font-medium">Grupo</th>
                  <th className="py-2 pr-4 font-medium">Docente</th>
                  <th className="py-2 pr-4 font-medium">Proyecto</th>
                  <th className="py-2 pr-4 text-right font-medium">Integrantes</th>
                  <th className="py-2 pr-4 font-medium">Estado</th>
                  <th className="py-2 font-medium">
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {groups.state.data.map((group) => {
                  const status = groupStatus(group)
                  return (
                    <tr key={group.id} className="border-b last:border-0">
                      <td className="py-3 pr-4 font-medium">{group.name}</td>
                      <td className="py-3 pr-4">{group.teacher.fullName}</td>
                      <td className="py-3 pr-4">
                        {group.projectTitle ?? <span className="text-muted-foreground">Sin título</span>}
                      </td>
                      <td className="py-3 pr-4 text-right whitespace-nowrap">
                        {group.memberCount} / {group.maxMembers}
                      </td>
                      <td className="py-3 pr-4">
                        <Badge variant={status.variant}>
                          {status.label}
                          {status.label === 'Bajo el mínimo' ? ` (${group.minMembers})` : ''}
                        </Badge>
                      </td>
                      <td className="py-3 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          aria-label={`Reasignar ${group.name}`}
                          onClick={() => setReassigning(group)}
                        >
                          Reasignar
                        </Button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
        {reassigning && (
          <ReassignPanel
            key={reassigning.id}
            group={reassigning}
            programId={moment.programId}
            onReassigned={replaceGroup}
            onCancel={() => setReassigning(null)}
          />
        )}
      </CardContent>
    </Card>
  )
}

interface ReassignPanelProps {
  group: Group
  programId: number
  onReassigned: (group: Group) => void
  onCancel: () => void
}

/** Elige el docente activo de la facultad del programa que recibe el grupo. */
function ReassignPanel({ group, programId, onReassigned, onCancel }: ReassignPanelProps) {
  const teachers = useLoad(async () => {
    const program = (await catalogService.programs()).find((candidate) => candidate.id === programId)
    if (!program) return []
    const all = await teacherService.list(program.facultyId)
    return all.filter((teacher) => teacher.active && teacher.id !== group.teacher.id)
  }, [programId, group.teacher.id])
  const [teacherId, setTeacherId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const reassign = async () => {
    setError(null)
    setSaving(true)
    try {
      onReassigned(await groupService.reassign(group.id, Number(teacherId)))
    } catch (reassignError) {
      setError(getErrorMessage(reassignError))
      setSaving(false)
    }
  }

  return (
    <section aria-label={`Reasignar ${group.name}`} className="bg-muted/40 flex flex-col gap-3 rounded-md border p-4">
      <div>
        <p className="font-medium">Reasignar {group.name}</p>
        <p className="text-muted-foreground text-sm">
          Hoy es de {group.teacher.fullName}. Si ya envió su evaluación no se puede reasignar; si tiene un borrador, se
          descarta y el nuevo docente califica desde cero.
        </p>
      </div>
      <LoadStatus state={teachers.state} onRetry={teachers.reload} label="Cargando los docentes…" />
      {teachers.state.kind === 'success' && teachers.state.data.length === 0 && (
        <p className="text-muted-foreground text-sm">No hay otro docente activo en la facultad.</p>
      )}
      {teachers.state.kind === 'success' && teachers.state.data.length > 0 && (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <label className="flex flex-1 flex-col gap-2 text-sm font-medium">
            Nuevo docente
            <select
              className="border-input bg-background h-9 rounded-md border px-3 text-sm font-normal"
              value={teacherId}
              onChange={(event) => setTeacherId(event.target.value)}
            >
              <option value="">Elige un docente</option>
              {teachers.state.data.map((teacher) => (
                <option key={teacher.id} value={teacher.id}>
                  {teacher.fullName}
                </option>
              ))}
            </select>
          </label>
          <div className="flex gap-2">
            <Button variant="outline" onClick={onCancel}>
              Cancelar
            </Button>
            <Button disabled={!teacherId || saving} onClick={() => void reassign()}>
              Reasignar
            </Button>
          </div>
        </div>
      )}
      <FormError message={error} />
    </section>
  )
}
