import { useState, type FormEvent } from 'react'
import { Plus } from 'lucide-react'
import { useSearchParams } from 'react-router'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError, FormField } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'
import type { CatalogEntryRequest, Faculty, Program } from '@/types/catalog'

/**
 * Facultades y programas (wireframe SA-01, super_admin): la lista de facultades y, de
 * la elegida, sus datos y sus programas con sus momentos del periodo actual y sus
 * estudiantes. Eliminar solo funciona si nada cuelga del recurso; si no, se muestra el
 * motivo que da la API (ADR-016). La facultad elegida queda en la dirección (`?facultyId=`).
 */
export function FacultiesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const faculties = useLoad(() => catalogService.faculties(), [])
  const [creating, setCreating] = useState(false)

  const list = faculties.state.kind === 'success' ? faculties.state.data : []
  const selected = list.find((faculty) => String(faculty.id) === searchParams.get('facultyId')) ?? list[0]
  const select = (id: number) => setSearchParams({ facultyId: String(id) })

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">Facultades y programas</h1>
          <p className="text-muted-foreground text-sm">Elige una facultad para ver y editar sus programas.</p>
        </div>
        <Button variant="outline" onClick={() => setCreating((open) => !open)}>
          <Plus aria-hidden />
          Nueva facultad
        </Button>
      </div>

      {creating && (
        <EntryForm
          title="Nueva facultad"
          submitLabel="Crear facultad"
          onCancel={() => setCreating(false)}
          onSave={async (request) => {
            const created = await catalogService.createFaculty(request)
            setCreating(false)
            faculties.setData((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name, 'es')))
            select(created.id)
          }}
        />
      )}

      <LoadStatus state={faculties.state} onRetry={faculties.reload} label="Cargando las facultades…" />
      {faculties.state.kind === 'success' && list.length === 0 && (
        <p className="text-muted-foreground text-sm">Todavía no hay facultades.</p>
      )}

      {selected && (
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
          <Card className="self-start">
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>Facultades</CardTitle>
              <Badge variant="secondary">{list.length}</Badge>
            </CardHeader>
            <CardContent>
              <ul className="flex flex-col gap-2">
                {list.map((faculty) => (
                  <li key={faculty.id}>
                    <button
                      type="button"
                      aria-current={faculty.id === selected.id ? 'true' : undefined}
                      onClick={() => select(faculty.id)}
                      className={`w-full rounded-md border px-4 py-3 text-left text-sm ${
                        faculty.id === selected.id ? 'border-primary border-2' : 'hover:bg-muted/50'
                      }`}
                    >
                      <span className="block font-semibold">{faculty.name}</span>
                      <span className="text-muted-foreground">
                        Código {faculty.code} · {programsLabel(faculty.programCount)}
                      </span>
                    </button>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>

          <FacultyDetail
            key={selected.id}
            faculty={selected}
            onChange={(updated) =>
              faculties.setData((current) => current.map((item) => (item.id === updated.id ? updated : item)))
            }
            onDeleted={() => {
              faculties.setData((current) => current.filter((item) => item.id !== selected.id))
              setSearchParams({})
            }}
          />
        </div>
      )}
    </div>
  )
}

/** Datos de la facultad elegida y sus programas. */
function FacultyDetail({
  faculty,
  onChange,
  onDeleted,
}: {
  faculty: Faculty
  onChange: (faculty: Faculty) => void
  onDeleted: () => void
}) {
  const programs = useLoad(() => catalogService.programs(faculty.id), [faculty.id])
  const [editing, setEditing] = useState(false)
  const [adding, setAdding] = useState(false)
  const [editingProgram, setEditingProgram] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  const sortByName = (list: Program[]) => [...list].sort((a, b) => a.name.localeCompare(b.name, 'es'))
  const programCount = (delta: number) => onChange({ ...faculty, programCount: faculty.programCount + delta })

  const removeFaculty = async () => {
    if (!window.confirm(`¿Eliminar la ${faculty.name} con sus programas?`)) return
    setError(null)
    try {
      await catalogService.deleteFaculty(faculty.id)
      onDeleted()
    } catch (deleteError) {
      setError(getErrorMessage(deleteError))
    }
  }

  const removeProgram = async (program: Program) => {
    if (!window.confirm(`¿Eliminar el programa ${program.name}?`)) return
    setError(null)
    try {
      await catalogService.deleteProgram(program.id)
      programs.setData((current) => current.filter((item) => item.id !== program.id))
      programCount(-1)
    } catch (deleteError) {
      setError(`No se puede eliminar ${program.name}: ${getErrorMessage(deleteError)}`)
    }
  }

  return (
    <Card>
      <CardHeader className="flex flex-row flex-wrap items-start justify-between gap-3">
        <div>
          <CardTitle>{faculty.name}</CardTitle>
          <CardDescription>Código {faculty.code}</CardDescription>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button variant="outline" size="sm" onClick={() => setEditing((open) => !open)}>
            Editar facultad
          </Button>
          <Button variant="ghost" size="sm" onClick={() => void removeFaculty()}>
            Eliminar facultad
          </Button>
          <Button size="sm" onClick={() => setAdding((open) => !open)}>
            <Plus aria-hidden />
            Nuevo programa
          </Button>
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {editing && (
          <EntryForm
            title={`Editar ${faculty.name}`}
            initial={{ code: faculty.code, name: faculty.name }}
            submitLabel="Guardar"
            onCancel={() => setEditing(false)}
            onSave={async (request) => {
              onChange(await catalogService.updateFaculty(faculty.id, request))
              setEditing(false)
            }}
          />
        )}
        {adding && (
          <EntryForm
            title={`Nuevo programa de la ${faculty.name}`}
            submitLabel="Crear programa"
            onCancel={() => setAdding(false)}
            onSave={async (request) => {
              const created = await catalogService.createProgram(faculty.id, request)
              programs.setData((current) => sortByName([...current, created]))
              programCount(1)
              setAdding(false)
            }}
          />
        )}
        {error && (
          <p role="alert" className="bg-muted rounded-md border px-4 py-3 text-sm">
            {error}
          </p>
        )}

        <LoadStatus state={programs.state} onRetry={programs.reload} label="Cargando los programas…" />
        {programs.state.kind === 'success' && programs.state.data.length === 0 && (
          <p className="text-muted-foreground text-sm">Esta facultad todavía no tiene programas.</p>
        )}
        {programs.state.kind === 'success' && programs.state.data.length > 0 && (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground border-b">
                <tr>
                  <th className="py-2 pr-4 font-medium">Código</th>
                  <th className="py-2 pr-4 font-medium">Programa</th>
                  <th className="py-2 pr-4 text-right font-medium">Momentos del periodo</th>
                  <th className="py-2 pr-4 text-right font-medium">Estudiantes</th>
                  <th className="py-2 font-medium">
                    <span className="sr-only">Acciones</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {programs.state.data.map((program) =>
                  editingProgram === program.id ? (
                    <tr key={program.id} className="border-b">
                      <td colSpan={5} className="py-3">
                        <EntryForm
                          title={`Editar ${program.name}`}
                          initial={{ code: program.code, name: program.name }}
                          submitLabel="Guardar"
                          onCancel={() => setEditingProgram(null)}
                          onSave={async (request) => {
                            const updated = await catalogService.updateProgram(program.id, request)
                            programs.setData((current) =>
                              sortByName(current.map((item) => (item.id === updated.id ? updated : item))),
                            )
                            setEditingProgram(null)
                          }}
                        />
                      </td>
                    </tr>
                  ) : (
                    <tr key={program.id} className="border-b last:border-0">
                      <td className="py-3 pr-4">{program.code}</td>
                      <td className="py-3 pr-4">{program.name}</td>
                      <td className="py-3 pr-4 text-right">{program.currentPeriodMomentCount}</td>
                      <td className="py-3 pr-4 text-right">{program.studentCount}</td>
                      <td className="py-3 text-right whitespace-nowrap">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setEditingProgram(program.id)}
                          aria-label={`Editar ${program.name}`}
                        >
                          Editar
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => void removeProgram(program)}
                          aria-label={`Eliminar ${program.name}`}
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
  )
}

/** Formulario de código y nombre, para facultades y programas. */
function EntryForm({
  title,
  initial = { code: '', name: '' },
  submitLabel,
  onSave,
  onCancel,
}: {
  title: string
  initial?: CatalogEntryRequest
  submitLabel: string
  onSave: (request: CatalogEntryRequest) => Promise<void>
  onCancel: () => void
}) {
  const [code, setCode] = useState(initial.code)
  const [name, setName] = useState(initial.name)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors: Record<string, string> = {}
    if (!code.trim()) errors.code = 'El código es obligatorio'
    if (!name.trim()) errors.name = 'El nombre es obligatorio'
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSaving(true)
    try {
      await onSave({ code: code.trim(), name: name.trim() })
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
      <div className="grid gap-3 sm:grid-cols-[10rem_1fr]">
        <FormField
          label="Código"
          value={code}
          maxLength={20}
          error={fieldErrors.code}
          onChange={(event) => setCode(event.target.value)}
        />
        <FormField
          label="Nombre"
          value={name}
          maxLength={150}
          error={fieldErrors.name}
          onChange={(event) => setName(event.target.value)}
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

function programsLabel(count: number): string {
  return count === 1 ? '1 programa' : `${count} programas`
}
