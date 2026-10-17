import { useState, type FormEvent } from 'react'
import { Plus } from 'lucide-react'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError, FormField } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import type { GlobalParameter } from '@/types/catalog'

/**
 * Parámetros globales (wireframe SA-03, super_admin). Guardar con una clave nueva crea
 * el parámetro; con una existente cambia su valor y su descripción. Por ahora ninguna
 * regla del sistema los lee (`docs/07`): cada parámetro que una regla empiece a usar
 * llega con su propio contrato.
 */
export function ParametersPage() {
  const parameters = useLoad(() => catalogService.parameters(), [])
  const [creating, setCreating] = useState(false)
  const [editing, setEditing] = useState<string | null>(null)

  const upsert = (saved: GlobalParameter) =>
    parameters.setData((current) =>
      [...current.filter((item) => item.key !== saved.key), saved].sort((a, b) => a.key.localeCompare(b.key)),
    )

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">Parámetros globales</h1>
          <p className="text-muted-foreground max-w-2xl text-sm">
            Valores generales del sistema que no dependen de una facultad ni de un periodo.
          </p>
        </div>
        <Button onClick={() => setCreating((open) => !open)}>
          <Plus aria-hidden />
          Nuevo parámetro
        </Button>
      </div>

      {creating && (
        <ParameterForm
          title="Nuevo parámetro"
          submitLabel="Crear parámetro"
          onCancel={() => setCreating(false)}
          onSave={async (key, value, description) => {
            upsert(await catalogService.saveParameter(key, value, description))
            setCreating(false)
          }}
        />
      )}

      <Card>
        <CardContent className="flex flex-col gap-4">
          <LoadStatus state={parameters.state} onRetry={parameters.reload} label="Cargando los parámetros…" />
          {parameters.state.kind === 'success' && parameters.state.data.length === 0 && (
            <p className="text-muted-foreground text-sm">
              Todavía no hay parámetros globales: por ahora ninguna regla del sistema los usa.
            </p>
          )}
          {parameters.state.kind === 'success' && parameters.state.data.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground border-b">
                  <tr>
                    <th className="py-2 pr-4 font-medium">Parámetro</th>
                    <th className="py-2 pr-4 font-medium">Valor</th>
                    <th className="py-2 pr-4 font-medium">Descripción</th>
                    <th className="py-2 font-medium">
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {parameters.state.data.map((parameter) =>
                    editing === parameter.key ? (
                      <tr key={parameter.key} className="border-b">
                        <td colSpan={4} className="py-3">
                          <ParameterForm
                            title={`Editar ${parameter.key}`}
                            initial={parameter}
                            submitLabel="Guardar"
                            onCancel={() => setEditing(null)}
                            onSave={async (key, value, description) => {
                              upsert(await catalogService.saveParameter(key, value, description))
                              setEditing(null)
                            }}
                          />
                        </td>
                      </tr>
                    ) : (
                      <tr key={parameter.key} className="border-b last:border-0">
                        <td className="py-3 pr-4 font-semibold">{parameter.key}</td>
                        <td className="py-3 pr-4 break-all">{parameter.value}</td>
                        <td className="text-muted-foreground py-3 pr-4">{parameter.description}</td>
                        <td className="py-3 text-right">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setEditing(parameter.key)}
                            aria-label={`Editar ${parameter.key}`}
                          >
                            Editar
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

/** Clave, valor y descripción de un parámetro; la clave solo se escribe al crearlo. */
function ParameterForm({
  title,
  initial,
  submitLabel,
  onSave,
  onCancel,
}: {
  title: string
  initial?: GlobalParameter
  submitLabel: string
  onSave: (key: string, value: string, description: string | null) => Promise<void>
  onCancel: () => void
}) {
  const [key, setKey] = useState(initial?.key ?? '')
  const [value, setValue] = useState(initial?.value ?? '')
  const [description, setDescription] = useState(initial?.description ?? '')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors: Record<string, string> = {}
    if (!key.trim()) errors.key = 'La clave es obligatoria'
    if (!value.trim()) errors.value = 'El valor es obligatorio'
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    try {
      await onSave(key.trim(), value.trim(), description.trim() || null)
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
      <div className="grid gap-3 sm:grid-cols-3">
        <FormField
          label="Clave"
          value={key}
          disabled={initial !== undefined}
          error={fieldErrors.key}
          onChange={(event) => setKey(event.target.value)}
        />
        <FormField
          label="Valor"
          value={value}
          maxLength={500}
          error={fieldErrors.value}
          onChange={(event) => setValue(event.target.value)}
        />
        <FormField
          label="Descripción (opcional)"
          value={description}
          maxLength={500}
          error={fieldErrors.description}
          onChange={(event) => setDescription(event.target.value)}
        />
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
