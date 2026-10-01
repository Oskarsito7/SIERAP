import { useState, type FormEvent } from 'react'
import { Download, Upload } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { LoadStatus } from '@/components/LoadStatus'
import { FormError, FormField } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { formatDate, formatDateTime, todayIso } from '@/lib/dates'
import { formatSize, presentationError, PRESENTATION_EXTENSIONS, saveFile, zipError } from '@/lib/files'
import { useLoad, type LoadState } from '@/lib/useLoad'
import { groupService } from '@/services/api/groupService'
import { getErrorMessage } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import { submissionService } from '@/services/api/submissionService'
import type { Group } from '@/types/group'
import type { MomentSummary } from '@/types/moment'
import type { Submission, SubmissionHistory } from '@/types/submission'

import { formatOptionalDate } from '../groups/groupDisplay'

/**
 * "Mi grupo y entregas" del estudiante (wireframe ES-03): sus grupos (uno por momento,
 * el más reciente primero), el título del proyecto, la carga de versiones nuevas
 * mientras la ventana de entregas está abierta y el historial con sus descargas. El
 * grupo elegido queda en la dirección (`?groupId=`).
 *
 * Las ventanas se toman de los momentos del periodo actual; si el momento del grupo no
 * está entre ellos (p. ej. un periodo anterior), solo se muestra el historial.
 */
export function MyGroupPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const data = useLoad(async () => {
    const [groups, moments] = await Promise.all([groupService.myGroups(), momentService.list()])
    return { groups, moments }
  }, [])

  if (data.state.kind !== 'success') {
    return (
      <div className="flex flex-col gap-6">
        <h1 className="text-2xl font-bold tracking-tight">Mi grupo y entregas</h1>
        <LoadStatus state={data.state} onRetry={data.reload} label="Cargando tu grupo…" />
      </div>
    )
  }

  const { groups, moments } = data.state.data
  const requestedId = Number(searchParams.get('groupId'))
  const group = groups.find((item) => item.id === requestedId) ?? groups[0]

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-bold tracking-tight">Mi grupo y entregas</h1>

      {!group && (
        <Card className="max-w-xl">
          <CardHeader>
            <CardTitle>Todavía no estás en un grupo</CardTitle>
            <CardDescription>
              Inscríbete en uno de los grupos de tu momento mientras la conformación esté abierta.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Button asChild>
              <Link to="/moment-groups">Ver los grupos del momento</Link>
            </Button>
          </CardContent>
        </Card>
      )}

      {group && groups.length > 1 && (
        <label className="flex max-w-sm flex-col gap-2 text-sm font-medium">
          Grupo
          <select
            className="border-input bg-background h-9 rounded-md border px-3 text-sm font-normal"
            value={group.id}
            onChange={(event) => setSearchParams({ groupId: event.target.value })}
          >
            {groups.map((item) => (
              <option key={item.id} value={item.id}>
                {item.momentName} · {item.name}
              </option>
            ))}
          </select>
        </label>
      )}

      {group && (
        <GroupView
          key={group.id}
          initialGroup={group}
          moment={moments.find((moment) => moment.id === group.momentId)}
        />
      )}
    </div>
  )
}

/** Entregas y datos de un grupo del estudiante. */
function GroupView({ initialGroup, moment }: { initialGroup: Group; moment?: MomentSummary }) {
  const [group, setGroup] = useState(initialGroup)
  const history = useLoad(() => submissionService.history(group.id), [group.id])

  const versions = history.state.kind === 'success' ? history.state.data.versions : []
  const nextVersion = (versions[0]?.versionNumber ?? 0) + 1

  const addVersion = (created: Submission) =>
    history.setData((current) => ({
      versions: [
        created,
        ...current.versions.map((version) => (created.toGrade ? { ...version, toGrade: false } : version)),
      ],
      currentPresentation: created.presentationFileName ? created : current.currentPresentation,
    }))

  return (
    <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
      <div className="flex flex-col gap-6">
        {moment?.submissionOpen ? (
          history.state.kind === 'success' && (
            <UploadCard group={group} moment={moment} nextVersion={nextVersion} onUploaded={addVersion} />
          )
        ) : (
          <p className="text-muted-foreground text-sm">{closedWindowMessage(moment)}</p>
        )}
        <HistoryCard state={history.state} onRetry={history.reload} />
      </div>
      <GroupInfoCard group={group} moment={moment} onChange={setGroup} />
    </div>
  )
}

