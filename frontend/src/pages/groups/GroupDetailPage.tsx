import { useState, type FormEvent } from 'react'
import { ArrowLeft, Search } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError, FormField } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { todayIso } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { groupService } from '@/services/api/groupService'
import { getErrorMessage } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import type { UserSummary } from '@/types/auth'
import type { Group, StudentWithoutGroup, UpdateGroupRequest } from '@/types/group'
import type { MomentSummary } from '@/types/moment'

import { formatWindow, groupStatus } from './groupDisplay'

interface GroupWithMoment {
  group: Group
  moment: MomentSummary
}

/** Recibe el grupo actualizado que devuelve la API después de una acción. */
type OnGroupChange = (group: Group) => void

/**
 * Detalle de un grupo del docente (wireframes DO-02 y DO-03): título del proyecto,
 * integrantes, fechas y evaluadores externos. Los botones se deshabilitan según el
 * estado y las ventanas del momento; el backend vuelve a validarlo y responde 422
 * con el motivo, que se muestra en la sección.
 */
export function GroupDetailPage() {
  const id = Number(useParams().id)
  const { state, reload, setData } = useLoad<GroupWithMoment>(async () => {
    const group = await groupService.get(id)
    return { group, moment: await momentService.get(group.momentId) }
  }, [id])

  if (state.kind !== 'success') {
    return (
      <div className="flex flex-col gap-6">
        <BackLink />
        <LoadStatus state={state} onRetry={reload} label="Cargando el grupo…" />
      </div>
    )
  }

  const { group, moment } = state.data
  const onChange: OnGroupChange = (updated) => setData((current) => ({ ...current, group: updated }))
  const status = groupStatus(group)
  const today = todayIso()
  const gradingClosed = moment.gradingEnd !== null && today > moment.gradingEnd

  return (
    <div className="flex flex-col gap-6">
      <BackLink />
      <div className="flex flex-wrap items-center gap-3">
        <h1 className="text-2xl font-bold tracking-tight">{group.name}</h1>
        <Badge variant={status.variant}>{status.label}</Badge>
      </div>

      <Card className="bg-muted/40">
        <CardContent className="flex flex-wrap gap-x-6 gap-y-2 text-sm">
          <span className="font-semibold">{moment.status === 'LAUNCHED' ? 'Momento lanzado' : 'En configuración'}</span>
          <span className="text-muted-foreground">
            Entregas {formatWindow(moment.submissionStart, moment.submissionEnd)}
          </span>
          <span className="text-muted-foreground">
            Calificación {formatWindow(moment.gradingStart, moment.gradingEnd)}
          </span>
          {gradingClosed && <span className="font-medium">La calificación ya cerró: el grupo no se puede modificar.</span>}
        </CardContent>
      </Card>

      <ProjectTitleSection group={group} moment={moment} today={today} onChange={onChange} />
      <MembersSection group={group} disabled={gradingClosed} onChange={onChange} />
      <DatesSection group={group} moment={moment} today={today} disabled={gradingClosed} onChange={onChange} />
      <EvaluatorsSection group={group} disabled={gradingClosed} onChange={onChange} />
      {moment.status === 'CONFIGURATION' && group.memberCount === 0 && <DeleteSection group={group} />}
    </div>
  )
}

function BackLink() {
  return (
    <Link to="/groups" className="text-muted-foreground flex items-center gap-2 text-sm hover:underline">
      <ArrowLeft className="size-4" aria-hidden />
      Mis grupos
    </Link>
  )
}

// ------------------------------------------------------------------ título

function ProjectTitleSection({
  group,
  moment,
  today,
  onChange,
}: {
  group: Group
  moment: MomentSummary
  today: string
  onChange: OnGroupChange
}) {
  const [title, setTitle] = useState(group.projectTitle ?? '')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const closed = moment.submissionEnd !== null && today > moment.submissionEnd

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setSaving(true)
    try {
      onChange(await groupService.updateProjectTitle(group.id, title.trim()))
    } catch (saveError) {
      setError(getErrorMessage(saveError))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Proyecto de aula</CardTitle>
        <CardDescription>
          Normalmente lo escriben los integrantes; se puede cambiar hasta que cierre la ventana de entregas.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4 sm:flex-row sm:items-end" onSubmit={submit} noValidate>
          <div className="flex-1">
            <FormField
              label="Título del proyecto"
              value={title}
              maxLength={200}
              disabled={closed}
              onChange={(event) => setTitle(event.target.value)}
            />
          </div>
          <Button type="submit" disabled={closed || saving || !title.trim() || title.trim() === group.projectTitle}>
            Guardar título
          </Button>
        </form>
        <div className="mt-3">
          <FormError message={error} />
        </div>
      </CardContent>
    </Card>
  )
}

