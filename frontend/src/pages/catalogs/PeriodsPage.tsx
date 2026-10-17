import { useState, type FormEvent } from 'react'
import { Plus } from 'lucide-react'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError, FormField } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { formatDate } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import type { AcademicPeriod, PeriodRequest, PeriodStatus } from '@/types/catalog'

const STATUS: Record<PeriodStatus, { label: string; variant: 'default' | 'secondary' | 'outline' }> = {
  CURRENT: { label: 'Actual', variant: 'default' },
  UPCOMING: { label: 'Próximo', variant: 'outline' },
  CLOSED: { label: 'Cerrado', variant: 'secondary' },
}

/**
 * Periodos académicos (wireframe SA-02, super_admin) con su límite de evaluación de la
 * UPC, que acota la ventana de calificación de todos sus momentos (ADR-012). El estado
 * (actual, próximo o cerrado) lo calcula el backend con la fecha de hoy. Un periodo con
 * momentos no se puede eliminar.
 */
export function PeriodsPage() {
  const periods = useLoad(() => catalogService.periods(), [])
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const sortNewestFirst = (list: AcademicPeriod[]) => [...list].sort((a, b) => b.startDate.localeCompare(a.startDate))

  const remove = async (period: AcademicPeriod) => {
    if (!window.confirm(`¿Eliminar el periodo ${period.code}?`)) return
    setError(null)
    try {
      await catalogService.deletePeriod(period.id)
      periods.setData((current) => current.filter((item) => item.id !== period.id))
    } catch (deleteError) {
      setError(`No se puede eliminar ${period.code}: ${getErrorMessage(deleteError)}`)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">Periodos académicos</h1>
          <p className="text-muted-foreground text-sm">El periodo actual es el que incluye la fecha de hoy.</p>
        </div>
        <Button onClick={() => setCreating((open) => !open)}>
          <Plus aria-hidden />
          Nuevo periodo
        </Button>
      </div>

      {creating && (
        <PeriodForm
          title="Nuevo periodo"
          submitLabel="Crear periodo"
          onCancel={() => setCreating(false)}
          onSave={async (request) => {
            const created = await catalogService.createPeriod(request)
            periods.setData((current) => sortNewestFirst([...current, created]))
            setCreating(false)
          }}
        />
      )}
      {error && (
        <p role="alert" className="bg-muted rounded-md border px-4 py-3 text-sm">
          {error}
        </p>
      )}

      <Card>
        <CardContent className="flex flex-col gap-4">
          <LoadStatus state={periods.state} onRetry={periods.reload} label="Cargando los periodos…" />
          {periods.state.kind === 'success' && periods.state.data.length === 0 && (
            <p className="text-muted-foreground text-sm">Todavía no hay periodos académicos.</p>
          )}
          {periods.state.kind === 'success' && periods.state.data.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground border-b">
                  <tr>
                    <th className="py-2 pr-4 font-medium">Periodo</th>
                    <th className="py-2 pr-4 font-medium">Inicio</th>
                    <th className="py-2 pr-4 font-medium">Fin</th>
                    <th className="py-2 pr-4 font-medium">Límite de evaluación UPC</th>
                    <th className="py-2 pr-4 font-medium">Estado</th>
                    <th className="py-2 font-medium">
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {periods.state.data.map((period) =>
                    editing === period.id ? (
                      <tr key={period.id} className="border-b">
                        <td colSpan={6} className="py-3">
                          <PeriodForm
                            title={`Editar ${period.code}`}
                            initial={period}
                            submitLabel="Guardar"
                            onCancel={() => setEditing(null)}
                            onSave={async (request) => {
                              const updated = await catalogService.updatePeriod(period.id, request)
                              periods.setData((current) =>
                                sortNewestFirst(current.map((item) => (item.id === updated.id ? updated : item))),
                              )
                              setEditing(null)
                            }}
                          />
                        </td>
                      </tr>
                    ) : (
                      <tr key={period.id} className="border-b last:border-0">
                        <td className="py-3 pr-4 font-semibold">{period.code}</td>
                        <td className="py-3 pr-4">{formatDate(period.startDate)}</td>
                        <td className="py-3 pr-4">{formatDate(period.endDate)}</td>
                        <td className="py-3 pr-4">{formatDate(period.upcEvaluationDeadline)}</td>
                        <td className="py-3 pr-4">
                          <Badge variant={STATUS[period.status].variant}>{STATUS[period.status].label}</Badge>
                        </td>
                        <td className="py-3 text-right whitespace-nowrap">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setEditing(period.id)}
                            aria-label={`Editar ${period.code}`}
                          >
                            Editar
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => void remove(period)}
                            aria-label={`Eliminar ${period.code}`}
                          >
                            Eliminar
                          </Button>
                        </td>
                      </tr>
                    ),
                  )}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

/**
 * Datos de un periodo. Validación del cliente con las reglas de `docs/07`: el fin no es
 * anterior al inicio y el límite de la UPC está entre los dos. El backend vuelve a
 * validar, también que el límite no quede antes del fin de calificación de un momento.
 */
function PeriodForm({
  title,
  initial,
  submitLabel,
  onSave,
  onCancel,
}: {
  title: string
  initial?: PeriodRequest
  submitLabel: string
  onSave: (request: PeriodRequest) => Promise<void>
  onCancel: () => void
}) {
  const [values, setValues] = useState<PeriodRequest>(
    initial ?? { code: '', startDate: '', endDate: '', upcEvaluationDeadline: '' },
  )
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const field = (name: keyof PeriodRequest) => ({
    value: values[name],
    error: fieldErrors[name],
    onChange: (event: { target: { value: string } }) =>
      setValues((current) => ({ ...current, [name]: event.target.value })),
  })

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors: Record<string, string> = {}
    if (!values.code.trim()) errors.code = 'El código es obligatorio'
    if (!values.startDate) errors.startDate = 'La fecha de inicio es obligatoria'
    if (!values.endDate) errors.endDate = 'La fecha de fin es obligatoria'
    if (!values.upcEvaluationDeadline) errors.upcEvaluationDeadline = 'El límite de evaluación es obligatorio'
    if (values.startDate && values.endDate && values.endDate < values.startDate) {
      errors.endDate = 'Debe ser igual o posterior al inicio'
    }
    const { startDate, endDate, upcEvaluationDeadline: deadline } = values
    if (!errors.endDate && deadline && startDate && endDate && (deadline < startDate || deadline > endDate)) {
      errors.upcEvaluationDeadline = 'El límite de evaluación de la UPC debe estar dentro del periodo'
    }
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    try {
      await onSave({ ...values, code: values.code.trim() })
    } catch (saveError) {
      setFieldErrors(getFieldErrors(saveError))
      setError(getErrorMessage(saveError))
      setSaving(false)
    }
  }

  return (
    <form
      aria-label={title}
      className="bg-muted/40 flex flex-col gap-3 rounded-md border p-4"
      onSubmit={submit}
      noValidate
    >
      <p className="text-sm font-semibold">{title}</p>
      <div className="grid gap-3 sm:grid-cols-4">
        <FormField label="Código" maxLength={20} placeholder="2027-1" {...field('code')} />
        <FormField label="Inicio" type="date" {...field('startDate')} />
        <FormField label="Fin" type="date" {...field('endDate')} />
        <FormField label="Límite de evaluación de la UPC" type="date" {...field('upcEvaluationDeadline')} />
      </div>
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
