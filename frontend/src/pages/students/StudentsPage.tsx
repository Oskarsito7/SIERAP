import { useState, type FormEvent } from 'react'
import { UserPlus } from 'lucide-react'
import { Link } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { LoadStatus } from '@/components/LoadStatus'
import { FormError, FormField } from '@/components/forms/FormField'
import { SelectField } from '@/components/forms/SelectField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { DOCUMENT_TYPES } from '@/lib/documentTypes'
import { useLoad } from '@/lib/useLoad'
import { accountService } from '@/services/api/accountService'
import { catalogService } from '@/services/api/catalogService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import { studentService } from '@/services/api/studentService'
import type { NewStudentRequest, Student } from '@/types/account'
import type { DocumentType, UserSummary } from '@/types/auth'
import type { Program } from '@/types/catalog'

/**
 * "Mis estudiantes" del docente (wireframe DO-08): los estudiantes de cada programa de
 * su facultad con su código, su semestre y cómo llegaron, la creación de uno nuevo, el
 * reenvío del enlace para crear la contraseña y la desactivación de la cuenta. El docente
 * solo gestiona a los que creó o se registraron con un código suyo: con los demás no se
 * muestran esas acciones (`manageable`). El filtro "Sin grupo" ayuda a agregar a un
 * grupo a quienes no se inscribieron (DO-02). La carga masiva queda para el Momento 2.
 */
export function StudentsPage() {
  const programs = useLoad(() => catalogService.programs(), [])

  if (programs.state.kind !== 'success') {
    return (
      <div className="flex flex-col gap-6">
        <h1 className="text-2xl font-bold tracking-tight">Mis estudiantes</h1>
        <LoadStatus state={programs.state} onRetry={programs.reload} label="Cargando los programas…" />
      </div>
    )
  }

  if (programs.state.data.length === 0) {
    return (
      <div className="flex flex-col gap-6">
        <h1 className="text-2xl font-bold tracking-tight">Mis estudiantes</h1>
        <p className="text-muted-foreground text-sm">Tu facultad todavía no tiene programas.</p>
      </div>
    )
  }
  return <ProgramStudents programs={programs.state.data} />
}

