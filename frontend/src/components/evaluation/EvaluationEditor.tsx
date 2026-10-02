import { useId, useMemo, useState } from 'react'
import { Download } from 'lucide-react'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { cn } from '@/lib/utils'
import { formatDateTime } from '@/lib/dates'
import { formatScore } from '@/lib/scores'
import { downloadSubmissionFile } from '@/lib/submissionDownload'
import { useLoad } from '@/lib/useLoad'
import { evaluationService } from '@/services/api/evaluationService'
import { getErrorMessage } from '@/services/api/httpClient'
import { submissionService } from '@/services/api/submissionService'
import type { CriterionForm, EvaluationForm, SaveEvaluationRequest } from '@/types/evaluation'
import type { SubmissionHistory } from '@/types/submission'

/** Nivel elegido por criterio (`criterionId` → `levelId`). */
type Levels = Record<number, number>

/** Ajustes por estudiante (`studentId` → nivel por criterio). */
type Adjustments = Record<number, Levels>

/**
 * Calificación propia de un grupo (wireframes DO-05, DO-06 y EV-02), la misma para el
 * docente y el evaluador externo. Se califica al grupo una vez, un nivel por criterio,
 * y desde la lista de integrantes se ajustan los criterios en que un estudiante se
 * destacó o se quedó atrás (ADR-018). Se guarda como borrador, se envía cuando está
 * completa y se puede corregir mientras la calificación esté abierta; la API vuelve a
 * validar todo y responde 422 con el motivo.
 */
export function EvaluationEditor({ groupId }: { groupId: number }) {
  const form = useLoad(() => evaluationService.form(groupId), [groupId])
  const history = useLoad(() => submissionService.history(groupId), [groupId])

  if (form.state.kind !== 'success') {
    return <LoadStatus state={form.state} onRetry={form.reload} label="Cargando tu evaluación…" />
  }
  const data = form.state.data
  return (
    <EditorBody
      // Al guardar llega la evaluación de la API y el formulario se reinicia con ella.
      key={`${data.id ?? 'new'}-${data.evaluatedAt ?? ''}-${data.status ?? ''}`}
      form={data}
      history={history.state.kind === 'success' ? history.state.data : null}
      onSaved={(saved) => form.setData(saved)}
    />
  )
}

