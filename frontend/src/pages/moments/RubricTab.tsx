import { useId, useState, type FormEvent, type ReactNode } from 'react'
import { ChevronDown, ChevronRight, Plus } from 'lucide-react'

import { FormError, FormField } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { formatScore } from '@/lib/scores'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import { rubricService } from '@/services/api/rubricService'
import type { EvaluationCriterion, LearningOutcome, MomentDetail, MomentLevel } from '@/types/moment'

import { parseScore } from './rubricValues'

/** Vuelve a leer el momento después de un cambio (la única fuente de su configuración). */
type Refresh = () => Promise<void>

/**
 * "RAP, criterios y niveles" de un momento (wireframe AD-03): el puntaje máximo, cada RAP
 * con sus criterios y cada criterio con sus niveles y puntajes. Nada es fijo en el código
 * (ADR-010): ni los nombres de los niveles, ni cuántos hay, ni los puntajes. Mientras el
 * momento está en configuración se agrega, edita y elimina; lanzado, solo se consulta.
 */
export function RubricTab({ moment, onChange }: { moment: MomentDetail; onChange: Refresh }) {
  const editable = moment.status === 'CONFIGURATION'
  const outcomes = moment.learningOutcomes
  const criteriaCount = outcomes.reduce((total, outcome) => total + outcome.criteria.length, 0)
  const [expanded, setExpanded] = useState<Set<number>>(
    () => new Set(outcomes.slice(0, 1).map((outcome) => outcome.id)),
  )
  const [adding, setAdding] = useState(false)

  const toggle = (id: number) =>
    setExpanded((current) => {
      const next = new Set(current)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-4xl font-bold" aria-label="Puntaje máximo del momento">
            {formatScore(moment.maxScore)}
          </p>
          <p className="text-muted-foreground text-sm">
            Puntaje máximo del momento · {plural(outcomes.length, 'RAP', 'RAP')} ·{' '}
            {plural(criteriaCount, 'criterio', 'criterios')}
          </p>
        </div>
        {editable && (
          <Button onClick={() => setAdding((open) => !open)}>
            <Plus aria-hidden />
            Agregar RAP
          </Button>
        )}
      </div>

      {adding && (
        <OutcomeForm
          title="Nuevo RAP"
          submitLabel="Agregar RAP"
          onCancel={() => setAdding(false)}
          onSave={async (request) => {
            await rubricService.createOutcome(moment.id, request)
            await onChange()
            setAdding(false)
          }}
        />
      )}

      {outcomes.length === 0 && (
        <p className="text-muted-foreground text-sm">
          El momento todavía no tiene RAP. Para lanzarlo necesita al menos uno, cada RAP con un criterio y cada
          criterio con sus niveles.
        </p>
      )}

      {outcomes.map((outcome) => (
        <OutcomeCard
          key={outcome.id}
          outcome={outcome}
          editable={editable}
          expanded={expanded.has(outcome.id)}
          onToggle={() => toggle(outcome.id)}
          onChange={onChange}
        />
      ))}
    </div>
  )
}

function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`
}

/** Ejecuta un cambio, vuelve a leer el momento y deja el mensaje de la API si falla. */
function useAction(onChange: Refresh) {
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const run = async (action: () => Promise<void>) => {
    setError(null)
    setBusy(true)
    try {
      await action()
      await onChange()
    } catch (actionError) {
      setError(getErrorMessage(actionError))
    } finally {
      setBusy(false)
    }
  }
  return { error, busy, run }
}

function OutcomeCard({
  outcome,
  editable,
  expanded,
  onToggle,
  onChange,
}: {
  outcome: LearningOutcome
  editable: boolean
  expanded: boolean
  onToggle: () => void
  onChange: Refresh
}) {
  const [editing, setEditing] = useState(false)
  const [addingCriterion, setAddingCriterion] = useState(false)
  const { error, busy, run } = useAction(onChange)
  const criteria = [...outcome.criteria].sort((a, b) => a.displayOrder - b.displayOrder)

  const remove = () => {
    if (!window.confirm(`¿Eliminar ${outcome.code} con sus criterios y niveles?`)) return
    void run(() => rubricService.deleteOutcome(outcome.id))
  }

  return (
    <Card className={expanded ? undefined : 'bg-muted/40'}>
      <CardHeader className="flex flex-row flex-wrap items-start justify-between gap-3">
        <button
          type="button"
          className="flex min-w-0 flex-1 items-start gap-2 text-left"
          aria-expanded={expanded}
          onClick={onToggle}
        >
          {expanded ? (
            <ChevronDown className="mt-1 size-4 shrink-0" aria-hidden />
          ) : (
            <ChevronRight className="mt-1 size-4 shrink-0" aria-hidden />
          )}
          <span className="min-w-0">
            <span className="font-semibold">{outcome.code}</span>
            <span className={`text-muted-foreground block text-sm ${expanded ? '' : 'truncate'}`}>
              {outcome.description}
              {!expanded && ` · ${plural(outcome.criteria.length, 'criterio', 'criterios')}`}
            </span>
          </span>
        </button>
        <div className="flex items-center gap-2">
          <Badge variant="secondary">Máx. {formatScore(outcome.maxScore)}</Badge>
          {editable && expanded && (
            <>
              <Button variant="ghost" size="sm" onClick={() => setEditing((open) => !open)}>
                Editar
              </Button>
              <Button
                variant="ghost"
                size="sm"
                disabled={busy}
                onClick={remove}
                aria-label={`Eliminar ${outcome.code}`}
              >
                Eliminar
              </Button>
            </>
          )}
        </div>
      </CardHeader>

      {expanded && (
        <CardContent className="flex flex-col gap-4">
          <FormError message={error} />
          {editing && (
            <OutcomeForm
              title={`Editar ${outcome.code}`}
              initial={{ code: outcome.code, description: outcome.description }}
              submitLabel="Guardar"
              onCancel={() => setEditing(false)}
              onSave={async (request) => {
                await rubricService.updateOutcome(outcome.id, request)
                await onChange()
                setEditing(false)
              }}
            />
          )}

          {criteria.length === 0 && (
            <p className="text-muted-foreground text-sm">Este RAP todavía no tiene criterios.</p>
          )}
          {criteria.map((criterion, index) => (
            <CriterionBlock
              key={criterion.id}
              criterion={criterion}
              position={index + 1}
              editable={editable}
              onChange={onChange}
            />
          ))}

          {editable &&
            (addingCriterion ? (
              <CriterionForm
                title={`Nuevo criterio de ${outcome.code}`}
                submitLabel="Agregar criterio"
                onCancel={() => setAddingCriterion(false)}
                onSave={async (description) => {
                  await rubricService.createCriterion(outcome.id, { description })
                  await onChange()
                  setAddingCriterion(false)
                }}
              />
            ) : (
              <Button variant="outline" size="sm" className="w-fit" onClick={() => setAddingCriterion(true)}>
                <Plus aria-hidden />
                Agregar criterio a {outcome.code}
              </Button>
            ))}
        </CardContent>
      )}
    </Card>
  )
}

function CriterionBlock({
  criterion,
  position,
  editable,
  onChange,
}: {
  criterion: EvaluationCriterion
  position: number
  editable: boolean
  onChange: Refresh
}) {
  const [editing, setEditing] = useState(false)
  const [addingLevel, setAddingLevel] = useState(false)
  const { error, busy, run } = useAction(onChange)
  const levels = [...criterion.levels].sort((a, b) => a.displayOrder - b.displayOrder)
  const title = `Criterio ${position} · ${criterion.description}`

  const remove = () => {
    if (!window.confirm(`¿Eliminar el criterio "${criterion.description}" con sus niveles?`)) return
    void run(() => rubricService.deleteCriterion(criterion.id))
  }

  return (
    <section aria-label={title} className="flex flex-col gap-3 border-t pt-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h3 className="font-semibold">{title}</h3>
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-muted-foreground text-xs">Máx. {formatScore(criterion.maxScore)}</span>
          {editable && (
            <>
              <Button variant="ghost" size="sm" onClick={() => setEditing((open) => !open)}>
                Editar criterio
              </Button>
              <Button variant="ghost" size="sm" disabled={busy} onClick={remove}>
                Quitar criterio
              </Button>
              <Button variant="outline" size="sm" onClick={() => setAddingLevel(true)}>
                Agregar nivel
              </Button>
            </>
          )}
        </div>
      </div>
      <FormError message={error} />

      {editing && (
        <CriterionForm
          title="Editar criterio"
          initial={criterion.description}
          submitLabel="Guardar"
          onCancel={() => setEditing(false)}
          onSave={async (description) => {
            await rubricService.updateCriterion(criterion.id, { description })
            await onChange()
            setEditing(false)
          }}
        />
      )}

      {levels.length === 0 ? (
        <p className="text-muted-foreground text-sm">Este criterio todavía no tiene niveles.</p>
      ) : (
        <ul className="flex flex-col divide-y">
          {levels.map((level) => (
            <LevelRow key={level.id} level={level} editable={editable} onChange={onChange} />
          ))}
        </ul>
      )}

      {addingLevel && (
        <LevelForm
          title="Nuevo nivel"
          submitLabel="Agregar nivel"
          onCancel={() => setAddingLevel(false)}
          onSave={async (request) => {
            await rubricService.createLevel(criterion.id, request)
            await onChange()
            setAddingLevel(false)
          }}
        />
      )}
    </section>
  )
}

function LevelRow({ level, editable, onChange }: { level: MomentLevel; editable: boolean; onChange: Refresh }) {
  const [editing, setEditing] = useState(false)
  const { error, busy, run } = useAction(onChange)

  if (editing) {
    return (
      <li className="py-3">
        <LevelForm
          title={`Editar nivel ${level.name}`}
          initial={level}
          submitLabel="Guardar"
          onCancel={() => setEditing(false)}
          onSave={async (request) => {
            await rubricService.updateLevel(level.id, request)
            await onChange()
            setEditing(false)
          }}
        />
      </li>
    )
  }

  const remove = () => {
    if (!window.confirm(`¿Quitar el nivel ${level.name}?`)) return
    void run(() => rubricService.deleteLevel(level.id))
  }

  return (
    <li className="flex flex-col gap-2 py-3 text-sm">
      <div className="grid items-center gap-2 sm:grid-cols-[10rem_1fr_auto_auto]">
        <span className="font-semibold">{level.name}</span>
        <span className="text-muted-foreground">{level.description}</span>
        <span className="font-medium tabular-nums" aria-label={`Puntaje de ${level.name}`}>
          {formatScore(level.score)}
        </span>
        {editable ? (
          <span className="flex gap-1">
            <Button variant="ghost" size="sm" onClick={() => setEditing(true)} aria-label={`Editar ${level.name}`}>
              Editar
            </Button>
            <Button variant="ghost" size="sm" disabled={busy} onClick={remove} aria-label={`Quitar ${level.name}`}>
              Quitar
            </Button>
          </span>
        ) : (
          <span />
        )}
      </div>
      <FormError message={error} />
    </li>
  )
}

// ------------------------------------------------------------------ formularios

/** Guarda un formulario y deja el mensaje y los errores por campo de la API si falla. */
function useFormSave() {
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const save = async (errors: Record<string, string>, action: () => Promise<void>) => {
    setError(null)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return
    setSaving(true)
    try {
      await action()
    } catch (saveError) {
      setFieldErrors(getFieldErrors(saveError))
      setError(getErrorMessage(saveError))
      setSaving(false)
    }
  }
  return { fieldErrors, error, saving, save }
}

function FormFrame({
  title,
  submitLabel,
  saving,
  error,
  onSubmit,
  onCancel,
  children,
}: {
  title: string
  submitLabel: string
  saving: boolean
  error: string | null
  onSubmit: () => void
  onCancel: () => void
  children: ReactNode
}) {
  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    onSubmit()
  }
  return (
    <form
      aria-label={title}
      className="bg-muted/40 flex flex-col gap-3 rounded-md border p-4"
      onSubmit={submit}
      noValidate
    >
      <p className="text-sm font-semibold">{title}</p>
      {children}
      <FormError message={error} />
      <div className="flex gap-3">
        <Button type="submit" size="sm" disabled={saving}>
          {saving ? 'Guardando…' : submitLabel}
        </Button>
        <Button type="button" size="sm" variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
      </div>
    </form>
  )
}

function TextAreaField({
  label,
  value,
  maxLength,
  error,
  onChange,
}: {
  label: string
  value: string
  maxLength: number
  error?: string
  onChange: (value: string) => void
}) {
  const id = useId()
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id}>{label}</Label>
      <textarea
        id={id}
        rows={3}
        maxLength={maxLength}
        value={value}
        aria-invalid={error ? true : undefined}
        onChange={(event) => onChange(event.target.value)}
        className="border-input bg-background rounded-md border px-3 py-2 text-sm"
      />
      {error && <p className="text-destructive text-sm">{error}</p>}
    </div>
  )
}

function OutcomeForm({
  title,
  initial = { code: '', description: '' },
  submitLabel,
  onSave,
  onCancel,
}: {
  title: string
  initial?: { code: string; description: string }
  submitLabel: string
  onSave: (request: { code: string; description: string }) => Promise<void>
  onCancel: () => void
}) {
  const [code, setCode] = useState(initial.code)
  const [description, setDescription] = useState(initial.description)
  const { fieldErrors, error, saving, save } = useFormSave()

  const submit = () => {
    const errors: Record<string, string> = {}
    if (!code.trim()) errors.code = 'El código es obligatorio'
    if (!description.trim()) errors.description = 'La descripción es obligatoria'
    void save(errors, () => onSave({ code: code.trim(), description: description.trim() }))
  }

  return (
    <FormFrame
      title={title}
      submitLabel={submitLabel}
      saving={saving}
      error={error}
      onSubmit={submit}
      onCancel={onCancel}
    >
      <div className="max-w-xs">
        <FormField
          label="Código"
          value={code}
          maxLength={20}
          error={fieldErrors.code}
          onChange={(event) => setCode(event.target.value)}
        />
      </div>
      <TextAreaField
        label="Descripción del RAP"
        value={description}
        maxLength={1000}
        error={fieldErrors.description}
        onChange={setDescription}
      />
    </FormFrame>
  )
}

function CriterionForm({
  title,
  initial = '',
  submitLabel,
  onSave,
  onCancel,
}: {
  title: string
  initial?: string
  submitLabel: string
  onSave: (description: string) => Promise<void>
  onCancel: () => void
}) {
  const [description, setDescription] = useState(initial)
  const { fieldErrors, error, saving, save } = useFormSave()

  const submit = () => {
    const errors: Record<string, string> = {}
    if (!description.trim()) errors.description = 'La descripción es obligatoria'
    void save(errors, () => onSave(description.trim()))
  }

  return (
    <FormFrame
      title={title}
      submitLabel={submitLabel}
      saving={saving}
      error={error}
      onSubmit={submit}
      onCancel={onCancel}
    >
      <TextAreaField
        label="Descripción del criterio"
        value={description}
        maxLength={500}
        error={fieldErrors.description}
        onChange={setDescription}
      />
    </FormFrame>
  )
}

function LevelForm({
  title,
  initial,
  submitLabel,
  onSave,
  onCancel,
}: {
  title: string
  initial?: MomentLevel
  submitLabel: string
  onSave: (request: { name: string; description: string | null; score: number }) => Promise<void>
  onCancel: () => void
}) {
  const [name, setName] = useState(initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [score, setScore] = useState(initial ? formatScore(initial.score) : '')
  const { fieldErrors, error, saving, save } = useFormSave()

  const submit = () => {
    const errors: Record<string, string> = {}
    if (!name.trim()) errors.name = 'El nombre es obligatorio'
    const parsed = parseScore(score)
    if (parsed === null) errors.score = 'Escribe un puntaje desde 0, con hasta 2 decimales (p. ej. 0,48)'
    void save(errors, () =>
      onSave({ name: name.trim(), description: description.trim() || null, score: parsed as number }),
    )
  }

  return (
    <FormFrame
      title={title}
      submitLabel={submitLabel}
      saving={saving}
      error={error}
      onSubmit={submit}
      onCancel={onCancel}
    >
      <div className="grid gap-3 sm:grid-cols-[1fr_10rem]">
        <FormField
          label="Nombre del nivel"
          value={name}
          maxLength={50}
          error={fieldErrors.name}
          onChange={(event) => setName(event.target.value)}
        />
        <FormField
          label="Puntaje"
          inputMode="decimal"
          value={score}
          error={fieldErrors.score}
          onChange={(event) => setScore(event.target.value)}
        />
      </div>
      <TextAreaField
        label="Descripción del desempeño (opcional)"
        value={description}
        maxLength={1000}
        error={fieldErrors.description}
        onChange={setDescription}
      />
    </FormFrame>
  )
}
