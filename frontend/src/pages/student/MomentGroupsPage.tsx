import { useState, type ReactNode } from 'react'
import { useSearchParams } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { LoadStatus } from '@/components/LoadStatus'
import { FormError } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatDate } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { groupService } from '@/services/api/groupService'
import { getErrorMessage } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import type { Group } from '@/types/group'
import type { MomentSummary } from '@/types/moment'

/**
 * "Grupos del momento" del estudiante (wireframes ES-01 y ES-02): los grupos de todos
 * los docentes de un momento de su programa. Se inscribe, se cambia o se sale mientras
 * la conformación esté abierta; cambiarse pide confirmación porque lo saca de su grupo
 * actual. El momento elegido queda en la dirección (`?momentId=`).
 */
export function MomentGroupsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const moments = useLoad(() => momentService.list(), [])

  const momentList = moments.state.kind === 'success' ? moments.state.data : []
  const requestedId = Number(searchParams.get('momentId'))
  const moment = momentList.find((item) => item.id === requestedId) ?? momentList[0]

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Grupos del momento</h1>
      <LoadStatus state={moments.state} onRetry={moments.reload} label="Cargando los momentos…" />

      {moments.state.kind === 'success' && momentList.length === 0 && (
        <p className="text-muted-foreground text-sm">
          No hay momentos de tu programa para tu semestre en el periodo actual.
        </p>
      )}

      {moment && (
        <>
          {momentList.length > 1 && (
            <label className="flex max-w-sm flex-col gap-2 text-sm font-medium">
              Momento
              <select
                className="border-input bg-background h-9 rounded-md border px-3 text-sm font-normal"
                value={moment.id}
                onChange={(event) => setSearchParams({ momentId: event.target.value })}
              >
                {momentList.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.name} · {item.subject}
                  </option>
                ))}
              </select>
            </label>
          )}
          <MomentGroups key={moment.id} moment={moment} />
        </>
      )}
    </div>
  )
}

