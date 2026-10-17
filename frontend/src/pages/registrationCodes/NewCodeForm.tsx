import { useState, type FormEvent } from 'react'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError, FormField } from '@/components/forms/FormField'
import { SelectField } from '@/components/forms/SelectField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { todayIso } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import { registrationCodeService } from '@/services/api/registrationCodeService'
import { teacherService } from '@/services/api/teacherService'
import type { UserSummary } from '@/types/auth'
import type { Program } from '@/types/catalog'
import type { RegistrationCode } from '@/types/registrationCode'

/** Programas de la facultad y, para el admin, sus docentes activos. */
interface CodeOptions {
  programs: Program[]
  teachers: UserSummary[]
}

/**
 * Formulario "Generar código de registro" (wireframes DO-10 y AD-07). El docente elige
 * el programa y el vencimiento; el admin, además, el docente a cuyo nombre queda. Los
 * programas son los de la facultad del usuario (la API solo le devuelve esos).
 */
export function NewCodeForm({
  forTeacher,
  onCreated,
  onCancel,
}: {
  /** `true` si lo genera el propio docente; si no, se pide el docente dueño del código. */
  forTeacher: boolean
  onCreated: (code: RegistrationCode) => void
  onCancel: () => void
}) {
  const options = useLoad(() => loadOptions(!forTeacher), [forTeacher])

  return (
    <Card>
      <CardHeader>
        <CardTitle>Generar código de registro</CardTitle>
        <CardDescription>
          {forTeacher
            ? 'Los estudiantes que lo usen quedan en el programa elegido y en tu lista.'
            : 'Los estudiantes que lo usen quedan en el programa elegido y en la lista del docente.'}{' '}
          Se puede usar muchas veces hasta su fecha de vencimiento.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <LoadStatus state={options.state} onRetry={options.reload} label="Cargando los programas…" />
        {options.state.kind === 'success' && (
          <CodeFields options={options.state.data} forTeacher={forTeacher} onCreated={onCreated} onCancel={onCancel} />
        )}
      </CardContent>
    </Card>
  )
}

function CodeFields({
  options,
  forTeacher,
  onCreated,
  onCancel,
}: {
  options: CodeOptions
  forTeacher: boolean
  onCreated: (code: RegistrationCode) => void
  onCancel: () => void
}) {
  const { programs, teachers } = options
  const [teacherId, setTeacherId] = useState('')
  // Con un solo programa no hay nada que elegir.
  const [programId, setProgramId] = useState(programs.length === 1 ? String(programs[0].id) : '')
  const [expiresOn, setExpiresOn] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  if (programs.length === 0) {
    return (
      <div className="flex flex-col items-start gap-3">
        <p className="text-muted-foreground text-sm">
          La facultad todavía no tiene programas. El super administrador debe crearlos primero.
        </p>
        <Button type="button" size="sm" variant="outline" onClick={onCancel}>
          Cerrar
        </Button>
      </div>
    )
  }
  if (!forTeacher && teachers.length === 0) {
    return (
      <div className="flex flex-col items-start gap-3">
        <p className="text-muted-foreground text-sm">
          La facultad no tiene docentes activos. Créalos primero en "Docentes".
        </p>
        <Button type="button" size="sm" variant="outline" onClick={onCancel}>
          Cerrar
        </Button>
      </div>
    )
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors: Record<string, string> = {}
    if (!forTeacher && !teacherId) errors.teacherId = 'Elige el docente'
    if (!programId) errors.programId = 'Elige el programa'
    if (!expiresOn) errors.expiresOn = 'La fecha de vencimiento es obligatoria'
    else if (expiresOn < todayIso()) errors.expiresOn = 'La fecha de vencimiento no puede estar en el pasado'
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    try {
      const code = await registrationCodeService.create({
        programId: Number(programId),
        expiresOn,
        ...(forTeacher ? {} : { teacherId: Number(teacherId) }),
      })
      onCreated(code)
    } catch (saveError) {
      setFieldErrors(getFieldErrors(saveError))
      setError(getErrorMessage(saveError))
      setSaving(false)
    }
  }

  return (
    <form aria-label="Generar código de registro" className="flex flex-col gap-4" onSubmit={submit} noValidate>
      <div className="grid gap-4 sm:grid-cols-3">
        {!forTeacher && (
          <SelectField
            label="Docente"
            value={teacherId}
            error={fieldErrors.teacherId}
            onChange={(event) => setTeacherId(event.target.value)}
          >
            <option value="">Elige un docente</option>
            {teachers.map((teacher) => (
              <option key={teacher.id} value={String(teacher.id)}>
                {teacher.fullName}
              </option>
            ))}
          </SelectField>
        )}
        <SelectField
          label="Programa"
          value={programId}
          error={fieldErrors.programId}
          onChange={(event) => setProgramId(event.target.value)}
        >
          {programs.length > 1 && <option value="">Elige un programa</option>}
          {programs.map((program) => (
            <option key={program.id} value={String(program.id)}>
              {program.name}
            </option>
          ))}
        </SelectField>
        <FormField
          label="Vence el"
          type="date"
          min={todayIso()}
          value={expiresOn}
          error={fieldErrors.expiresOn}
          onChange={(event) => setExpiresOn(event.target.value)}
        />
      </div>
      <FormError message={error} />
      <div className="flex gap-3">
        <Button type="submit" size="sm" disabled={saving}>
          {saving ? 'Generando…' : 'Generar'}
        </Button>
        <Button type="button" size="sm" variant="outline" onClick={onCancel}>
          Cancelar
        </Button>
      </div>
    </form>
  )
}

/**
 * Programas de la facultad del usuario y, si `withTeachers`, los docentes activos de la
 * facultad del admin (la API le devuelve solo su facultad), por nombre.
 */
async function loadOptions(withTeachers: boolean): Promise<CodeOptions> {
  const [programs, teachers] = await Promise.all([
    catalogService.programs(),
    withTeachers ? facultyTeachers() : Promise.resolve([]),
  ])
  return {
    programs,
    teachers: teachers
      .filter((teacher) => teacher.active)
      .sort((a, b) => a.fullName.localeCompare(b.fullName, 'es')),
  }
}

async function facultyTeachers(): Promise<UserSummary[]> {
  const [faculty] = await catalogService.faculties()
  return faculty ? teacherService.list(faculty.id) : []
}
