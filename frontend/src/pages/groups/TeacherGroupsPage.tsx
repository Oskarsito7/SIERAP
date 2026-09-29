import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError, FormField } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useLoad } from '@/lib/useLoad'
import { groupService } from '@/services/api/groupService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import type { Group } from '@/types/group'
import type { MomentSummary } from '@/types/moment'

import { formatOptionalDate, formatWindow, groupStatus } from './groupDisplay'

/**
 * "Mis grupos" del docente (wireframe DO-01): los grupos que habilitó en un momento del
 * periodo actual y el formulario para habilitar uno nuevo mientras el momento siga en
 * configuración. El momento elegido queda en la dirección (`?momentId=`).
 */
export function TeacherGroupsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const moments = useLoad(() => momentService.list(), [])

  const momentList = moments.state.kind === 'success' ? moments.state.data : []
  const requestedId = Number(searchParams.get('momentId'))
  const moment = momentList.find((item) => item.id === requestedId) ?? momentList[0]

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Mis grupos</h1>
      <LoadStatus state={moments.state} onRetry={moments.reload} label="Cargando los momentos…" />

      {moments.state.kind === 'success' && momentList.length === 0 && (
        <p className="text-muted-foreground text-sm">No hay momentos de tu facultad en el periodo actual.</p>
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
                    {item.name} · {item.programName}
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

/** Grupos del docente en un momento y el formulario para habilitar uno. */
function MomentGroups({ moment }: { moment: MomentSummary }) {
  const groups = useLoad(() => groupService.list(moment.id), [moment.id])
  const configuring = moment.status === 'CONFIGURATION'

  return (
    <div className="flex flex-col gap-6">
      <Card className="bg-muted/40">
        <CardContent className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
          <span className="font-semibold">
            {moment.name} · {moment.programName}
          </span>
          <Badge variant={configuring ? 'secondary' : 'default'}>{configuring ? 'En configuración' : 'Lanzado'}</Badge>
          <span className="text-muted-foreground">
            Grupos de {moment.minMembers} a {moment.maxMembers} integrantes
          </span>
          <span className="text-muted-foreground">
            Conformación {formatWindow(moment.groupFormationStart, moment.groupFormationEnd)}
          </span>
        </CardContent>
      </Card>

      <LoadStatus state={groups.state} onRetry={groups.reload} label="Cargando tus grupos…" />

      {groups.state.kind === 'success' && (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {groups.state.data.map((group) => (
            <GroupCard key={group.id} group={group} />
          ))}
          {groups.state.data.length === 0 && (
            <p className="text-muted-foreground text-sm">Todavía no has habilitado grupos en este momento.</p>
          )}
        </div>
      )}

      {configuring ? (
        <NewGroupForm
          moment={moment}
          onCreated={(group) => groups.setData((current) => [...current, group].sort(byName))}
        />
      ) : (
        <p className="text-muted-foreground text-sm">
          El momento ya fue lanzado: no se pueden habilitar grupos nuevos.
        </p>
      )}
    </div>
  )
}

function GroupCard({ group }: { group: Group }) {
  const status = groupStatus(group)
  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between gap-2">
          <CardTitle>{group.name}</CardTitle>
          <Badge variant={status.variant}>{status.label}</Badge>
        </div>
        <CardDescription>{group.projectTitle ?? 'Sin título todavía'}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
          <dt className="text-muted-foreground">Integrantes</dt>
          <dd>
            {group.memberCount} de {group.maxMembers}
          </dd>
          <dt className="text-muted-foreground">Entrega hasta</dt>
          <dd>{formatOptionalDate(group.submissionDeadline)}</dd>
          <dt className="text-muted-foreground">Evaluación</dt>
          <dd>{formatOptionalDate(group.evaluationDate)}</dd>
          <dt className="text-muted-foreground">Evaluadores</dt>
          <dd>{evaluatorsLabel(group.evaluators.length)}</dd>
        </dl>
        <Button variant="outline" asChild>
          <Link to={`/groups/${group.id}`}>Abrir grupo</Link>
        </Button>
      </CardContent>
    </Card>
  )
}

function NewGroupForm({ moment, onCreated }: { moment: MomentSummary; onCreated: (group: Group) => void }) {
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldError, setFieldError] = useState<string | undefined>()
  const [saving, setSaving] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setFieldError(undefined)
    if (!name.trim()) {
      setFieldError('El nombre es obligatorio')
      return
    }
    setSaving(true)
    try {
      onCreated(await groupService.create(moment.id, name.trim()))
      setName('')
    } catch (createError) {
      setFieldError(getFieldErrors(createError).name)
      setError(getErrorMessage(createError))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Card className="max-w-md">
      <CardHeader>
        <CardTitle>Habilitar grupo</CardTitle>
        <CardDescription>
          Los estudiantes del {moment.name} lo verán enseguida y podrán inscribirse mientras la conformación esté
          abierta.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
          <FormField
            label="Nombre del grupo"
            value={name}
            maxLength={100}
            error={fieldError}
            onChange={(event) => setName(event.target.value)}
          />
          <FormError message={error} />
          <Button type="submit" disabled={saving}>
            {saving ? 'Habilitando…' : 'Habilitar'}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

function evaluatorsLabel(count: number): string {
  if (count === 0) return 'Ninguno'
  return count === 1 ? '1 asignado' : `${count} asignados`
}

function byName(a: Group, b: Group): number {
  return a.name.localeCompare(b.name, 'es', { numeric: true })
}
