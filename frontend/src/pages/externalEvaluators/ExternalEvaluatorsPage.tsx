import { useState, type FormEvent } from 'react'
import { UserPlus } from 'lucide-react'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError, FormField } from '@/components/forms/FormField'
import { SelectField } from '@/components/forms/SelectField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { DOCUMENT_TYPES } from '@/lib/documentTypes'
import { useLoad } from '@/lib/useLoad'
import { accountService } from '@/services/api/accountService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import type { NewExternalEvaluatorRequest } from '@/types/account'
import type { DocumentType, UserSummary } from '@/types/auth'

/**
 * Evaluadores externos que creó el docente (wireframe DO-09): la lista, el reenvío del
 * enlace para crear la contraseña y el formulario para crear uno nuevo. Antes de crear
 * uno conviene buscarlo desde el grupo (DO-03): si otro docente ya lo creó, se reutiliza.
 */
export function ExternalEvaluatorsPage() {
  const evaluators = useLoad(() => accountService.listExternalEvaluators(), [])
  const [search, setSearch] = useState('')
  const [creating, setCreating] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const list = evaluators.state.kind === 'success' ? evaluators.state.data : []
  const term = search.trim().toLowerCase()
  const visible = term
    ? list.filter((evaluator) => `${evaluator.fullName} ${evaluator.email}`.toLowerCase().includes(term))
    : list

  const resendLink = async (evaluator: UserSummary) => {
    setNotice(null)
    setError(null)
    try {
      await accountService.resendPasswordLink(evaluator.id)
      setNotice(`Se envió de nuevo el enlace a ${evaluator.email}.`)
    } catch (resendError) {
      setError(getErrorMessage(resendError))
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">Evaluadores externos</h1>
          <p className="text-muted-foreground max-w-2xl text-sm">
            Los evaluadores que creaste. Antes de crear uno, búscalo desde el grupo por su correo o documento: si
            otro docente ya lo creó, se reutiliza su cuenta.
          </p>
        </div>
        <Button onClick={() => setCreating((open) => !open)}>
          <UserPlus aria-hidden />
          Nuevo evaluador
        </Button>
      </div>

      {creating && (
        <NewEvaluatorForm
          onCancel={() => setCreating(false)}
          onCreated={(evaluator) => {
            setCreating(false)
            setError(null)
            setNotice(`Se creó a ${evaluator.fullName}. Recibirá un correo para crear su contraseña.`)
            evaluators.setData((current) =>
              [...current, evaluator].sort((a, b) => a.lastNames.localeCompare(b.lastNames, 'es')),
            )
          }}
        />
      )}

      {notice && (
        <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
          {notice}
        </p>
      )}
      <FormError message={error} />

      <Card>
        <CardContent className="flex flex-col gap-4">
          <div className="max-w-sm">
            <FormField
              label="Buscar por nombre o correo"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <LoadStatus state={evaluators.state} onRetry={evaluators.reload} label="Cargando los evaluadores…" />
          {evaluators.state.kind === 'success' && visible.length === 0 && (
            <p className="text-muted-foreground text-sm">
              {list.length === 0 ? 'Todavía no has creado evaluadores externos.' : 'Ningún evaluador coincide.'}
            </p>
          )}
          {visible.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground border-b">
                  <tr>
                    <th className="py-2 pr-4 font-medium">Nombre</th>
                    <th className="py-2 pr-4 font-medium">Correo</th>
                    <th className="py-2 pr-4 font-medium">Estado</th>
                    <th className="py-2 font-medium">
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((evaluator) => (
                    <tr key={evaluator.id} className="border-b last:border-0">
                      <td className="py-3 pr-4">{evaluator.fullName}</td>
                      <td className="py-3 pr-4 break-all">{evaluator.email}</td>
                      <td className="py-3 pr-4">
                        <Badge variant={evaluator.active ? 'default' : 'secondary'}>
                          {evaluator.active ? 'Activo' : 'Desactivado'}
                        </Badge>
                      </td>
                      <td className="py-3 text-right">
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={!evaluator.active}
                          onClick={() => void resendLink(evaluator)}
                          aria-label={`Reenviar enlace a ${evaluator.fullName}`}
                        >
                          Reenviar enlace
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
    </div>
  )
}

interface EvaluatorForm {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
  evaluatorTypeId: string
  organization: string
  position: string
  graduationYear: string
}

const EMPTY_FORM: EvaluatorForm = {
  firstNames: '',
  lastNames: '',
  documentType: 'CC',
  documentNumber: '',
  email: '',
  evaluatorTypeId: '',
  organization: '',
  position: '',
  graduationYear: '',
}

/** Formulario de un evaluador nuevo; los datos de organización y grado son opcionales. */
function NewEvaluatorForm({
  onCreated,
  onCancel,
}: {
  onCreated: (evaluator: UserSummary) => void
  onCancel: () => void
}) {
  const types = useLoad(() => accountService.listEvaluatorTypes(), [])
  const [form, setForm] = useState<EvaluatorForm>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const activeTypes = types.state.kind === 'success' ? types.state.data.filter((type) => type.active) : []

  const update =
    (field: keyof EvaluatorForm) => (event: { target: { value: string } }) =>
      setForm((current) => ({ ...current, [field]: event.target.value }))

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    try {
      onCreated(await accountService.createExternalEvaluator(toRequest(form)))
    } catch (createError) {
      setFieldErrors(getFieldErrors(createError))
      setError(getErrorMessage(createError))
      setSaving(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Nuevo evaluador externo</CardTitle>
        <CardDescription>Recibirá un correo para crear su contraseña. El correo puede ser personal.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
          <div className="grid gap-4 md:grid-cols-2">
            <FormField
              label="Nombres"
              value={form.firstNames}
              maxLength={100}
              error={fieldErrors.firstNames}
              onChange={update('firstNames')}
            />
            <FormField
              label="Apellidos"
              value={form.lastNames}
              maxLength={100}
              error={fieldErrors.lastNames}
              onChange={update('lastNames')}
            />
            <SelectField
              label="Tipo de documento"
              value={form.documentType}
              error={fieldErrors.documentType}
              onChange={update('documentType')}
            >
              {DOCUMENT_TYPES.map((type) => (
                <option key={type.value} value={type.value}>
                  {type.label}
                </option>
              ))}
            </SelectField>
            <FormField
              label="Número de documento"
              value={form.documentNumber}
              maxLength={30}
              error={fieldErrors.documentNumber}
              onChange={update('documentNumber')}
            />
            <FormField
              label="Correo"
              type="email"
              value={form.email}
              maxLength={150}
              error={fieldErrors.email}
              onChange={update('email')}
            />
            <SelectField
              label="Tipo de evaluador"
              value={form.evaluatorTypeId}
              error={fieldErrors.evaluatorTypeId}
              onChange={update('evaluatorTypeId')}
            >
              <option value="">{types.state.kind === 'loading' ? 'Cargando…' : 'Elige un tipo'}</option>
              {activeTypes.map((type) => (
                <option key={type.id} value={type.id}>
                  {type.name}
                </option>
              ))}
            </SelectField>
            <FormField
              label="Organización (opcional)"
              value={form.organization}
              maxLength={150}
              error={fieldErrors.organization}
              onChange={update('organization')}
            />
            <FormField
              label="Cargo (opcional)"
              value={form.position}
              maxLength={100}
              error={fieldErrors.position}
              onChange={update('position')}
            />
            <FormField
              label="Año de grado (opcional)"
              type="number"
              inputMode="numeric"
              value={form.graduationYear}
              error={fieldErrors.graduationYear}
              onChange={update('graduationYear')}
            />
          </div>
          {types.state.kind === 'error' && <FormError message={types.state.message} />}
          <FormError message={error} />
          <div className="flex gap-3">
            <Button type="submit" disabled={saving}>
              {saving ? 'Creando…' : 'Crear y enviar enlace'}
            </Button>
            <Button type="button" variant="outline" onClick={onCancel}>
              Cancelar
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}

/** Validación del cliente; el backend vuelve a validar y responde 400 por campo. */
function validate(form: EvaluatorForm): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!form.firstNames.trim()) errors.firstNames = 'Los nombres son obligatorios'
  if (!form.lastNames.trim()) errors.lastNames = 'Los apellidos son obligatorios'
  if (!form.documentNumber.trim()) errors.documentNumber = 'El número de documento es obligatorio'
  if (!form.email.trim()) errors.email = 'El correo es obligatorio'
  if (!form.evaluatorTypeId) errors.evaluatorTypeId = 'El tipo de evaluador es obligatorio'
  const year = form.graduationYear.trim()
  if (year && !/^\d{4}$/.test(year)) errors.graduationYear = 'Escribe el año con cuatro cifras'
  return errors
}

function toRequest(form: EvaluatorForm): NewExternalEvaluatorRequest {
  const optional = (value: string) => value.trim() || undefined
  return {
    firstNames: form.firstNames.trim(),
    lastNames: form.lastNames.trim(),
    documentType: form.documentType,
    documentNumber: form.documentNumber.trim(),
    email: form.email.trim(),
    evaluatorTypeId: Number(form.evaluatorTypeId),
    organization: optional(form.organization),
    position: optional(form.position),
    graduationYear: form.graduationYear.trim() ? Number(form.graduationYear) : undefined,
  }
}
