import { useState, type FormEvent } from 'react'
import { UserPlus } from 'lucide-react'

import { LoadStatus } from '@/components/LoadStatus'
import { SampleDataNotice } from '@/components/SampleDataNotice'
import { FormError, FormField } from '@/components/forms/FormField'
import { SelectField } from '@/components/forms/SelectField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { DOCUMENT_TYPES } from '@/lib/documentTypes'
import { useLoad } from '@/lib/useLoad'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import { TEACHERS_API_READY, teacherService } from '@/services/api/teacherService'
import type { NewTeacherRequest, TeacherSummary } from '@/types/account'
import type { DocumentType } from '@/types/auth'

/**
 * Docentes de la facultad del admin (wireframe AD-06): la lista con su documento y sus
 * grupos del periodo, el reenvío del enlace para crear la contraseña, la desactivación
 * de la cuenta y el formulario para crear uno nuevo (ADR-017).
 */
export function TeachersPage() {
  const teachers = useLoad(() => teacherService.list(), [])
  const [search, setSearch] = useState('')
  const [creating, setCreating] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const list = teachers.state.kind === 'success' ? teachers.state.data : []
  const term = search.trim().toLowerCase()
  const visible = term
    ? list.filter((teacher) =>
        `${teacher.fullName} ${teacher.documentNumber} ${teacher.email}`.toLowerCase().includes(term),
      )
    : list

  const replace = (updated: TeacherSummary) =>
    teachers.setData((current) => current.map((teacher) => (teacher.id === updated.id ? updated : teacher)))

  const resendLink = async (teacher: TeacherSummary) => {
    setNotice(null)
    setError(null)
    try {
      await teacherService.resendPasswordLink(teacher.id)
      setNotice(`Se envió de nuevo el enlace a ${teacher.email}.`)
    } catch (resendError) {
      setError(getErrorMessage(resendError))
    }
  }

  const toggleActive = async (teacher: TeacherSummary) => {
    const question = `¿Desactivar la cuenta de ${teacher.fullName}? No podrá iniciar sesión.`
    if (teacher.active && !window.confirm(question)) return
    setNotice(null)
    setError(null)
    try {
      const updated = await teacherService.setActive(teacher.id, !teacher.active)
      replace({ ...teacher, active: updated.active })
      setNotice(`Se ${updated.active ? 'reactivó' : 'desactivó'} la cuenta de ${teacher.fullName}.`)
    } catch (toggleError) {
      setError(getErrorMessage(toggleError))
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">Docentes</h1>
          <p className="text-muted-foreground max-w-2xl text-sm">
            Los docentes de tu facultad. Cada docente nuevo recibe un correo para crear su contraseña.
          </p>
        </div>
        <Button onClick={() => setCreating((open) => !open)}>
          <UserPlus aria-hidden />
          Nuevo docente
        </Button>
      </div>

      {!TEACHERS_API_READY && (
        <SampleDataNotice reason="la API todavía no usa la facultad del admin; los cambios no se guardan." />
      )}

      {creating && (
        <NewTeacherForm
          onCancel={() => setCreating(false)}
          onCreated={(teacher) => {
            setCreating(false)
            setError(null)
            setNotice(`Se creó a ${teacher.fullName}. Recibirá un correo para crear su contraseña.`)
            teachers.setData((current) =>
              [...current, teacher].sort((a, b) => a.lastNames.localeCompare(b.lastNames, 'es')),
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
              label="Buscar por nombre, documento o correo"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <LoadStatus state={teachers.state} onRetry={teachers.reload} label="Cargando los docentes…" />
          {teachers.state.kind === 'success' && visible.length === 0 && (
            <p className="text-muted-foreground text-sm">
              {list.length === 0 ? 'Tu facultad todavía no tiene docentes.' : 'Ningún docente coincide.'}
            </p>
          )}
          {visible.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground border-b">
                  <tr>
                    <th className="py-2 pr-4 font-medium">Nombre</th>
                    <th className="py-2 pr-4 font-medium">Correo</th>
                    <th className="py-2 pr-4 font-medium">Documento</th>
                    <th className="py-2 pr-4 text-right font-medium">Grupos del periodo</th>
                    <th className="py-2 pr-4 font-medium">Estado</th>
                    <th className="py-2 font-medium">
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((teacher) => (
                    <tr key={teacher.id} className="border-b last:border-0">
                      <td className="py-3 pr-4">{teacher.fullName}</td>
                      <td className="py-3 pr-4 break-all">{teacher.email}</td>
                      <td className="py-3 pr-4 whitespace-nowrap">
                        {teacher.documentType} {teacher.documentNumber}
                      </td>
                      <td className="py-3 pr-4 text-right">{teacher.groupCount}</td>
                      <td className="py-3 pr-4">
                        <Badge variant={teacher.active ? 'default' : 'secondary'}>
                          {teacher.active ? 'Activo' : 'Desactivado'}
                        </Badge>
                      </td>
                      <td className="py-3 text-right whitespace-nowrap">
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={!teacher.active}
                          onClick={() => void resendLink(teacher)}
                          aria-label={`Reenviar enlace a ${teacher.fullName}`}
                        >
                          Reenviar enlace
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => void toggleActive(teacher)}
                          aria-label={`${teacher.active ? 'Desactivar' : 'Reactivar'} a ${teacher.fullName}`}
                        >
                          {teacher.active ? 'Desactivar' : 'Reactivar'}
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

interface TeacherForm {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
}

const EMPTY_FORM: TeacherForm = { firstNames: '', lastNames: '', documentType: 'CC', documentNumber: '', email: '' }

/** Formulario de un docente nuevo; queda en la facultad del admin. */
function NewTeacherForm({
  onCreated,
  onCancel,
}: {
  onCreated: (teacher: TeacherSummary) => void
  onCancel: () => void
}) {
  const [form, setForm] = useState<TeacherForm>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const update =
    (field: keyof TeacherForm) => (event: { target: { value: string } }) =>
      setForm((current) => ({ ...current, [field]: event.target.value }))

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    try {
      onCreated(await teacherService.create(toRequest(form)))
    } catch (createError) {
      setFieldErrors(getFieldErrors(createError))
      setError(getErrorMessage(createError))
      setSaving(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Nuevo docente</CardTitle>
        <CardDescription>Recibirá un correo para crear su contraseña.</CardDescription>
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
          </div>
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
function validate(form: TeacherForm): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!form.firstNames.trim()) errors.firstNames = 'Los nombres son obligatorios'
  if (!form.lastNames.trim()) errors.lastNames = 'Los apellidos son obligatorios'
  if (!form.documentNumber.trim()) errors.documentNumber = 'El número de documento es obligatorio'
  if (!form.email.trim()) errors.email = 'El correo es obligatorio'
  return errors
}

function toRequest(form: TeacherForm): NewTeacherRequest {
  return {
    firstNames: form.firstNames.trim(),
    lastNames: form.lastNames.trim(),
    documentType: form.documentType,
    documentNumber: form.documentNumber.trim(),
    email: form.email.trim(),
  }
}
