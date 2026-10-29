import { useId, useState, type FormEvent } from 'react'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError, FormField } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { useLoad } from '@/lib/useLoad'
import { accountService } from '@/services/api/accountService'
import { catalogService } from '@/services/api/catalogService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import type { EvaluatorType } from '@/types/account'
import type { EvaluatorTypeRequest } from '@/types/catalog'

/**
 * Catálogo de tipos de evaluador externo (wireframe SA-04, super_admin, ADR-011). Un tipo
 * desactivado deja de ofrecerse para evaluadores nuevos, sin tocar a sus evaluadores ni
 * sus pesos en los momentos. Solo se elimina si ningún evaluador ni momento lo usa.
 */
export function EvaluatorTypesPage() {
  const types = useLoad(() => accountService.listEvaluatorTypes(), [])
  const [editing, setEditing] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  // Cambia con cada tipo creado para dejar el formulario "Nuevo tipo" vacío otra vez.
  const [created, setCreated] = useState(0)

  const replace = (updated: EvaluatorType) =>
    types.setData((current) => current.map((item) => (item.id === updated.id ? updated : item)))

  const toggle = async (type: EvaluatorType) => {
    setError(null)
    try {
      replace(
        await catalogService.updateEvaluatorType(type.id, {
          name: type.name,
          description: type.description,
          active: !type.active,
        }),
      )
    } catch (toggleError) {
      setError(getErrorMessage(toggleError))
    }
  }

  const remove = async (type: EvaluatorType) => {
    if (!window.confirm(`¿Eliminar el tipo ${type.name}?`)) return
    setError(null)
    try {
      await catalogService.deleteEvaluatorType(type.id)
      types.setData((current) => current.filter((item) => item.id !== type.id))
    } catch (deleteError) {
      setError(`No se puede eliminar ${type.name}: ${getErrorMessage(deleteError)}`)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">Tipos de evaluador</h1>
        <p className="text-muted-foreground max-w-2xl text-sm">
          Los tipos de evaluador externo que los docentes pueden asignar y que el admin puede ponderar en cada momento.
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-[minmax(0,2fr)_minmax(0,1fr)]">
        <Card className="self-start">
          <CardContent className="flex flex-col gap-4">
            {error && (
              <p role="alert" className="bg-muted rounded-md border px-4 py-3 text-sm">
                {error}
              </p>
            )}
            <LoadStatus state={types.state} onRetry={types.reload} label="Cargando los tipos…" />
            {types.state.kind === 'success' && types.state.data.length === 0 && (
              <p className="text-muted-foreground text-sm">Todavía no hay tipos de evaluador.</p>
            )}
            {types.state.kind === 'success' && types.state.data.length > 0 && (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-muted-foreground border-b">
                    <tr>
                      <th className="py-2 pr-4 font-medium">Tipo</th>
                      <th className="py-2 pr-4 font-medium">Descripción</th>
                      <th className="py-2 pr-4 text-right font-medium">Evaluadores</th>
                      <th className="py-2 pr-4 font-medium">Activo</th>
                      <th className="py-2 font-medium">
                        <span className="sr-only">Acciones</span>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {types.state.data.map((type) =>
                      editing === type.id ? (
                        <tr key={type.id} className="border-b">
                          <td colSpan={5} className="py-3">
                            <TypeForm
                              title={`Editar ${type.name}`}
                              initial={type}
                              submitLabel="Guardar"
                              onCancel={() => setEditing(null)}
                              onSave={async (request) => {
                                const changes = { ...request, active: type.active }
                                replace(await catalogService.updateEvaluatorType(type.id, changes))
                                setEditing(null)
                              }}
                            />
                          </td>
                        </tr>
                      ) : (
                        <tr key={type.id} className="border-b last:border-0">
                          <td className="py-3 pr-4 font-semibold">{type.name}</td>
                          <td className="text-muted-foreground py-3 pr-4">{type.description}</td>
                          <td className="py-3 pr-4 text-right">{type.evaluatorCount}</td>
                          <td className="py-3 pr-4">
                            <input
                              type="checkbox"
                              role="switch"
                              aria-label={`${type.name} activo`}
                              checked={type.active}
                              onChange={() => void toggle(type)}
                              className="accent-primary size-4"
                            />
                          </td>
                          <td className="py-3 text-right whitespace-nowrap">
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setEditing(type.id)}
                              aria-label={`Editar ${type.name}`}
                            >
                              Editar
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => void remove(type)}
                              aria-label={`Eliminar ${type.name}`}
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

        <Card className="self-start">
          <CardHeader>
            <CardTitle>Nuevo tipo</CardTitle>
          </CardHeader>
          <CardContent>
            <TypeForm
              key={created}
              submitLabel="Agregar tipo"
              onSave={async (request) => {
                const type = await catalogService.createEvaluatorType(request)
                types.setData((current) => [...current, type].sort((a, b) => a.name.localeCompare(b.name, 'es')))
                setCreated((count) => count + 1)
              }}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}

/** Nombre y descripción de un tipo de evaluador. */
function TypeForm({
  title,
  initial,
  submitLabel,
  onSave,
  onCancel,
}: {
  title?: string
  initial?: EvaluatorType
  submitLabel: string
  onSave: (request: EvaluatorTypeRequest) => Promise<void>
  onCancel?: () => void
}) {
  const descriptionId = useId()
  const [name, setName] = useState(initial?.name ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors: Record<string, string> = {}
    if (!name.trim()) errors.name = 'El nombre es obligatorio'
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    try {
      await onSave({ name: name.trim(), description: description.trim() || null })
    } catch (saveError) {
      setFieldErrors(getFieldErrors(saveError))
      setError(getErrorMessage(saveError))
    } finally {
      setSaving(false)
    }
  }

  return (
    <form
      aria-label={title ?? 'Nuevo tipo'}
      className={`flex flex-col gap-3 ${title ? 'bg-muted/40 rounded-md border p-4' : ''}`}
      onSubmit={submit}
      noValidate
    >
      {title && <p className="text-sm font-semibold">{title}</p>}
      <FormField
        label="Nombre"
        value={name}
        maxLength={100}
        placeholder="Ej. Docente de otra universidad"
        error={fieldErrors.name}
        onChange={(event) => setName(event.target.value)}
      />
      <div className="flex flex-col gap-2">
        <Label htmlFor={descriptionId}>Descripción</Label>
        <textarea
          id={descriptionId}
          rows={3}
          maxLength={500}
          placeholder="Quiénes son y por qué evalúan."
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          className="border-input bg-background rounded-md border px-3 py-2 text-sm"
        />
      </div>
      <FormError message={error} />
      <div className="flex gap-3">
        <Button type="submit" size="sm" disabled={saving}>
          {saving ? 'Guardando…' : submitLabel}
        </Button>
        {onCancel && (
          <Button type="button" size="sm" variant="outline" onClick={onCancel}>
            Cancelar
          </Button>
        )}
      </div>
    </form>
  )
}