/** Formulario para subir una versión nueva: `.zip` obligatorio y presentación opcional. */
function UploadCard({
  group,
  moment,
  nextVersion,
  onUploaded,
}: {
  group: Group
  moment: MomentSummary
  nextVersion: number
  onUploaded: (submission: Submission) => void
}) {
  const [zip, setZip] = useState<File | null>(null)
  const [presentation, setPresentation] = useState<File | null>(null)
  const [errors, setErrors] = useState<{ zip?: string; presentation?: string }>({})
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [progress, setProgress] = useState<number | null>(null)
  // Cambia después de cada carga para vaciar los campos de archivo.
  const [formKey, setFormKey] = useState(0)

  const deadline = group.submissionDeadline ?? moment.submissionEnd
  const late = deadline !== null && todayIso() > deadline

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setNotice(null)
    const found = { zip: zipError(zip) ?? undefined, presentation: presentationError(presentation) ?? undefined }
    setErrors(found)
    if (found.zip || found.presentation || !zip) return

    setProgress(0)
    try {
      const created = await submissionService.upload(group.id, zip, presentation, setProgress)
      onUploaded(created)
      setNotice(`Se subió la versión ${created.versionNumber}${created.late ? ' (tardía)' : ''}.`)
      setZip(null)
      setPresentation(null)
      setFormKey((key) => key + 1)
    } catch (uploadError) {
      setError(getErrorMessage(uploadError))
    } finally {
      setProgress(null)
    }
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-wrap items-center justify-between gap-2">
          <CardTitle>Subir una versión nueva</CardTitle>
          {moment.submissionEnd && <Badge>Entregas abiertas hasta el {formatDate(moment.submissionEnd)}</Badge>}
        </div>
      </CardHeader>
      <CardContent>
        <form key={formKey} className="flex flex-col gap-4" onSubmit={submit} noValidate>
          <div className="grid gap-4 md:grid-cols-2">
            <FileField
              label="Proyecto (.zip)"
              hint="Hasta 100 MB"
              accept=".zip"
              file={zip}
              error={errors.zip}
              onChange={setZip}
            />
            <FileField
              label="Presentación (opcional)"
              hint=".pdf, .docx o .pptx · hasta 30 MB"
              accept={PRESENTATION_EXTENSIONS.join(',')}
              file={presentation}
              error={errors.presentation}
              onChange={setPresentation}
            />
          </div>
          <p className="text-muted-foreground text-sm">
            Será la versión {nextVersion}.{' '}
            {deadline &&
              (late
                ? `Tu fecha límite fue el ${formatDate(deadline)}: esta versión quedará marcada como tardía.`
                : `Tu fecha límite es el ${formatDate(deadline)}: después de esa fecha las versiones se ` +
                  'marcan como tardías.')}
          </p>
          <FormError message={error} />
          {notice && (
            <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
              {notice}
            </p>
          )}
          <div className="flex items-center gap-3">
            <Button type="submit" disabled={progress !== null}>
              <Upload aria-hidden />
              {progress !== null ? `Subiendo… ${progress} %` : `Subir versión ${nextVersion}`}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}

/** Campo de archivo con su nombre y tamaño una vez elegido. */
function FileField({
  label,
  hint,
  accept,
  file,
  error,
  onChange,
}: {
  label: string
  hint: string
  accept: string
  file: File | null
  error?: string
  onChange: (file: File | null) => void
}) {
  const id = label.replace(/\W+/g, '-').toLowerCase()
  return (
    <div className="flex flex-col gap-2 rounded-md border border-dashed p-4">
      <Label htmlFor={id}>{label}</Label>
      <span className="text-muted-foreground text-xs">{hint}</span>
      {/* El campo nativo queda oculto porque su botón sale en el idioma del navegador. */}
      <div className="flex flex-wrap items-center gap-3">
        <input
          id={id}
          type="file"
          accept={accept}
          className="peer sr-only"
          aria-invalid={error ? true : undefined}
          onChange={(event) => onChange(event.target.files?.[0] ?? null)}
        />
        <Button
          variant="outline"
          size="sm"
          className="peer-focus-visible:ring-ring/50 cursor-pointer peer-focus-visible:ring-[3px]"
          asChild
        >
          <label htmlFor={id}>Elegir archivo</label>
        </Button>
        <span className="text-sm break-all">
          {file ? `${file.name} · ${formatSize(file.size)}` : 'Ningún archivo elegido'}
        </span>
      </div>
      {error && <p className="text-destructive text-sm">{error}</p>}
    </div>
  )
}

