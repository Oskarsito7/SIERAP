import { useState, type FormEvent, type ReactNode } from 'react'

import { FormError, FormField } from '@/components/forms/FormField'
import { SelectField } from '@/components/forms/SelectField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatDate } from '@/lib/dates'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import type { AcademicPeriod, Program } from '@/types/catalog'

import { MOMENT_WINDOWS, validateMomentForm, type MomentFormField, type MomentFormValues } from './momentFormValues'

interface MomentFormProps {
  initial: MomentFormValues
  periods: AcademicPeriod[]
  /** Programas para elegir; solo al crear (el programa de un momento no cambia). */
  programs?: Program[]
  readOnly?: boolean
  submitLabel: string
  onSubmit: (values: MomentFormValues) => Promise<void>
  /** Acciones extra junto al botón de guardar (p. ej. cancelar o eliminar). */
  actions?: ReactNode
}

/**
 * Datos generales y ventanas de fechas de un momento (wireframe AD-02), para crearlo o
 * editarlo mientras sigue en configuración. El límite de la UPC sale del periodo elegido.
 */
export function MomentForm({
  initial,
  periods,
  programs,
  readOnly = false,
  submitLabel,
  onSubmit,
  actions,
}: MomentFormProps) {
  const [values, setValues] = useState(initial)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const period = periods.find((item) => String(item.id) === values.periodId)
  const upcDeadline = period?.upcEvaluationDeadline ?? null

  const field = (name: MomentFormField) => ({
    value: values[name],
    error: fieldErrors[name],
    disabled: readOnly,
    onChange: (event: { target: { value: string } }) =>
      setValues((current) => ({ ...current, [name]: event.target.value })),
  })

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors = validateMomentForm(values, { requireProgram: programs !== undefined, upcDeadline })
    setFieldErrors(errors as Record<string, string>)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    try {
      await onSubmit(values)
    } catch (submitError) {
      setFieldErrors(getFieldErrors(submitError))
      setError(getErrorMessage(submitError))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form className="flex flex-col gap-6" onSubmit={submit} noValidate>
      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>Datos generales</CardTitle>
          </CardHeader>
          <CardContent className="grid gap-4 sm:grid-cols-2">
            {programs && (
              <SelectField label="Programa" {...field('programId')}>
                <option value="">Elige un programa</option>
                {programs.map((program) => (
                  <option key={program.id} value={program.id}>
                    {program.name}
                  </option>
                ))}
              </SelectField>
            )}
            <div className={programs ? undefined : 'sm:col-span-2'}>
              <SelectField label="Periodo académico" {...field('periodId')}>
                <option value="">Elige un periodo</option>
                {periods.map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.code}
                  </option>
                ))}
              </SelectField>
            </div>
            <FormField label="Nombre" maxLength={100} {...field('name')} />
            <FormField label="Número" type="number" min={1} {...field('number')} />
            <div className="sm:col-span-2">
              <FormField label="Asignatura" maxLength={150} {...field('subject')} />
            </div>
            <FormField label="Desde el semestre" type="number" min={1} {...field('semesterFrom')} />
            <FormField label="Hasta el semestre" type="number" min={1} {...field('semesterTo')} />
            <FormField label="Mínimo de integrantes" type="number" min={1} {...field('minMembers')} />
            <FormField label="Máximo de integrantes" type="number" min={1} {...field('maxMembers')} />
            <div className="flex flex-col gap-1 sm:col-span-2">
              <FormField label="Nivel de exigencia" maxLength={100} {...field('requirementLevel')} />
              <p className="text-muted-foreground text-xs">Texto libre: cada programa usa sus propios nombres.</p>
            </div>
          </CardContent>
        </Card>

        <Card className="self-start">
          <CardHeader>
            <div className="flex flex-wrap items-baseline justify-between gap-2">
              <CardTitle>Ventanas de fechas</CardTitle>
              {upcDeadline && (
                <span className="text-muted-foreground text-xs">Límite UPC: {formatDate(upcDeadline)}</span>
              )}
            </div>
            <CardDescription>
              Puedes dejarlas vacías mientras configuras; para lanzar el momento deben estar las tres.
            </CardDescription>
          </CardHeader>
          <CardContent className="flex flex-col gap-4">
            {MOMENT_WINDOWS.map((window) => (
              <fieldset key={window.start} className="flex flex-col gap-2">
                <legend className="mb-2 text-sm font-semibold">{window.title}</legend>
                <div className="grid gap-4 sm:grid-cols-2">
                  <FormField label={`Inicio · ${window.title}`} type="date" {...field(window.start)} />
                  <FormField label={`Fin · ${window.title}`} type="date" {...field(window.end)} />
                </div>
              </fieldset>
            ))}
            {upcDeadline && (
              <p className="text-muted-foreground text-xs">
                La calificación debe terminar a más tardar el {formatDate(upcDeadline)} (límite de la UPC).
              </p>
            )}
          </CardContent>
        </Card>
      </div>

      <FormError message={error} />
      {!readOnly && (
        <div className="flex flex-wrap gap-3">
          <Button type="submit" disabled={saving}>
            {saving ? 'Guardando…' : submitLabel}
          </Button>
          {actions}
        </div>
      )}
    </form>
  )
}
