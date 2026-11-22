import { useState, type FormEvent } from 'react'

import { FormError, FormField } from '@/components/forms/FormField'
import { SelectField } from '@/components/forms/SelectField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { DOCUMENT_TYPES } from '@/lib/documentTypes'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import { userService } from '@/services/api/userService'
import type { UpdateUserRequest } from '@/types/account'
import type { DocumentType, UserSummary } from '@/types/auth'

interface EditForm {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
  studentCode: string
  currentSemester: string
}

/**
 * Formulario para editar los datos de una cuenta (`PATCH /api/users/{id}`): nombres,
 * apellidos, documento y correo; los estudiantes además editan su código y su semestre
 * (se muestran cuando se pasa `student`). Solo se envían los campos que cambiaron.
 *
 * Como el correo es el usuario de inicio de sesión, cambiarlo cierra las sesiones de la
 * cuenta: el formulario lo avisa antes de guardar.
 */
export function EditUserForm({
  user,
  student,
  onSaved,
  onCancel,
}: {
  user: UserSummary
  /** Datos académicos actuales; solo para estudiantes. */
  student?: { studentCode: string; currentSemester: number }
  /** Recibe la cuenta ya actualizada; la pantalla recarga su lista. */
  onSaved: (updated: UserSummary, emailChanged: boolean) => void
  onCancel: () => void
}) {
  const [form, setForm] = useState<EditForm>({
    firstNames: user.firstNames,
    lastNames: user.lastNames,
    documentType: user.documentType ?? 'CC',
    documentNumber: user.documentNumber ?? '',
    email: user.email,
    studentCode: student?.studentCode ?? '',
    currentSemester: student ? String(student.currentSemester) : '',
  })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const update =
    (field: keyof EditForm) => (event: { target: { value: string } }) =>
      setForm((current) => ({ ...current, [field]: event.target.value }))

  const emailChanged = form.email.trim().toLowerCase() !== user.email

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors = validate(form, Boolean(student))
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    const changes = changesOf(form, user, student)
    if (Object.keys(changes).length === 0) {
      setError('No hay cambios para guardar.')
      return
    }

    setSaving(true)
    try {
      onSaved(await userService.update(user.id, changes), emailChanged)
    } catch (saveError) {
      setFieldErrors(getFieldErrors(saveError))
      setError(getErrorMessage(saveError))
      setSaving(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Editar a {user.fullName}</CardTitle>
        <CardDescription>Solo se guardan los datos que cambies.</CardDescription>
      </CardHeader>
      <CardContent>
        <form aria-label={`Editar a ${user.fullName}`} className="flex flex-col gap-4" onSubmit={submit} noValidate>
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
            {student && (
              <>
                <FormField
                  label="Código estudiantil"
                  value={form.studentCode}
                  maxLength={30}
                  error={fieldErrors.studentCode}
                  onChange={update('studentCode')}
                />
                <FormField
                  label="Semestre"
                  type="number"
                  min={1}
                  step={1}
                  value={form.currentSemester}
                  error={fieldErrors.currentSemester}
                  onChange={update('currentSemester')}
                />
              </>
            )}
          </div>
          {emailChanged && (
            <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
              Al cambiar el correo se cerrarán las sesiones abiertas de esta cuenta y deberá iniciar sesión con el
              correo nuevo.
            </p>
          )}
          <FormError message={error} />
          <div className="flex gap-3">
            <Button type="submit" disabled={saving}>
              {saving ? 'Guardando…' : 'Guardar cambios'}
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
function validate(form: EditForm, isStudent: boolean): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!form.firstNames.trim()) errors.firstNames = 'Los nombres son obligatorios'
  if (!form.lastNames.trim()) errors.lastNames = 'Los apellidos son obligatorios'
  if (!form.documentNumber.trim()) errors.documentNumber = 'El número de documento es obligatorio'
  if (!form.email.trim()) errors.email = 'El correo es obligatorio'
  if (isStudent) {
    if (!form.studentCode.trim()) errors.studentCode = 'El código estudiantil es obligatorio'
    if (!form.currentSemester.trim()) errors.currentSemester = 'El semestre es obligatorio'
    else if (!/^\d+$/.test(form.currentSemester.trim()) || Number(form.currentSemester) < 1) {
      errors.currentSemester = 'El semestre debe ser 1 o mayor'
    }
  }
  return errors
}

/** Solo los campos que difieren de los datos actuales de la cuenta. */
function changesOf(
  form: EditForm,
  user: UserSummary,
  student: { studentCode: string; currentSemester: number } | undefined,
): UpdateUserRequest {
  const changes: UpdateUserRequest = {}
  if (form.firstNames.trim() !== user.firstNames) changes.firstNames = form.firstNames.trim()
  if (form.lastNames.trim() !== user.lastNames) changes.lastNames = form.lastNames.trim()
  if (form.documentType !== user.documentType) changes.documentType = form.documentType
  if (form.documentNumber.trim() !== (user.documentNumber ?? '')) changes.documentNumber = form.documentNumber.trim()
  if (form.email.trim().toLowerCase() !== user.email) changes.email = form.email.trim()
  if (student) {
    if (form.studentCode.trim() !== student.studentCode) changes.studentCode = form.studentCode.trim()
    if (Number(form.currentSemester) !== student.currentSemester) changes.currentSemester = Number(form.currentSemester)
  }
  return changes
}