/** Historial de versiones con sus descargas. */
function HistoryCard({
  state,
  onRetry,
}: {
  state: LoadState<SubmissionHistory>
  onRetry: () => void
}) {
  const [error, setError] = useState<string | null>(null)

  const download = async (submission: Submission, kind: 'zip' | 'presentation') => {
    setError(null)
    try {
      if (kind === 'zip') {
        saveFile(await submissionService.downloadZip(submission.id), submission.zipFileName)
      } else {
        saveFile(await submissionService.downloadPresentation(submission.id), submission.presentationFileName ?? '')
      }
    } catch (downloadError) {
      setError(getErrorMessage(downloadError))
    }
  }

  const current = state.kind === 'success' ? state.data.currentPresentation : null
  const latest = state.kind === 'success' ? state.data.versions[0] : undefined

  return (
    <Card>
      <CardHeader>
        <CardTitle>Historial de entregas</CardTitle>
        {current && latest && current.id !== latest.id && (
          <CardDescription>
            La presentación vigente es {current.presentationFileName}, de la versión {current.versionNumber}.
          </CardDescription>
        )}
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <LoadStatus state={state} onRetry={onRetry} label="Cargando las entregas…" />
        <FormError message={error} />
        {state.kind === 'success' && state.data.versions.length === 0 && (
          <p className="text-muted-foreground text-sm">Tu grupo todavía no ha subido entregas.</p>
        )}
        {state.kind === 'success' && state.data.versions.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground border-b">
                <tr>
                  <th className="py-2 pr-4 font-medium">Versión</th>
                  <th className="py-2 pr-4 font-medium">Subida</th>
                  <th className="py-2 pr-4 font-medium">Proyecto</th>
                  <th className="py-2 pr-4 font-medium">Presentación</th>
                  <th className="py-2 font-medium">Estado</th>
                </tr>
              </thead>
              <tbody>
                {state.data.versions.map((version) => (
                  <tr key={version.id} className="border-b align-top last:border-0">
                    <td className="py-3 pr-4 font-semibold">v{version.versionNumber}</td>
                    <td className="py-3 pr-4 whitespace-nowrap">{formatDateTime(version.submittedAt)}</td>
                    <td className="py-3 pr-4">
                      <Button
                        variant="link"
                        className="h-auto p-0 break-all whitespace-normal"
                        aria-label={`Descargar ${version.zipFileName}`}
                        onClick={() => void download(version, 'zip')}
                      >
                        <Download aria-hidden />
                        {version.zipFileName}
                      </Button>
                      <span className="text-muted-foreground block text-xs">{formatSize(version.zipSize)}</span>
                    </td>
                    <td className="py-3 pr-4">
                      {version.presentationFileName ? (
                        <Button
                          variant="link"
                          className="h-auto p-0 break-all whitespace-normal"
                          aria-label={`Descargar ${version.presentationFileName}`}
                          onClick={() => void download(version, 'presentation')}
                        >
                          <Download aria-hidden />
                          {version.presentationFileName}
                        </Button>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td className="py-3">
                      <div className="flex flex-wrap gap-1">
                        <Badge variant={version.late ? 'destructive' : 'outline'}>
                          {version.late ? 'Tardía' : 'A tiempo'}
                        </Badge>
                        {version.toGrade && <Badge>Se califica</Badge>}
                      </div>
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

/** Datos del grupo: título del proyecto (editable), fechas e integrantes. */
function GroupInfoCard({
  group,
  moment,
  onChange,
}: {
  group: Group
  moment?: MomentSummary
  onChange: (group: Group) => void
}) {
  const userId = useAuth().user?.id
  const [title, setTitle] = useState(group.projectTitle ?? '')
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)

  const submissionEnd = moment?.submissionEnd ?? null
  const editable = submissionEnd === null || todayIso() <= submissionEnd

  const save = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setSaved(false)
    if (!title.trim()) {
      setError('Escribe el título del proyecto.')
      return
    }
    setSaving(true)
    try {
      onChange(await groupService.updateProjectTitle(group.id, title.trim()))
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
        <CardTitle>{group.name}</CardTitle>
        <CardDescription>
          {group.momentName} · Docente: {group.teacher.fullName}
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-6">
        {editable ? (
          <form className="flex flex-col gap-3" onSubmit={save} noValidate>
            <FormField
              label="Título del proyecto"
              value={title}
              maxLength={200}
              onChange={(event) => setTitle(event.target.value)}
            />
            {submissionEnd && (
              <p className="text-muted-foreground text-xs">
                Lo pueden cambiar los integrantes hasta el {formatDate(submissionEnd)}.
              </p>
            )}
            <FormError message={error} />
            {saved && (
              <p role="status" className="text-sm">
                Título guardado.
              </p>
            )}
            <Button type="submit" variant="outline" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar título'}
            </Button>
          </form>
        ) : (
          <div className="flex flex-col gap-1 text-sm">
            <span className="font-medium">Título del proyecto</span>
            <span>{group.projectTitle ?? 'Sin título'}</span>
          </div>
        )}

        <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
          <dt className="text-muted-foreground">Entrega hasta</dt>
          <dd>{formatOptionalDate(group.submissionDeadline)}</dd>
          <dt className="text-muted-foreground">Evaluación</dt>
          <dd>{formatOptionalDate(group.evaluationDate)}</dd>
        </dl>

        <div className="flex flex-col gap-2 text-sm">
          <span className="font-medium">Integrantes</span>
          <ul className="list-disc pl-5">
            {group.members.map((member) => (
              <li key={member.id}>
                {member.fullName}
                {member.id === userId && ' (tú)'}
              </li>
            ))}
          </ul>
        </div>
      </CardContent>
    </Card>
  )
}

/** Por qué no se puede subir una versión en este momento. */
function closedWindowMessage(moment?: MomentSummary): string {
  if (!moment) return 'Las entregas de este momento ya no están abiertas.'
  if (moment.status !== 'LAUNCHED') {
    return 'Podrás subir entregas cuando el momento se lance y abra su ventana de entregas.'
  }
  if (moment.submissionStart && todayIso() < moment.submissionStart) {
    return `La ventana de entregas abre el ${formatDate(moment.submissionStart)}.`
  }
  return moment.submissionEnd
    ? `La ventana de entregas cerró el ${formatDate(moment.submissionEnd)}.`
    : 'La ventana de entregas no está abierta.'
}
