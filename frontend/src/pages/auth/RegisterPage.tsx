import { useId, useState, type ChangeEvent, type FormEvent } from 'react'
import { Link } from 'react-router'

import { FormError, FormField } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Label } from '@/components/ui/label'
import { authService } from '@/services/api/authService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import type { DocumentType } from '@/types/auth'

const DOCUMENT_TYPES: { value: DocumentType; label: string }[] = [
  { value: 'CC', label: 'Cédula de ciudadanía' },
  { value: 'TI', label: 'Tarjeta de identidad' },
  { value: 'CE', label: 'Cédula de extranjería' },
  { value: 'PASSPORT', label: 'Pasaporte' },
]

interface RegisterForm {
  registrationCode: string
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
  studentCode: string
  currentSemester: string
}

const EMPTY_FORM: RegisterForm = {
  registrationCode: '',
  firstNames: '',
  lastNames: '',
  documentType: 'CC',
  documentNumber: '',
  email: '',
  studentCode: '',
  currentSemester: '',
}

/**
 * Registro de un estudiante con el código que le dio su docente. La cuenta queda
 * creada sin contraseña y el enlace para crearla llega al correo institucional.
 */
export function RegisterPage() {
  const documentTypeId = useId()
  const [form, setForm] = useState<RegisterForm>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [registeredEmail, setRegisteredEmail] = useState<string | null>(null)

  const update =
    (field: keyof RegisterForm) => (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      setForm((previous) => ({ ...previous, [field]: event.target.value }))

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)
    const email = form.email.trim()
    try {
      await authService.register({
        registrationCode: form.registrationCode.trim(),
        firstNames: form.firstNames.trim(),
        lastNames: form.lastNames.trim(),
        documentType: form.documentType,
        documentNumber: form.documentNumber.trim(),
        email,
        studentCode: form.studentCode.trim(),
        currentSemester: Number(form.currentSemester),
      })
      setRegisteredEmail(email)
    } catch (registerError) {
      // 400 trae los errores por campo; 422 (código inválido, correo no institucional,
      // datos ya registrados) solo trae el mensaje general.
      setFieldErrors(getFieldErrors(registerError))
      setError(getErrorMessage(registerError))
    } finally {
      setSubmitting(false)
    }
  }

  if (registeredEmail) {
    return (
      <Card className="mx-auto w-full max-w-md">
        <CardHeader>
          <CardTitle>
            <h1>Revisa tu correo</h1>
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <p role="status" className="text-sm">
            Tu cuenta quedó creada. Enviamos a <strong>{registeredEmail}</strong> un enlace para crear
            tu contraseña; vence en 72 horas.
          </p>
          <Link to="/login" className="text-primary text-sm underline-offset-4 hover:underline">
            Ir a iniciar sesión
          </Link>
        </CardContent>
      </Card>
    )
  }

  return (
    <Card className="mx-auto w-full max-w-md">
      <CardHeader>
        <CardTitle>
          <h1>Registro de estudiante</h1>
        </CardTitle>
        <CardDescription>
          Usa el código de registro que te dio tu docente y tu correo institucional.
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
          <FormError message={error} />
          <FormField
            label="Código de registro"
            required
            autoCapitalize="characters"
            value={form.registrationCode}
            error={fieldErrors.registrationCode}
            onChange={update('registrationCode')}
          />
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField
              label="Nombres"
              required
              autoComplete="given-name"
              value={form.firstNames}
              error={fieldErrors.firstNames}
              onChange={update('firstNames')}
            />
            <FormField
              label="Apellidos"
              required
              autoComplete="family-name"
              value={form.lastNames}
              error={fieldErrors.lastNames}
              onChange={update('lastNames')}
            />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="flex flex-col gap-2">
              <Label htmlFor={documentTypeId}>Tipo de documento</Label>
              <select
                id={documentTypeId}
                className="border-input h-9 rounded-md border bg-transparent px-3 text-sm shadow-xs"
                value={form.documentType}
                onChange={update('documentType')}
              >
                {DOCUMENT_TYPES.map((type) => (
                  <option key={type.value} value={type.value}>
                    {type.label}
                  </option>
                ))}
              </select>
            </div>
            <FormField
              label="Número de documento"
              required
              value={form.documentNumber}
              error={fieldErrors.documentNumber}
              onChange={update('documentNumber')}
            />
          </div>
          <FormField
            label="Correo institucional"
            type="email"
            required
            autoComplete="email"
            placeholder="usuario@unicesar.edu.co"
            value={form.email}
            error={fieldErrors.email}
            onChange={update('email')}
          />
          <div className="grid gap-4 sm:grid-cols-2">
            <FormField
              label="Código estudiantil"
              required
              value={form.studentCode}
              error={fieldErrors.studentCode}
              onChange={update('studentCode')}
            />
            <FormField
              label="Semestre actual"
              type="number"
              min={1}
              required
              value={form.currentSemester}
              error={fieldErrors.currentSemester}
              onChange={update('currentSemester')}
            />
          </div>
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Registrando…' : 'Registrarme'}
          </Button>
          <Link to="/login" className="text-primary text-sm underline-offset-4 hover:underline">
            Ya tengo cuenta
          </Link>
        </form>
      </CardContent>
    </Card>
  )
}