// ------------------------------------------------------------------ integrantes

function MembersSection({ group, disabled, onChange }: { group: Group; disabled: boolean; onChange: OnGroupChange }) {
  const [error, setError] = useState<string | null>(null)
  const [adding, setAdding] = useState(false)

  const remove = async (studentId: number, name: string) => {
    if (!window.confirm(`¿Quitar a ${name} del grupo?`)) return
    setError(null)
    try {
      await groupService.removeMember(group.id, studentId)
      onChange(await groupService.get(group.id))
    } catch (removeError) {
      setError(getErrorMessage(removeError))
    }
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <CardTitle>
            Integrantes ({group.memberCount} de {group.maxMembers})
          </CardTitle>
          <Button variant="outline" size="sm" disabled={disabled} onClick={() => setAdding((open) => !open)}>
            Agregar estudiante sin grupo
          </Button>
        </div>
        <CardDescription>
          Quitar un integrante solo se puede mientras el grupo no tenga evaluaciones enviadas.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {adding && (
          <AddMemberPanel
            group={group}
            onAdded={(updated) => {
              setAdding(false)
              onChange(updated)
            }}
          />
        )}
        <FormError message={error} />
        {group.members.length === 0 ? (
          <p className="text-muted-foreground text-sm">El grupo todavía no tiene integrantes.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground border-b">
                <tr>
                  <th className="py-2 pr-4 font-medium">Nombre</th>
                  <th className="py-2 pr-4 font-medium">Código</th>
                  <th className="py-2 pr-4 font-medium">Correo</th>
                  <th className="py-2 font-medium">
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {group.members.map((member) => (
                  <tr key={member.id} className="border-b last:border-0">
                    <td className="py-3 pr-4">{member.fullName}</td>
                    <td className="py-3 pr-4">{member.studentCode}</td>
                    <td className="py-3 pr-4 break-all">{member.email}</td>
                    <td className="py-3 text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        disabled={disabled}
                        onClick={() => void remove(member.id, member.fullName)}
                        aria-label={`Quitar a ${member.fullName}`}
                      >
                        Quitar
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </CardContent>
    </Card>
  )
}

function AddMemberPanel({ group, onAdded }: { group: Group; onAdded: OnGroupChange }) {
  const students = useLoad<StudentWithoutGroup[]>(() => groupService.studentsWithoutGroup(group.momentId), [
    group.momentId,
  ])
  const [studentId, setStudentId] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const add = async () => {
    setError(null)
    setSaving(true)
    try {
      onAdded(await groupService.addMember(group.id, Number(studentId)))
    } catch (addError) {
      setError(getErrorMessage(addError))
      setSaving(false)
    }
  }

  return (
    <div className="bg-muted/40 flex flex-col gap-3 rounded-md border p-4">
      <LoadStatus state={students.state} onRetry={students.reload} label="Cargando estudiantes sin grupo…" />
      {students.state.kind === 'success' && students.state.data.length === 0 && (
        <p className="text-muted-foreground text-sm">Todos los estudiantes del momento ya tienen grupo.</p>
      )}
      {students.state.kind === 'success' && students.state.data.length > 0 && (
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <label className="flex flex-1 flex-col gap-2 text-sm font-medium">
            Estudiante sin grupo
            <select
              className="border-input bg-background h-9 rounded-md border px-3 text-sm font-normal"
              value={studentId}
              onChange={(event) => setStudentId(event.target.value)}
            >
              <option value="">Elige un estudiante</option>
              {students.state.data.map((student) => (
                <option key={student.id} value={student.id}>
                  {student.fullName} · {student.studentCode} · semestre {student.currentSemester}
                </option>
              ))}
            </select>
          </label>
          <Button disabled={!studentId || saving} onClick={() => void add()}>
            Agregar
          </Button>
        </div>
      )}
      <FormError message={error} />
    </div>
  )
}

// ------------------------------------------------------------------ fechas

function DatesSection({
  group,
  moment,
  today,
  disabled,
  onChange,
}: {
  group: Group
  moment: MomentSummary
  today: string
  disabled: boolean
  onChange: OnGroupChange
}) {
  const [submissionDeadline, setSubmissionDeadline] = useState(group.submissionDeadline ?? '')
  const [evaluationDate, setEvaluationDate] = useState(group.evaluationDate ?? '')
  const [error, setError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [saving, setSaving] = useState(false)

  const changes: UpdateGroupRequest = {}
  if (submissionDeadline && submissionDeadline !== group.submissionDeadline) {
    changes.submissionDeadline = submissionDeadline
  }
  if (evaluationDate && evaluationDate !== group.evaluationDate) {
    changes.evaluationDate = evaluationDate
  }
  const hasChanges = Object.keys(changes).length > 0

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setSaved(false)
    setSaving(true)
    try {
      onChange(await groupService.update(group.id, changes))
      setSaved(true)
    } catch (saveError) {
      setError(getErrorMessage(saveError))
    } finally {
      setSaving(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Fechas del grupo</CardTitle>
        <CardDescription>Ninguna puede ser anterior a hoy.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
          <div className="grid gap-4 md:grid-cols-2">
            <DateField
              label="Fecha límite de entrega"
              help={`Dentro de la ventana de entregas (${formatWindow(moment.submissionStart, moment.submissionEnd)}). Lo que llegue después se marca como tardío.`}
              value={submissionDeadline}
              min={maxDate(today, moment.submissionStart)}
              max={moment.submissionEnd}
              disabled={disabled || !moment.submissionStart}
              onChange={setSubmissionDeadline}
            />
            <DateField
              label="Fecha de evaluación"
              help={`Dentro de la ventana de calificación (${formatWindow(moment.gradingStart, moment.gradingEnd)}). Desde ese día se puede calificar.`}
              value={evaluationDate}
              min={maxDate(today, moment.gradingStart)}
              max={moment.gradingEnd}
              disabled={disabled || !moment.gradingStart}
              onChange={setEvaluationDate}
            />
          </div>
          <FormError message={error} />
          <div className="flex items-center gap-3">
            <Button type="submit" disabled={disabled || saving || !hasChanges}>
              Guardar fechas
            </Button>
            {saved && !hasChanges && <span className="text-muted-foreground text-sm">Fechas guardadas.</span>}
          </div>
        </form>
      </CardContent>
    </Card>
  )
}

function DateField({
  label,
  help,
  value,
  min,
  max,
  disabled,
  onChange,
}: {
  label: string
  help: string
  value: string
  min: string | null
  max: string | null
  disabled: boolean
  onChange: (value: string) => void
}) {
  return (
    <div className="flex flex-col gap-2">
      <FormField
        label={label}
        type="date"
        value={value}
        min={min ?? undefined}
        max={max ?? undefined}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
      />
      <p className="text-muted-foreground text-xs">{help}</p>
    </div>
  )
}

/** La mayor de dos fechas `YYYY-MM-DD` (la segunda puede faltar). */
function maxDate(first: string, second: string | null): string {
  return second && second > first ? second : first
}

// ------------------------------------------------------------------ evaluadores

function EvaluatorsSection({
  group,
  disabled,
  onChange,
}: {
  group: Group
  disabled: boolean
  onChange: OnGroupChange
}) {
  const [error, setError] = useState<string | null>(null)
  const [assigning, setAssigning] = useState(false)

  const remove = async (evaluatorId: number, name: string) => {
    if (!window.confirm(`¿Quitar a ${name} de los evaluadores del grupo?`)) return
    setError(null)
    try {
      await groupService.removeEvaluator(group.id, evaluatorId)
      onChange(await groupService.get(group.id))
    } catch (removeError) {
      setError(getErrorMessage(removeError))
    }
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <CardTitle>Evaluadores externos</CardTitle>
          <Button variant="outline" size="sm" disabled={disabled} onClick={() => setAssigning((open) => !open)}>
            Asignar
          </Button>
        </div>
        <CardDescription>No se puede quitar a un evaluador que ya empezó a calificar el grupo.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {assigning && (
          <AssignEvaluatorPanel
            group={group}
            onAssigned={(updated) => {
              setAssigning(false)
              onChange(updated)
            }}
          />
        )}
        <FormError message={error} />
        {group.evaluators.length === 0 ? (
          <p className="text-muted-foreground text-sm">El grupo todavía no tiene evaluadores externos.</p>
        ) : (
          <ul className="flex flex-col divide-y">
            {group.evaluators.map((evaluator) => (
              <li key={evaluator.id} className="flex items-center justify-between gap-3 py-3 text-sm">
                <span>
                  <span className="font-medium">{evaluator.fullName}</span>
                  <span className="text-muted-foreground"> · {evaluator.evaluatorType}</span>
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={disabled}
                  onClick={() => void remove(evaluator.id, evaluator.fullName)}
                  aria-label={`Quitar a ${evaluator.fullName}`}
                >
                  Quitar
                </Button>
              </li>
            ))}
          </ul>
        )}
      </CardContent>
    </Card>
  )
}

/**
 * Asignar un evaluador (wireframe DO-03): primero se busca por correo o documento
 * exactos, para reutilizar la cuenta si otro docente ya lo creó.
 */
function AssignEvaluatorPanel({ group, onAssigned }: { group: Group; onAssigned: OnGroupChange }) {
  const [query, setQuery] = useState('')
  const [found, setFound] = useState<UserSummary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const search = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setFound(null)
    setBusy(true)
    try {
      setFound(await groupService.lookupEvaluator(query.trim()))
    } catch (lookupError) {
      setError(getErrorMessage(lookupError))
    } finally {
      setBusy(false)
    }
  }

  const assign = async (evaluator: UserSummary) => {
    setError(null)
    setBusy(true)
    try {
      onAssigned(await groupService.assignEvaluator(group.id, evaluator.id))
    } catch (assignError) {
      setError(getErrorMessage(assignError))
      setBusy(false)
    }
  }

  return (
    <div className="bg-muted/40 flex flex-col gap-3 rounded-md border p-4">
      <form className="flex flex-col gap-3 sm:flex-row sm:items-end" onSubmit={search} noValidate>
        <div className="flex-1">
          <FormField
            label="Correo o número de documento completos"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
        </div>
        <Button type="submit" disabled={busy || !query.trim()}>
          <Search aria-hidden />
          Buscar
        </Button>
      </form>
      {found && (
        <div className="bg-background flex flex-wrap items-center justify-between gap-3 rounded-md border p-3 text-sm">
          <span>
            <span className="font-medium">{found.fullName}</span>
            <span className="text-muted-foreground"> · {found.email}</span>
          </span>
          <Button size="sm" disabled={busy} onClick={() => void assign(found)}>
            Asignar
          </Button>
        </div>
      )}
      <FormError message={error} />
      <p className="text-muted-foreground text-xs">
        ¿No aparece?{' '}
        <Link to="/external-evaluators" className="underline">
          Créalo en Evaluadores externos
        </Link>{' '}
        y vuelve a buscarlo.
      </p>
    </div>
  )
}

// ------------------------------------------------------------------ eliminar

function DeleteSection({ group }: { group: Group }) {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  const remove = async () => {
    if (!window.confirm(`¿Eliminar el ${group.name}? Esta acción no se puede deshacer.`)) return
    setError(null)
    try {
      await groupService.remove(group.id)
      navigate('/groups', { replace: true })
    } catch (removeError) {
      setError(getErrorMessage(removeError))
    }
  }

  return (
    <Card className="border-destructive/40">
      <CardHeader>
        <CardTitle>Eliminar grupo</CardTitle>
        <CardDescription>Solo se puede antes de lanzar el momento y mientras no tenga integrantes.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <FormError message={error} />
        <div>
          <Button variant="destructive" onClick={() => void remove()}>
            Eliminar grupo
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}