/** Grupos de un momento con las acciones del estudiante. */
function MomentGroups({ moment }: { moment: MomentSummary }) {
  const userId = useAuth().user?.id
  const groups = useLoad(() => groupService.list(moment.id), [moment.id])
  const [pending, setPending] = useState<number | null>(null)
  const [switchTo, setSwitchTo] = useState<Group | null>(null)
  const [error, setError] = useState<string | null>(null)

  const list = groups.state.kind === 'success' ? groups.state.data : []
  const myGroup = list.find((group) => group.myGroup)
  const open = moment.groupFormationOpen

  /** Saca al estudiante de un grupo en la lista local, sin volver a pedirla. */
  const withoutMe = (group: Group): Group => ({
    ...group,
    members: group.members.filter((member) => member.id !== userId),
    memberCount: group.memberCount - 1,
    full: false,
    myGroup: false,
  })

  const join = async (group: Group) => {
    setSwitchTo(null)
    setError(null)
    setPending(group.id)
    try {
      const joined = await groupService.join(group.id)
      groups.setData((current) =>
        current.map((item) => (item.id === joined.id ? joined : item.myGroup ? withoutMe(item) : item)),
      )
    } catch (joinError) {
      setError(getErrorMessage(joinError))
    } finally {
      setPending(null)
    }
  }

  const leave = async (group: Group) => {
    setError(null)
    setPending(group.id)
    try {
      await groupService.leave(group.id)
      groups.setData((current) => current.map((item) => (item.id === group.id ? withoutMe(item) : item)))
    } catch (leaveError) {
      setError(getErrorMessage(leaveError))
    } finally {
      setPending(null)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <Card className="bg-muted/40">
        <CardContent className="flex flex-wrap items-center justify-between gap-4">
          <div className="flex flex-col gap-1">
            <span className="font-semibold">
              {moment.name} · {moment.subject}
            </span>
            <span className="text-muted-foreground text-sm">
              Semestres {moment.semesterFrom} a {moment.semesterTo} · grupos de {moment.minMembers} a{' '}
              {moment.maxMembers} integrantes
            </span>
          </div>
          <div className="flex flex-wrap gap-2">
            <Badge variant={open ? 'default' : 'secondary'}>{formationLabel(moment)}</Badge>
            {myGroup && <Badge variant="outline">Estás en el {myGroup.name}</Badge>}
          </div>
        </CardContent>
      </Card>

      {switchTo && myGroup && (
        <Card role="alertdialog" aria-labelledby="switch-title" className="border-primary max-w-xl">
          <CardHeader>
            <CardTitle id="switch-title">¿Cambiarte al {switchTo.name}?</CardTitle>
            <CardDescription>
              Saldrás del {myGroup.name}
              {myGroup.projectTitle && ` (${myGroup.projectTitle})`}, docente {myGroup.teacher.fullName}, y quedarás
              en el {switchTo.name}
              {switchTo.projectTitle && ` (${switchTo.projectTitle})`}.
              {moment.groupFormationEnd &&
                ` Puedes volver a cambiar hasta el ${formatDate(moment.groupFormationEnd)} si hay cupo.`}
            </CardDescription>
          </CardHeader>
          <CardContent className="flex gap-3">
            <Button variant="outline" onClick={() => setSwitchTo(null)}>
              Cancelar
            </Button>
            <Button onClick={() => void join(switchTo)}>Cambiarme al {switchTo.name}</Button>
          </CardContent>
        </Card>
      )}

      <FormError message={error} />
      <LoadStatus state={groups.state} onRetry={groups.reload} label="Cargando los grupos…" />

      {groups.state.kind === 'success' && list.length === 0 && (
        <p className="text-muted-foreground text-sm">Los docentes todavía no han habilitado grupos en este momento.</p>
      )}

      {list.length > 0 && (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {list.map((group) => (
            <GroupCard
              key={group.id}
              group={group}
              userId={userId}
              action={
                group.myGroup ? (
                  <Button variant="outline" disabled={!open || pending !== null} onClick={() => void leave(group)}>
                    {pending === group.id ? 'Saliendo…' : 'Salir del grupo'}
                  </Button>
                ) : group.full ? (
                  <Button variant="secondary" disabled>
                    Grupo completo
                  </Button>
                ) : (
                  <Button
                    disabled={!open || pending !== null}
                    onClick={() => (myGroup ? setSwitchTo(group) : void join(group))}
                  >
                    {pending === group.id ? 'Inscribiendo…' : myGroup ? 'Cambiarme a este grupo' : 'Unirme'}
                  </Button>
                )
              }
            />
          ))}
        </div>
      )}
    </div>
  )
}

function GroupCard({ group, userId, action }: { group: Group; userId?: number; action: ReactNode }) {
  return (
    <Card className={group.myGroup ? 'border-primary border-2' : undefined}>
      <CardHeader>
        <div className="flex items-center justify-between gap-2">
          <CardTitle>{group.name}</CardTitle>
          <Badge variant={group.myGroup ? 'default' : group.full ? 'secondary' : 'outline'}>
            {group.myGroup ? 'Mi grupo' : group.full ? 'Completo' : 'Con cupo'}
          </Badge>
        </div>
        <CardDescription>
          Docente: {group.teacher.fullName}
          {group.projectTitle && ` · Proyecto: ${group.projectTitle}`}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-1 flex-col gap-4">
        {group.members.length > 0 ? (
          <ul className="list-disc pl-5 text-sm">
            {group.members.map((member) => (
              <li key={member.id}>
                {member.fullName}
                {member.id === userId && ' (tú)'}
              </li>
            ))}
          </ul>
        ) : (
          <p className="text-muted-foreground text-sm">Sin integrantes todavía.</p>
        )}
        <div className="text-muted-foreground mt-auto flex justify-between text-sm">
          <span>Integrantes</span>
          <span>
            {group.memberCount} de {group.maxMembers}
          </span>
        </div>
        {action}
      </CardContent>
    </Card>
  )
}

/** Estado de la conformación de grupos, como lo muestra el wireframe ES-01. */
function formationLabel(moment: MomentSummary): string {
  if (moment.status === 'LAUNCHED') return 'Conformación cerrada: el momento ya fue lanzado'
  if (moment.groupFormationOpen && moment.groupFormationEnd) {
    return `Conformación abierta hasta el ${formatDate(moment.groupFormationEnd)}`
  }
  if (!moment.groupFormationStart || !moment.groupFormationEnd) return 'Conformación sin fechas todavía'
  return `Conformación del ${formatDate(moment.groupFormationStart)} al ${formatDate(moment.groupFormationEnd)}`
}
