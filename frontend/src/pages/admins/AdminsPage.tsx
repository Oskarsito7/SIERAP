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
import { adminService } from '@/services/api/adminService'
import { catalogService } from '@/services/api/catalogService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import type { NewAdminRequest } from '@/types/account'
import type { DocumentType, UserSummary } from '@/types/auth'
import type { Faculty } from '@/types/catalog'
import { ResponsiveTable } from '@/components/ui/responsive-table'

/**
 * Administradores de facultad para el super_admin (wireframe SA-05): la lista de una
 * facultad, el reenvío del enlace para crear la contraseña, la desactivación de la
 * cuenta y el formulario para crear uno nuevo (ADR-017). La facultad se elige primero
 * porque `GET /api/admins` pide `facultyId`; por eso no hay "Todas las facultades".
 */
export function AdminsPage() {
  const faculties = useLoad(() => catalogService.faculties(), [])
  const [facultyId, setFacultyId] = useState<number | null>(null)

  if (faculties.state.kind !== 'success') {
    return (
      <div className="flex flex-col gap-6">
        <h1 className="text-2xl font-bold tracking-tight">Administradores</h1>
        <LoadStatus state={faculties.state} onRetry={faculties.reload} label="Cargando las facultades…" />
      </div>
    )
  }

  const list = faculties.state.data
  if (list.length === 0) {
    return (
      <div className="flex flex-col gap-6">
        <h1 className="text-2xl font-bold tracking-tight">Administradores</h1>
        <p className="text-muted-foreground text-sm">Primero crea una facultad en "Facultades y programas".</p>
      </div>
    )
  }
  const faculty = list.find((item) => item.id === facultyId) ?? list[0]
  return (
    <FacultyAdmins key={faculty.id} faculty={faculty} faculties={list} onFacultyChange={setFacultyId} />
  )
}

interface FacultyAdminsProps {
  faculty: Faculty
  faculties: Faculty[]
  onFacultyChange: (facultyId: number) => void
}

