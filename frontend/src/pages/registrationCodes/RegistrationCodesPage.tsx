import { useEffect, useMemo, useState } from 'react'
import { Check, Copy, LoaderCircle, Plus, RefreshCw } from 'lucide-react'

import { useAuth } from '@/auth/authContext'
import { FormError } from '@/components/forms/FormField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatDate, todayIso } from '@/lib/dates'
import { getErrorMessage } from '@/services/api/httpClient'
import { registrationCodeService } from '@/services/api/registrationCodeService'
import type { RegistrationCode } from '@/types/registrationCode'

import { NewCodeForm } from './NewCodeForm'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'success'; codes: RegistrationCode[] }
  | { kind: 'error'; message: string }

/** Estado de un código tal como lo ve el usuario. */
type CodeStatus = 'Activo' | 'Vencido' | 'Desactivado'

const ALL_TEACHERS = 'all'

/**
 * Códigos de registro (wireframes DO-10 y AD-07). El docente ve los suyos; el admin, los
 * de los docentes de su facultad, con un filtro por docente. Un código activo se copia
 * para compartirlo y se puede desactivar antes de que venza (no se reactiva). "Generar
 * código" abre el formulario; el admin elige además el docente dueño del código.
 */
export function RegistrationCodesPage() {
  const { user } = useAuth()
  const [load, setLoad] = useState<LoadState>({ kind: 'loading' })
  const [attempt, setAttempt] = useState(0)
  const [teacherFilter, setTeacherFilter] = useState(ALL_TEACHERS)
  const [actionError, setActionError] = useState<string | null>(null)
  const [copiedId, setCopiedId] = useState<number | null>(null)
  const [deactivatingId, setDeactivatingId] = useState<number | null>(null)
  const [creating, setCreating] = useState(false)
  const [notice, setNotice] = useState<string | null>(null)

  const isTeacher = user?.role === 'TEACHER'

  useEffect(() => {
    let cancelled = false
    registrationCodeService
      .list()
      .then((codes) => {
        if (!cancelled) setLoad({ kind: 'success', codes })
      })
      .catch((error: unknown) => {
        if (!cancelled) setLoad({ kind: 'error', message: getErrorMessage(error) })
      })
    return () => {
      cancelled = true
    }
  }, [attempt])

  const codes = useMemo(() => (load.kind === 'success' ? load.codes : []), [load])
  const teachers = useMemo(() => {
    const byId = new Map(codes.map((code) => [code.teacherId, code.teacherName]))
    return [...byId.entries()].sort((a, b) => a[1].localeCompare(b[1], 'es'))
  }, [codes])
  const visible =
    teacherFilter === ALL_TEACHERS ? codes : codes.filter((code) => String(code.teacherId) === teacherFilter)

  const retry = () => {
    setLoad({ kind: 'loading' })
    setAttempt((previous) => previous + 1)
  }

  const created = (code: RegistrationCode) => {
    setLoad((current) => (current.kind === 'success' ? { kind: 'success', codes: [code, ...current.codes] } : current))
    setCreating(false)
    setTeacherFilter(ALL_TEACHERS)
    setNotice(`Se generó el código ${code.code} para ${code.programName}.`)
  }

  const copy = async (code: RegistrationCode) => {
    setActionError(null)
    try {
      await navigator.clipboard.writeText(code.code)
      setCopiedId(code.id)
    } catch {
      setActionError('No se pudo copiar el código. Cópialo a mano.')
    }
  }

  const deactivate = async (code: RegistrationCode) => {
    const confirmed = window.confirm(
      `¿Desactivar el código ${code.code}? Dejará de servir de inmediato y no se podrá volver a activar.`,
    )
    if (!confirmed) return
    setActionError(null)
    setDeactivatingId(code.id)
    try {
      const updated = await registrationCodeService.deactivate(code.id)
      setLoad((current) =>
        current.kind === 'success'
          ? { kind: 'success', codes: current.codes.map((item) => (item.id === updated.id ? updated : item)) }
          : current,
      )
    } catch (error) {
      setActionError(getErrorMessage(error))
    } finally {
      setDeactivatingId(null)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div className="flex flex-col gap-1">
          <h1 className="text-2xl font-bold tracking-tight">Códigos de registro</h1>
          <p className="text-muted-foreground max-w-2xl text-sm">
            {isTeacher
              ? 'Tus estudiantes se registran con un código tuyo y su correo institucional; quedan en tu lista y en el programa del código.'
              : 'Los estudiantes que usan un código quedan en el programa elegido y en la lista de su docente.'}{' '}
            La dirección de registro es <span className="font-medium">{registerUrl()}</span>.
          </p>
        </div>
        <Button
          onClick={() => {
            setNotice(null)
            setCreating((open) => !open)
          }}
        >
          <Plus aria-hidden />
          Generar código
        </Button>
      </div>

      {creating && (
        <NewCodeForm forTeacher={isTeacher} onCreated={created} onCancel={() => setCreating(false)} />
      )}
      {notice && (
        <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
          {notice}
        </p>
      )}
      <FormError message={actionError} />

      <Card>
        <CardHeader>
          <CardTitle>{isTeacher ? 'Mis códigos' : 'Códigos de la facultad'}</CardTitle>
          <CardDescription>
            Un código sirve para muchos registros hasta que vence. Si lo desactivas, genera uno nuevo.
          </CardDescription>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          {!isTeacher && teachers.length > 1 && (
            <label className="flex max-w-xs flex-col gap-2 text-sm font-medium">
              Docente
              <select
                className="border-input bg-background h-9 rounded-md border px-3 text-sm font-normal"
                value={teacherFilter}
                onChange={(event) => setTeacherFilter(event.target.value)}
              >
                <option value={ALL_TEACHERS}>Todos los docentes</option>
                {teachers.map(([id, name]) => (
                  <option key={id} value={String(id)}>
                    {name}
                  </option>
                ))}
              </select>
            </label>
          )}

          {load.kind === 'loading' && (
            <p className="text-muted-foreground flex items-center gap-2 text-sm">
              <LoaderCircle className="size-4 animate-spin" aria-hidden />
              Cargando los códigos…
            </p>
          )}

          {load.kind === 'error' && (
            <div className="flex flex-col items-start gap-3">
              <FormError message={load.message} />
              <Button variant="outline" size="sm" onClick={retry}>
                <RefreshCw aria-hidden />
                Reintentar
              </Button>
            </div>
          )}

          {load.kind === 'success' && visible.length === 0 && (
            <p className="text-muted-foreground text-sm">Todavía no hay códigos de registro.</p>
          )}

          {load.kind === 'success' && visible.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-muted-foreground border-b">
                  <tr>
                    <th className="py-2 pr-4 font-medium">Código</th>
                    {!isTeacher && <th className="py-2 pr-4 font-medium">Docente</th>}
                    <th className="py-2 pr-4 font-medium">Programa</th>
                    <th className="py-2 pr-4 font-medium">Vence</th>
                    <th className="py-2 pr-4 font-medium">Registrados</th>
                    <th className="py-2 pr-4 font-medium">Estado</th>
                    <th className="py-2 font-medium">
                      <span className="sr-only">Acciones</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {visible.map((code) => {
                    const status = statusOf(code)
                    return (
                      <tr key={code.id} className="border-b last:border-0">
                        <td className="py-3 pr-4 font-mono font-semibold tracking-wider">{code.code}</td>
                        {!isTeacher && <td className="py-3 pr-4">{code.teacherName}</td>}
                        <td className="py-3 pr-4">{code.programName}</td>
                        <td className="py-3 pr-4">{formatDate(code.expiresOn)}</td>
                        <td className="py-3 pr-4">{code.registeredCount}</td>
                        <td className="py-3 pr-4">
                          <Badge variant={status === 'Activo' ? 'default' : 'secondary'}>{status}</Badge>
                        </td>
                        <td className="py-3">
                          {status === 'Activo' && (
                            <div className="flex justify-end gap-2">
                              <Button
                                variant="outline"
                                size="sm"
                                onClick={() => void copy(code)}
                                aria-label={`Copiar el código ${code.code}`}
                              >
                                {copiedId === code.id ? <Check aria-hidden /> : <Copy aria-hidden />}
                                {copiedId === code.id ? 'Copiado' : 'Copiar'}
                              </Button>
                              <Button
                                variant="ghost"
                                size="sm"
                                disabled={deactivatingId === code.id}
                                onClick={() => void deactivate(code)}
                                aria-label={`Desactivar el código ${code.code}`}
                              >
                                Desactivar
                              </Button>
                            </div>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

/** Un código desactivado lo es para siempre; si no, vence al pasar su fecha. */
function statusOf(code: RegistrationCode): CodeStatus {
  if (!code.active) return 'Desactivado'
  return code.expiresOn < todayIso() ? 'Vencido' : 'Activo'
}

/** Dirección que el estudiante abre para registrarse con el código. */
function registerUrl(): string {
  return `${window.location.origin}/register`
}
