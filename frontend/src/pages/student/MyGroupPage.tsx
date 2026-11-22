import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Upload } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { LoadStatus } from '@/components/LoadStatus'
import { SubmissionHistoryCard } from '@/components/submissions/SubmissionHistoryCard'
import { FileDropzone } from '@/components/forms/FileDropzone'
import { FormError, FormField } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatDate, todayIso } from '@/lib/dates'
import { formatSize, presentationError, PRESENTATION_EXTENSIONS, zipError } from '@/lib/files'
import { useLoad } from '@/lib/useLoad'
import { groupService } from '@/services/api/groupService'
import { getErrorMessage, isCanceled } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import { submissionService } from '@/services/api/submissionService'
import type { Group } from '@/types/group'
import type { MomentSummary } from '@/types/moment'
import type { Submission } from '@/types/submission'

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
  const maxVersions = history.state.kind === 'success' ? history.state.data.maxVersions : null
  const nextVersion = (versions[0]?.versionNumber ?? 0) + 1

  const addVersion = (created: Submission) =>
    history.setData((current) => ({
      ...current,
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
            <UploadCard
              group={group}
              moment={moment}
              nextVersion={nextVersion}
              uploadedVersions={versions.length}
              maxVersions={maxVersions}
              onUploaded={addVersion}
            />
          )
        ) : (
          <p className="text-muted-foreground text-sm">{closedWindowMessage(moment)}</p>
        )}
        <SubmissionHistoryCard
          state={history.state}
          onRetry={history.reload}
          emptyMessage="Tu grupo todavía no ha subido entregas."
        />
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
  uploadedVersions,
  maxVersions,
  onUploaded,
}: {
  group: Group
  moment: MomentSummary
  nextVersion: number
  /** Versiones que el grupo ya subió (el tope cuenta todas, también las tardías). */
  uploadedVersions: number
  /** Tope de versiones por grupo; `null` si no está configurado. */
  maxVersions: number | null
  onUploaded: (submission: Submission) => void
}) {
  const [zip, setZip] = useState<File | null>(null)
  const [presentation, setPresentation] = useState<File | null>(null)
  const [errors, setErrors] = useState<{ zip?: string; presentation?: string }>({})
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [progress, setProgress] = useState<number | null>(null)
  const abortRef = useRef<AbortController | null>(null)
  // Cambia después de cada carga para vaciar los campos de archivo.
  const [formKey, setFormKey] = useState(0)

  // Si el estudiante sale de la página a mitad de una carga, se cancela.
  useEffect(() => () => abortRef.current?.abort(), [])

  const deadline = group.submissionDeadline ?? moment.submissionEnd
  const late = deadline !== null && todayIso() > deadline
  const limitReached = maxVersions !== null && uploadedVersions >= maxVersions
  const uploading = progress !== null
  const totalSize = (zip?.size ?? 0) + (presentation?.size ?? 0)

  // Se valida apenas se elige o se suelta el archivo, no solo al pulsar "Subir".
  const chooseZip = (file: File | null) => {
    setZip(file)
    setErrors((current) => ({ ...current, zip: file ? (zipError(file) ?? undefined) : undefined }))
  }
  const choosePresentation = (file: File | null) => {
    setPresentation(file)
    setErrors((current) => ({ ...current, presentation: presentationError(file) ?? undefined }))
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setNotice(null)
    const found = { zip: zipError(zip) ?? undefined, presentation: presentationError(presentation) ?? undefined }
    setErrors(found)
    if (found.zip || found.presentation || !zip) return

    const controller = new AbortController()
    abortRef.current = controller
    setProgress(0)
    try {
      const created = await submissionService.upload(group.id, zip, presentation, setProgress, controller.signal)
      onUploaded(created)
      setNotice(`Se subió la versión ${created.versionNumber}${created.late ? ' (tardía)' : ''}.`)
      setZip(null)
      setPresentation(null)
      setFormKey((key) => key + 1)
    } catch (uploadError) {
      if (isCanceled(uploadError)) {
        setNotice('Se canceló la subida. Tus archivos siguen elegidos por si quieres intentarlo de nuevo.')
      } else {
        setError(getErrorMessage(uploadError))
      }
    } finally {
      abortRef.current = null
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
        {maxVersions !== null && (
          <CardDescription>
            Tu grupo ha subido {uploadedVersions} de {maxVersions} versiones permitidas.
          </CardDescription>
        )}
      </CardHeader>
      <CardContent>
        {limitReached ? (
          <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
            Tu grupo ya subió las {maxVersions} versiones permitidas en este momento, así que no puede subir más. Se
            califica la versión marcada en el historial.
          </p>
        ) : (
          <form
            key={formKey}
            className="flex flex-col gap-4"
            onSubmit={submit}
            // Un archivo soltado fuera de los recuadros no debe hacer que el navegador lo abra.
            onDragOver={(event) => event.preventDefault()}
            onDrop={(event) => event.preventDefault()}
            noValidate
          >
            <div className="grid gap-4 md:grid-cols-2">
              <FileDropzone
                label="Proyecto (.zip)"
                hint="Hasta 100 MB"
                accept=".zip"
                file={zip}
                error={errors.zip}
                disabled={uploading}
                onChange={chooseZip}
              />
              <FileDropzone
                label="Presentación (opcional)"
                hint=".pdf, .docx o .pptx · hasta 30 MB"
                accept={PRESENTATION_EXTENSIONS.join(',')}
                file={presentation}
                error={errors.presentation}
                disabled={uploading}
                onChange={choosePresentation}
              />
            </div>
            <p className="text-muted-foreground text-sm">
              Será la versión {nextVersion}
              {maxVersions !== null && ` de ${maxVersions}`}.{' '}
              {deadline &&
                (late
                  ? `Tu fecha límite fue el ${formatDate(deadline)}: esta versión quedará marcada como tardía.`
                  : `Tu fecha límite es el ${formatDate(deadline)}: después de esa fecha las versiones se ` +
                    'marcan como tardías.')}
            </p>
            {uploading && <UploadProgress percent={progress} totalSize={totalSize} />}
            <FormError message={error} />
            {notice && (
              <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
                {notice}
              </p>
            )}
            <div className="flex flex-wrap items-center gap-3">
              <Button type="submit" disabled={uploading}>
                <Upload aria-hidden />
                {uploading ? 'Subiendo…' : `Subir versión ${nextVersion}`}
              </Button>
              {uploading && (
                <Button type="button" variant="outline" onClick={() => abortRef.current?.abort()}>
                  Cancelar subida
                </Button>
              )}
            </div>
          </form>
        )}
      </CardContent>
    </Card>
  )
}

/** Barra de avance de la carga, con lo subido en MB. */
function UploadProgress({ percent, totalSize }: { percent: number; totalSize: number }) {
  const loaded = Math.round((totalSize * percent) / 100)
  return (
    <div className="flex flex-col gap-1">
      <div
        role="progressbar"
        aria-label="Avance de la subida"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuenow={percent}
        className="bg-muted h-2 w-full overflow-hidden rounded-full"
      >
        <div className="bg-primary h-full transition-[width]" style={{ width: `${percent}%` }} />
      </div>
      <span className="text-muted-foreground text-xs">
        {percent} % · {formatSize(loaded)} de {formatSize(totalSize)}
      </span>
    </div>
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