function EditorBody({
  form,
  history,
  onSaved,
}: {
  form: EvaluationForm
  history: SubmissionHistory | null
  onSaved: (form: EvaluationForm) => void
}) {
  const [levels, setLevels] = useState<Levels>(() => initialLevels(form))
  const [adjustments, setAdjustments] = useState<Adjustments>(() => initialAdjustments(form))
  const [comment, setComment] = useState(form.comment ?? '')
  const [adjusting, setAdjusting] = useState<number | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const criteria = useMemo(() => form.learningOutcomes.flatMap((outcome) => outcome.criteria), [form])
  const scores = useMemo(() => levelScores(criteria), [criteria])
  const graded = criteria.filter((criterion) => levels[criterion.id] !== undefined).length
  const complete = graded === criteria.length
  const groupTotal = criteria.reduce((sum, criterion) => sum + (scores[levels[criterion.id]] ?? 0), 0)
  const memberTotal = (studentId: number) =>
    criteria.reduce((sum, criterion) => {
      const levelId = adjustments[studentId]?.[criterion.id] ?? levels[criterion.id]
      return sum + (levelId === undefined ? 0 : scores[levelId])
    }, 0)
  const dirty =
    JSON.stringify(levels) !== JSON.stringify(initialLevels(form)) ||
    JSON.stringify(adjustments) !== JSON.stringify(initialAdjustments(form)) ||
    comment !== (form.comment ?? '')
  const submitted = form.status === 'SUBMITTED'
  const disabled = !form.canEdit || busy

  const request = (): SaveEvaluationRequest => ({
    levels: Object.entries(levels).map(([criterionId, levelId]) => ({ criterionId: Number(criterionId), levelId })),
    comment,
    adjustments: Object.entries(adjustments).flatMap(([studentId, byCriterion]) =>
      Object.entries(byCriterion).map(([criterionId, levelId]) => ({
        studentId: Number(studentId),
        criterionId: Number(criterionId),
        levelId,
      })),
    ),
  })

  const run = async (action: () => Promise<EvaluationForm>) => {
    setError(null)
    setBusy(true)
    try {
      onSaved(await action())
    } catch (actionError) {
      setError(getErrorMessage(actionError))
    } finally {
      setBusy(false)
    }
  }
  const save = () => run(() => evaluationService.save(form.groupId, request()))
  const submit = () =>
    run(async () => {
      if (dirty) await evaluationService.save(form.groupId, request())
      return evaluationService.submit(form.groupId)
    })

  /** Elige el nivel de un estudiante; si es el del grupo, quita el ajuste. */
  const adjust = (studentId: number, criterionId: number, levelId: number) =>
    setAdjustments((current) => {
      const own = { ...current[studentId] }
      if (levels[criterionId] === levelId) delete own[criterionId]
      else own[criterionId] = levelId
      const next = { ...current, [studentId]: own }
      if (Object.keys(own).length === 0) delete next[studentId]
      return next
    })

  /** Cambia el nivel del grupo y descarta los ajustes que quedan iguales a él. */
  const gradeGroup = (criterionId: number, levelId: number) => {
    setLevels((current) => ({ ...current, [criterionId]: levelId }))
    setAdjustments((current) =>
      Object.fromEntries(
        Object.entries(current)
          .map(([studentId, own]) => [
            studentId,
            Object.fromEntries(
              Object.entries(own).filter(([id, level]) => !(Number(id) === criterionId && level === levelId)),
            ),
          ] as const)
          .filter(([, own]) => Object.keys(own).length > 0),
      ),
    )
  }

  const member = form.members.find((item) => item.id === adjusting)

  return (
    <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_20rem]">
      <div className="flex flex-col gap-6">
        <SubmissionBanner form={form} history={history} />
        {!form.canEdit && (
          <p className="bg-muted rounded-md px-3 py-2 text-sm">
            Hoy no puedes modificar esta evaluación: la calificación de este grupo no está abierta.
          </p>
        )}

        {member ? (
          <Card>
            <CardHeader>
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div className="flex flex-col gap-1">
                  <CardTitle>Ajuste individual · {member.fullName}</CardTitle>
                  <CardDescription>
                    Parte de las calificaciones del grupo. Cambia solo lo que este estudiante hizo mejor o peor;
                    elegir el nivel del grupo quita el ajuste.
                  </CardDescription>
                </div>
                <div className="flex flex-wrap gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    disabled={disabled || !adjustments[member.id]}
                    onClick={() =>
                      setAdjustments((current) => {
                        const next = { ...current }
                        delete next[member.id]
                        return next
                      })
                    }
                  >
                    Quitar ajustes
                  </Button>
                  <Button variant="outline" size="sm" onClick={() => setAdjusting(null)}>
                    Volver al grupo
                  </Button>
                </div>
              </div>
            </CardHeader>
            <CardContent className="flex flex-col gap-6">
              <div className="bg-muted/50 grid grid-cols-3 gap-2 rounded-md p-3 text-center text-sm">
                <ScoreFigure label="Nota del grupo" value={formatScore(groupTotal)} />
                <ScoreFigure
                  label={`Nota de ${member.fullName.split(' ')[0]}`}
                  value={formatScore(memberTotal(member.id))}
                />
                <ScoreFigure label="Diferencia" value={signed(memberTotal(member.id) - groupTotal)} />
              </div>
              {form.learningOutcomes.map((outcome) =>
                outcome.criteria.map((criterion) => (
                  <LevelPicker
                    key={criterion.id}
                    title={`${outcome.code} · ${criterion.description}`}
                    criterion={criterion}
                    selected={adjustments[member.id]?.[criterion.id] ?? levels[criterion.id]}
                    groupLevel={levels[criterion.id]}
                    adjusted={adjustments[member.id]?.[criterion.id] !== undefined}
                    disabled={disabled}
                    onPick={(levelId) => adjust(member.id, criterion.id, levelId)}
                  />
                )),
              )}
            </CardContent>
          </Card>
        ) : (
          form.learningOutcomes.map((outcome) => {
            const gradedHere = outcome.criteria.filter((criterion) => levels[criterion.id] !== undefined).length
            return (
              <Card key={outcome.id}>
                <CardHeader>
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <div className="flex flex-col gap-1">
                      <CardTitle>{outcome.code}</CardTitle>
                      <CardDescription>
                        {outcome.description} · máx. {formatScore(outcome.maxScore)}
                      </CardDescription>
                    </div>
                    <Badge variant={gradedHere === outcome.criteria.length ? 'default' : 'outline'}>
                      {gradedHere} de {outcome.criteria.length}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent className="flex flex-col gap-6">
                  {outcome.criteria.map((criterion) => (
                    <LevelPicker
                      key={criterion.id}
                      title={criterion.description}
                      criterion={criterion}
                      selected={levels[criterion.id]}
                      disabled={disabled}
                      onPick={(levelId) => gradeGroup(criterion.id, levelId)}
                    />
                  ))}
                </CardContent>
              </Card>
            )
          })
        )}
      </div>

      <div className="flex flex-col gap-6 lg:sticky lg:top-6">
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between gap-2">
              <Badge variant={submitted ? 'default' : 'outline'}>
                {submitted ? 'Enviada' : form.status === 'DRAFT' ? 'Borrador' : 'Sin empezar'}
              </Badge>
              {form.evaluatedAt && (
                <span className="text-muted-foreground text-xs">Guardada {formatDateTime(form.evaluatedAt)}</span>
              )}
            </div>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            <div>
              <p className="text-3xl font-bold" aria-label="Nota del grupo">
                {formatScore(groupTotal)}
              </p>
              <p className="text-muted-foreground text-sm">
                de {formatScore(form.maxScore)} · {graded} de {criteria.length} criterios calificados
              </p>
            </div>
            <CommentField value={comment} disabled={disabled} onChange={setComment} />
            <FormError message={error} />
            {dirty && <p className="text-muted-foreground text-xs">Tienes cambios sin guardar.</p>}
            <div className="flex flex-col gap-2">
              <Button
                variant={submitted ? 'default' : 'outline'}
                disabled={disabled || !dirty}
                onClick={() => void save()}
              >
                {busy ? 'Guardando…' : submitted ? 'Guardar corrección' : 'Guardar borrador'}
              </Button>
              {!submitted && (
                <Button disabled={disabled || !complete} onClick={() => void submit()}>
                  Enviar evaluación
                </Button>
              )}
            </div>
            <p className="text-muted-foreground text-xs">
              {submitted
                ? 'Ya cuenta para la nota. Puedes corregirla mientras la calificación esté abierta; ' +
                  'debe seguir completa.'
                : complete
                  ? 'Al enviarla, empieza a contar para la nota del grupo.'
                  : `Para enviar, califica los ${criteria.length} criterios.`}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Integrantes</CardTitle>
            <CardDescription>Ajusta a un estudiante si se destacó o se quedó atrás.</CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="flex flex-col divide-y text-sm">
              {form.members.map((item) => {
                const count = Object.keys(adjustments[item.id] ?? {}).length
                return (
                  <li key={item.id} className="flex items-center justify-between gap-2 py-2">
                    <div className="flex flex-col">
                      <span className="font-medium">{item.fullName}</span>
                      <span className="text-muted-foreground text-xs">
                        {count === 0
                          ? 'Igual al grupo'
                          : `Ajustado en ${count} ${count === 1 ? 'criterio' : 'criterios'}`}
                        {' · '}
                        {formatScore(memberTotal(item.id))}
                      </span>
                    </div>
                    <Button
                      variant={adjusting === item.id ? 'default' : 'outline'}
                      size="sm"
                      aria-label={`Ajustar a ${item.fullName}`}
                      onClick={() => setAdjusting(item.id)}
                    >
                      Ajustar
                    </Button>
                  </li>
                )
              })}
              {form.members.length === 0 && (
                <li className="text-muted-foreground py-2">El grupo no tiene integrantes.</li>
              )}
            </ul>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

/** Versión que se califica y descarga de sus archivos. */
function SubmissionBanner({ form, history }: { form: EvaluationForm; history: SubmissionHistory | null }) {
  const [error, setError] = useState<string | null>(null)
  const ref = form.submissionToGrade
  if (!ref) {
    return <p className="bg-muted rounded-md px-3 py-2 text-sm">El grupo todavía no ha subido entregas.</p>
  }
  const version = history?.versions.find((item) => item.id === ref.id)
  const presentation = history?.currentPresentation ?? null
  const download = async (kind: 'zip' | 'presentation') => {
    setError(null)
    try {
      if (kind === 'zip' && version) await downloadSubmissionFile(version, 'zip')
      if (kind === 'presentation' && presentation) await downloadSubmissionFile(presentation, 'presentation')
    } catch (downloadError) {
      setError(getErrorMessage(downloadError))
    }
  }
  return (
    <Card className="bg-muted/40">
      <CardContent className="flex flex-wrap items-center gap-x-6 gap-y-2 text-sm">
        <span className="font-semibold">
          Se califica la v{ref.versionNumber}
          {version && ` (${formatDateTime(version.submittedAt)}, ${ref.late ? 'tardía' : 'a tiempo'})`}
        </span>
        {version && (
          <Button variant="link" className="h-auto p-0" onClick={() => void download('zip')}>
            <Download aria-hidden />
            Descargar la v{ref.versionNumber}
          </Button>
        )}
        {presentation && (
          <Button variant="link" className="h-auto p-0" onClick={() => void download('presentation')}>
            <Download aria-hidden />
            Descargar la presentación
          </Button>
        )}
        <FormError message={error} />
      </CardContent>
    </Card>
  )
}

/**
 * Niveles de un criterio como opciones. En el ajuste individual se marca con borde
 * punteado el nivel del grupo y con borde grueso el del estudiante.
 */
function LevelPicker({
  title,
  criterion,
  selected,
  groupLevel,
  adjusted = false,
  disabled,
  onPick,
}: {
  title: string
  criterion: CriterionForm
  selected: number | undefined
  /** Solo en el ajuste individual: el nivel del grupo. */
  groupLevel?: number
  adjusted?: boolean
  disabled: boolean
  onPick: (levelId: number) => void
}) {
  const titleId = useId()
  return (
    <div className="flex flex-col gap-2">
      <div className="flex items-center gap-2">
        <span id={titleId} className="text-sm font-medium">
          {title}
        </span>
        {adjusted && <Badge variant="secondary">Ajustado</Badge>}
      </div>
      <div role="radiogroup" aria-labelledby={titleId} className="grid gap-2 sm:grid-cols-2 xl:grid-cols-4">
        {criterion.levels.map((level) => {
          const checked = selected === level.id
          const isGroupLevel = groupLevel !== undefined && groupLevel === level.id
          return (
            <button
              key={level.id}
              type="button"
              role="radio"
              aria-checked={checked}
              aria-label={`${level.name} ${formatScore(level.score)}${isGroupLevel ? ' (nivel del grupo)' : ''}`}
              disabled={disabled}
              onClick={() => onPick(level.id)}
              className={cn(
                'flex flex-col gap-1 rounded-md border p-3 text-left text-sm transition-colors',
                'disabled:cursor-not-allowed disabled:opacity-70',
                checked ? 'border-primary bg-primary/5 border-2' : 'hover:bg-muted',
                isGroupLevel && !checked && 'border-muted-foreground border-dashed',
              )}
            >
              <span className="flex justify-between gap-2 font-medium">
                {level.name}
                <span>{formatScore(level.score)}</span>
              </span>
              {level.description && <span className="text-muted-foreground text-xs">{level.description}</span>}
              {isGroupLevel && <span className="text-muted-foreground text-xs">Nivel del grupo</span>}
            </button>
          )
        })}
      </div>
    </div>
  )
}

function CommentField({
  value,
  disabled,
  onChange,
}: {
  value: string
  disabled: boolean
  onChange: (value: string) => void
}) {
  const id = useId()
  return (
    <div className="flex flex-col gap-2">
      <label htmlFor={id} className="text-sm font-medium">
        Comentario para el grupo
      </label>
      <textarea
        id={id}
        rows={4}
        maxLength={2000}
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        className="border-input bg-background rounded-md border px-3 py-2 text-sm disabled:opacity-70"
      />
      <span className="text-muted-foreground text-xs">Los estudiantes lo ven al cierre, sin tu nombre.</span>
    </div>
  )
}

function ScoreFigure({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col">
      <span className="text-muted-foreground text-xs">{label}</span>
      <span className="text-lg font-semibold">{value}</span>
    </div>
  )
}

function signed(value: number): string {
  const rounded = Math.round(value * 100) / 100
  return `${rounded > 0 ? '+' : ''}${formatScore(rounded)}`
}

function initialLevels(form: EvaluationForm): Levels {
  return Object.fromEntries(
    form.learningOutcomes
      .flatMap((outcome) => outcome.criteria)
      .filter((criterion) => criterion.levelId !== null)
      .map((criterion) => [criterion.id, criterion.levelId as number]),
  )
}

function initialAdjustments(form: EvaluationForm): Adjustments {
  return Object.fromEntries(
    form.members
      .filter((member) => member.adjustments.length > 0)
      .map((member) => [
        member.id,
        Object.fromEntries(member.adjustments.map((adjustment) => [adjustment.criterionId, adjustment.levelId])),
      ]),
  )
}

function levelScores(criteria: CriterionForm[]): Record<number, number> {
  return Object.fromEntries(criteria.flatMap((criterion) => criterion.levels.map((level) => [level.id, level.score])))
}