function FacultyAdmins({ faculty, faculties, onFacultyChange }: FacultyAdminsProps) {
  const admins = useLoad(() => adminService.list(faculty.id), [faculty.id])
  const [search, setSearch] = useState('')
  const [creating, setCreating] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const list = admins.state.kind === 'success' ? admins.state.data : []
  const term = search.trim().toLowerCase()
  const visible = term
    ? list.filter((admin) => `${admin.fullName} ${admin.email}`.toLowerCase().includes(term))
    : list

  const replace = (updated: UserSummary) =>
    admins.setData((current) => current.map((admin) => (admin.id === updated.id ? updated : admin)))

  const resendLink = async (admin: UserSummary) => {
    setNotice(null)
    setError(null)
    try {
      await accountService.resendPasswordLink(admin.id)
      setNotice(`Se envió de nuevo el enlace a ${admin.email}.`)
    } catch (resendError) {
      setError(getErrorMessage(resendError))
    }
  }

  const toggleActive = async (admin: UserSummary) => {
    const question = `¿Desactivar la cuenta de ${admin.fullName}? No podrá iniciar sesión.`
    if (admin.active && !window.confirm(question)) return
    setNotice(null)
    setError(null)
    try {
      const updated = await adminService.setActive(admin.id, !admin.active)
      replace({ ...admin, active: updated.active })
      setNotice(`Se ${updated.active ? 'reactivó' : 'desactivó'} la cuenta de ${admin.fullName}.`)
    } catch (toggleError) {
      setError(getErrorMessage(toggleError))
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">Administradores</h1>
          <p className="text-muted-foreground max-w-2xl text-sm">
            Una facultad puede tener varios administradores con los mismos permisos. Cada uno nuevo recibe un
            correo para crear su contraseña.
          </p>
        </div>
        <Button onClick={() => setCreating((open) => !open)}>
          <UserPlus aria-hidden />
          Nuevo administrador
        </Button>
      </div>

      {creating && (
        <NewAdminForm
          faculty={faculty}
          onCancel={() => setCreating(false)}
          onCreated={(admin) => {
            setCreating(false)
            setError(null)
            setNotice(`Se creó a ${admin.fullName}. Recibirá un correo para crear su contraseña.`)
            admins.setData((current) =>
              [...current, admin].sort((a, b) => a.lastNames.localeCompare(b.lastNames, 'es')),
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
          <div className="grid gap-4 md:max-w-2xl md:grid-cols-2">
            <SelectField
              label="Facultad"
              value={String(faculty.id)}
              onChange={(event) => onFacultyChange(Number(event.target.value))}
            >
              {faculties.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </SelectField>
            <FormField
              label="Buscar por nombre o correo"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <LoadStatus state={admins.state} onRetry={admins.reload} label="Cargando los administradores…" />
          {admins.state.kind === 'success' && visible.length === 0 && (
            <p className="text-muted-foreground text-sm">
              {list.length === 0
                ? 'Esta facultad todavía no tiene administradores.'
                : 'Ningún administrador coincide.'}
            </p>
          )}
          {visible.length > 0 && (
            <ResponsiveTable>
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
                {visible.map((admin) => (
                  <tr key={admin.id} className="border-b last:border-0">
                    <td className="py-3 pr-4">{admin.fullName}</td>
                    <td className="py-3 pr-4 break-all">{admin.email}</td>
                    <td className="py-3 pr-4">
                      <Badge variant={admin.active ? 'default' : 'secondary'}>
                        {admin.active ? 'Activo' : 'Desactivado'}
                      </Badge>
                    </td>
                    <td className="py-3 text-right whitespace-nowrap">
                      <Button
                        variant="ghost"
                        size="sm"
                        disabled={!admin.active}
                        onClick={() => void resendLink(admin)}
                        aria-label={`Reenviar enlace a ${admin.fullName}`}
                      >
                        Reenviar enlace
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() => void toggleActive(admin)}
                        aria-label={`${admin.active ? 'Desactivar' : 'Reactivar'} a ${admin.fullName}`}
                      >
                        {admin.active ? 'Desactivar' : 'Reactivar'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </ResponsiveTable>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

interface AdminForm {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
}

const EMPTY_FORM: AdminForm = { firstNames: '', lastNames: '', documentType: 'CC', documentNumber: '', email: '' }

/** Formulario de un administrador nuevo de la facultad elegida. */
function NewAdminForm({
  faculty,
  onCreated,
  onCancel,
}: {
  faculty: Faculty
  onCreated: (admin: UserSummary) => void
  onCancel: () => void
}) {
  const [form, setForm] = useState<AdminForm>(EMPTY_FORM)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const update =
    (field: keyof AdminForm) => (event: { target: { value: string } }) =>
      setForm((current) => ({ ...current, [field]: event.target.value }))

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    try {
      onCreated(await adminService.create(faculty.id, toRequest(form)))
    } catch (createError) {
      setFieldErrors(getFieldErrors(createError))
      setError(getErrorMessage(createError))
      setSaving(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Nuevo administrador</CardTitle>
        <CardDescription>
          Queda en la {faculty.name} y recibirá un correo para crear su contraseña.
        </CardDescription>
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
function validate(form: AdminForm): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!form.firstNames.trim()) errors.firstNames = 'Los nombres son obligatorios'
  if (!form.lastNames.trim()) errors.lastNames = 'Los apellidos son obligatorios'
  if (!form.documentNumber.trim()) errors.documentNumber = 'El número de documento es obligatorio'
  if (!form.email.trim()) errors.email = 'El correo es obligatorio'
  return errors
}

function toRequest(form: AdminForm): NewAdminRequest {
  return {
    firstNames: form.firstNames.trim(),
    lastNames: form.lastNames.trim(),
    documentType: form.documentType,
    documentNumber: form.documentNumber.trim(),
    email: form.email.trim(),
  }
}
