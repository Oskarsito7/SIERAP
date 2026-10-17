import { LoadStatus } from '@/components/LoadStatus'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent } from '@/components/ui/card'
import { useLoad } from '@/lib/useLoad'
import { groupService } from '@/services/api/groupService'
import type { MomentDetail } from '@/types/moment'

import { groupStatus } from '../groups/groupDisplay'

/**
 * Grupos que los docentes habilitaron en el momento (wireframe AD-05), con sus
 * integrantes frente al mínimo y el máximo. Los grupos por debajo del mínimo no impiden
 * lanzar: se pide confirmación y quedan como están (`docs/04`).
 */
export function GroupsTab({ moment }: { moment: MomentDetail }) {
  const groups = useLoad(() => groupService.list(moment.id), [moment.id])

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
                  <th className="py-2 font-medium">Estado</th>
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
                      <td className="py-3">
                        <Badge variant={status.variant}>
                          {status.label}
                          {status.label === 'Bajo el mínimo' ? ` (${group.minMembers})` : ''}
                        </Badge>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  )
}