function ProgramStudents({ programs }: { programs: Program[] }) {
  const { user } = useAuth()
  const [programId, setProgramId] = useState(programs[0].id)
  // Momento del filtro "Sin grupo"; `null` muestra a todos los estudiantes del programa.
  const [withoutGroupIn, setWithoutGroupIn] = useState<number | null>(null)
  const moments = useLoad(() => momentService.list(), [])
  const students = useLoad(
    () => studentService.list(programId, withoutGroupIn ?? undefined),
    [programId, withoutGroupIn],
  )
  const [search, setSearch] = useState('')
  const [creating, setCreating] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const program = programs.find((item) => item.id === programId) ?? programs[0]
  const programMoments =
    moments.state.kind === 'success' ? moments.state.data.filter((moment) => moment.programId === programId) : []
  const filterMoment = programMoments.find((moment) => moment.id === withoutGroupIn)
  const list = students.state.kind === 'success' ? sortByLastNames(students.state.data) : []
  const term = search.trim().toLowerCase()
  const visible = term
    ? list.filter((student) =>
        `${student.fullName} ${student.studentCode} ${student.email}`.toLowerCase().includes(term),
      )
    : list

  const replace = (updated: Student) =>
    students.setData((current) => current.map((student) => (student.id === updated.id ? updated : student)))

  const changeProgram = (id: number) => {
    setProgramId(id)
    setWithoutGroupIn(null)
  }

  const resendLink = async (student: Student) => {
    setNotice(null)
    setError(null)
    try {
      await accountService.resendPasswordLink(student.id)
      setNotice(`Se envió de nuevo el enlace a ${student.email}.`)
    } catch (resendError) {
      setError(getErrorMessage(resendError))
    }
  }

  const toggleActive = async (student: Student) => {
    const question = `¿Desactivar la cuenta de ${student.fullName}? No podrá iniciar sesión.`
    if (student.active && !window.confirm(question)) return
    setNotice(null)
    setError(null)
    try {
      const updated = await studentService.setActive(student.id, !student.active)
      replace({ ...student, active: updated.active })
      setNotice(`Se ${updated.active ? 'reactivó' : 'desactivó'} la cuenta de ${student.fullName}.`)
    } catch (toggleError) {
      setError(getErrorMessage(toggleError))
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">Mis estudiantes</h1>
          <p className="text-muted-foreground max-w-2xl text-sm">
            Crea a tus estudiantes o compárteles un{' '}
            <Link to="/registration-codes" className="text-foreground underline underline-offset-4">
              código de registro
            </Link>
            . Cada estudiante nuevo recibe un correo para crear su contraseña.
          </p>
        </div>
        <Button onClick={() => setCreating((open) => !open)}>
          <UserPlus aria-hidden />
          Nuevo estudiante
        </Button>
      </div>

      {creating && (
        <NewStudentForm
          programs={programs}
          initialProgramId={programId}
          onCancel={() => setCreating(false)}
          onCreated={(student, createdIn) => {
            setCreating(false)
            setError(null)
            const target = programs.find((item) => item.id === createdIn)?.name ?? 'el programa'
            setNotice(`Se creó a ${student.fullName} en ${target}. Recibirá un correo para crear su contraseña.`)
            // Con el filtro "Sin grupo" no se sabe si el nuevo cumple la regla: se vuelve a pedir la lista.
            if (createdIn === programId && withoutGroupIn === null) {
              students.setData((current) => [...current, student])
            } else if (createdIn === programId) {
              students.reload()
            }
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
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <SelectField
              label="Programa"
              value={String(programId)}
              onChange={(event) => changeProgram(Number(event.target.value))}
            >
              {programs.map((item) => (
                <option key={item.id} value={String(item.id)}>
                  {item.name}
                </option>
              ))}
            </SelectField>
            {programMoments.length > 0 && (
              <SelectField
                label="Mostrar"
                value={withoutGroupIn === null ? '' : String(withoutGroupIn)}
                onChange={(event) => setWithoutGroupIn(event.target.value ? Number(event.target.value) : null)}
              >
                <option value="">Todos los estudiantes</option>
                {programMoments.map((moment) => (
                  <option key={moment.id} value={String(moment.id)}>
                    Sin grupo en {moment.name}
                  </option>
                ))}
              </SelectField>
            )}
            <FormField
              label="Buscar por nombre, código o correo"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <LoadStatus state={students.state} onRetry={students.reload} label="Cargando los estudiantes…" />
          {students.state.kind === 'success' && visible.length === 0 && (
            <p className="text-muted-foreground text-sm">
              {list.length > 0
                ? 'Ningún estudiante coincide.'
                : filterMoment
                  ? `No hay estudiantes sin grupo en ${filterMoment.name}.`
                  : `${program.name} todavía no tiene estudiantes.`}
            </p>
          )}
          {visible.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground border-b">
                  <tr>
                    <th className="py-2 pr-4 font-medium">Nombre</th>
                    <th className="py-2 pr-4 font-medium">Código</th>
                    <th className="py-2 pr-4 text-right font-medium">Semestre</th>
                    <th className="py-2 pr-4 font-medium">Correo</th>
                    <th className="py-2 pr-4 font-medium">Cómo llegó</th>
                    <th className="py-2 pr-4 font-medium">Estado</th>
                    <th className="py-2 font-medium">
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((student) => (
                    <tr key={student.id} className="border-b last:border-0">
                      <td className="py-3 pr-4">{student.fullName}</td>
                      <td className="py-3 pr-4 tabular-nums">{student.studentCode}</td>
                      <td className="py-3 pr-4 text-right tabular-nums">{student.currentSemester}</td>
                      <td className="py-3 pr-4 break-all">{student.email}</td>
                      <td className="py-3 pr-4">{arrival(student, user?.id)}</td>
                      <td className="py-3 pr-4">
                        <Badge variant={student.active ? 'default' : 'secondary'}>
                          {student.active ? 'Activo' : 'Desactivado'}
                        </Badge>
                      </td>
                      <td className="py-3 text-right whitespace-nowrap">
                        {student.manageable && (
                          <>
                            <Button
                              variant="ghost"
                              size="sm"
                              disabled={!student.active}
                              onClick={() => void resendLink(student)}
                              aria-label={`Reenviar enlace a ${student.fullName}`}
                            >
                              Reenviar enlace
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => void toggleActive(student)}
                              aria-label={`${student.active ? 'Desactivar' : 'Reactivar'} a ${student.fullName}`}
                            >
                              {student.active ? 'Desactivar' : 'Reactivar'}
                            </Button>
                          </>
                        )}
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

interface StudentForm {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
  programId: string
  studentCode: string
  currentSemester: string
}

/** Formulario de un estudiante nuevo en un programa de la facultad del docente. */
function NewStudentForm({
  programs,
  initialProgramId,
  onCreated,
  onCancel,
}: {
  programs: Program[]
  initialProgramId: number
  onCreated: (student: Student, programId: number) => void
  onCancel: () => void
}) {
  const [form, setForm] = useState<StudentForm>({
    firstNames: '',
    lastNames: '',
    documentType: 'CC',
    documentNumber: '',
    email: '',
    programId: String(initialProgramId),
    studentCode: '',
    currentSemester: '',
  })
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const update =
    (field: keyof StudentForm) => (event: { target: { value: string } }) =>
      setForm((current) => ({ ...current, [field]: event.target.value }))

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors = validate(form)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    try {
      const request = toRequest(form)
      onCreated(await studentService.create(request), request.programId)
    } catch (createError) {
      setFieldErrors(getFieldErrors(createError))
      setError(getErrorMessage(createError))
      setSaving(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Nuevo estudiante</CardTitle>
        <CardDescription>Recibirá un correo para crear su contraseña.</CardDescription>
      </CardHeader>
      <CardContent>
        <form aria-label="Nuevo estudiante" className="flex flex-col gap-4" onSubmit={submit} noValidate>
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
              label="Programa"
              value={form.programId}
              error={fieldErrors.programId}
              onChange={update('programId')}
            >
              {programs.map((program) => (
                <option key={program.id} value={String(program.id)}>
                  {program.name}
                </option>
              ))}
            </SelectField>
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
function validate(form: StudentForm): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!form.firstNames.trim()) errors.firstNames = 'Los nombres son obligatorios'
  if (!form.lastNames.trim()) errors.lastNames = 'Los apellidos son obligatorios'
  if (!form.documentNumber.trim()) errors.documentNumber = 'El número de documento es obligatorio'
  if (!form.email.trim()) errors.email = 'El correo es obligatorio'
  if (!form.studentCode.trim()) errors.studentCode = 'El código estudiantil es obligatorio'
  if (!form.currentSemester.trim()) errors.currentSemester = 'El semestre es obligatorio'
  else if (!/^\d+$/.test(form.currentSemester.trim()) || Number(form.currentSemester) < 1) {
    errors.currentSemester = 'El semestre debe ser 1 o mayor'
  }
  return errors
}

function toRequest(form: StudentForm): NewStudentRequest {
  return {
    firstNames: form.firstNames.trim(),
    lastNames: form.lastNames.trim(),
    documentType: form.documentType,
    documentNumber: form.documentNumber.trim(),
    email: form.email.trim(),
    programId: Number(form.programId),
    studentCode: form.studentCode.trim(),
    currentSemester: Number(form.currentSemester),
  }
}

function sortByLastNames<T extends UserSummary>(students: T[]): T[] {
  return [...students].sort((a, b) => a.lastNames.localeCompare(b.lastNames, 'es'))
}

/** Cómo llegó el estudiante: con un código de registro o creado por alguien. */
function arrival(student: Student, viewerId: number | undefined): string {
  if (student.registrationCode) return `Código ${student.registrationCode}`
  if (!student.createdBy) return '—'
  return student.createdBy.id === viewerId ? 'Creado por mí' : `Creado por ${student.createdBy.fullName}`
}
