import { useState, type FormEvent } from 'react'
import { Search } from 'lucide-react'

import { LoadStatus } from '@/components/LoadStatus'
import { FormField } from '@/components/forms/FormField'
import { SelectField } from '@/components/forms/SelectField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ResponsiveTable } from '@/components/ui/responsive-table'
import { formatDateTime } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { auditService } from '@/services/api/auditService'
import type { AuditAction } from '@/types/audit'

import { AUDIT_ACTION_LABELS } from './auditLabels'

/** Tamaño de página de la tabla. */
const PAGE_SIZE = 50

interface Filters {
  action: AuditAction | ''
  from: string
  to: string
  q: string
}

const NO_FILTERS: Filters = { action: '', from: '', to: '', q: '' }

/**
 * Bitácora de auditoría (ADR-023): quién hizo qué y cuándo en las operaciones
 * sensibles, del hecho más reciente al más antiguo. El super_admin ve todo el sistema y
 * el admin, solo los hechos de su facultad. Es de solo lectura: ningún registro se
 * edita ni se borra. Se filtra por tipo de hecho, rango de fechas y texto, y se pagina.
 */
export function AuditLogPage() {
  const [draft, setDraft] = useState<Filters>(NO_FILTERS)
  const [applied, setApplied] = useState<Filters>(NO_FILTERS)
  const [page, setPage] = useState(0)
  const log = useLoad(
    () =>
      auditService.list({
        page,
        size: PAGE_SIZE,
        action: applied.action || undefined,
        from: applied.from,
        to: applied.to,
        q: applied.q.trim(),
      }),
    [applied, page],
  )

  const invalidRange = draft.from !== '' && draft.to !== '' && draft.to < draft.from

  const search = (event: FormEvent) => {
    event.preventDefault()
    if (invalidRange) return
    setPage(0)
    setApplied(draft)
  }

  const clear = () => {
    setDraft(NO_FILTERS)
    setApplied(NO_FILTERS)
    setPage(0)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">Bitácora</h1>
        <p className="text-muted-foreground max-w-2xl text-sm">
          Registro de los hechos sensibles del sistema: cuentas, códigos, momentos, parámetros, evaluaciones y grupos.
          No se puede editar ni borrar.
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Filtros</CardTitle>
          <CardDescription>Combina el tipo de hecho, las fechas y un texto del resumen o del nombre.</CardDescription>
        </CardHeader>
        <CardContent>
          <form onSubmit={search} className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4" noValidate>
            <SelectField
              label="Acción"
              value={draft.action}
              onChange={(event) => setDraft({ ...draft, action: event.target.value as AuditAction | '' })}
            >
              <option value="">Todas las acciones</option>
              {(Object.keys(AUDIT_ACTION_LABELS) as AuditAction[]).map((action) => (
                <option key={action} value={action}>
                  {AUDIT_ACTION_LABELS[action]}
                </option>
              ))}
            </SelectField>
            <FormField
              label="Desde"
              type="date"
              value={draft.from}
              onChange={(event) => setDraft({ ...draft, from: event.target.value })}
            />
            <FormField
              label="Hasta"
              type="date"
              value={draft.to}
              onChange={(event) => setDraft({ ...draft, to: event.target.value })}
              error={invalidRange ? 'La fecha final no puede ser anterior a la inicial' : undefined}
            />
            <FormField
              label="Buscar"
              type="search"
              placeholder="Texto del resumen o del actor"
              value={draft.q}
              onChange={(event) => setDraft({ ...draft, q: event.target.value })}
            />
            <div className="flex gap-3 sm:col-span-2 lg:col-span-4">
              <Button type="submit" disabled={invalidRange}>
                <Search aria-hidden />
                Buscar
              </Button>
              <Button type="button" variant="outline" onClick={clear}>
                Limpiar filtros
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="flex flex-col gap-4">
          <LoadStatus state={log.state} onRetry={log.reload} label="Cargando la bitácora…" />

          {log.state.kind === 'success' && log.state.data.items.length === 0 && (
            <p className="text-muted-foreground text-sm">No hay registros con esos filtros.</p>
          )}

          {log.state.kind === 'success' && log.state.data.items.length > 0 && (
            <>
              <ResponsiveTable>
                <thead className="text-muted-foreground border-b">
                  <tr>
                    <th className="py-2 pr-4 font-medium">Fecha</th>
                    <th className="py-2 pr-4 font-medium">Actor</th>
                    <th className="py-2 pr-4 font-medium">Acción</th>
                    <th className="py-2 font-medium">Resumen</th>
                  </tr>
                </thead>
                <tbody>
                  {log.state.data.items.map((entry) => (
                    <tr key={entry.id} className="border-b last:border-0">
                      <td className="py-3 pr-4 whitespace-nowrap">{formatDateTime(entry.createdAt)}</td>
                      <td className="py-3 pr-4">{entry.actorName ?? 'Sistema'}</td>
                      <td className="py-3 pr-4">
                        <Badge variant="secondary">{AUDIT_ACTION_LABELS[entry.action] ?? entry.action}</Badge>
                      </td>
                      <td className="py-3">{entry.summary}</td>
                    </tr>
                  ))}
                </tbody>
              </ResponsiveTable>
              <Pagination
                page={log.state.data.page}
                size={log.state.data.size}
                total={log.state.data.total}
                onPage={setPage}
              />
            </>
          )}
        </CardContent>
      </Card>
    </div>
  )
}

/** "Página 2 de 7 · 312 registros" con los botones para moverse entre páginas. */
function Pagination({
  page,
  size,
  total,
  onPage,
}: {
  page: number
  size: number
  total: number
  onPage: (page: number) => void
}) {
  const pages = Math.max(1, Math.ceil(total / size))
  return (
    <nav aria-label="Paginación" className="flex flex-wrap items-center justify-between gap-3 text-sm">
      <span className="text-muted-foreground">
        Página {page + 1} de {pages} · {total === 1 ? '1 registro' : `${total} registros`}
      </span>
      <div className="flex gap-2">
        <Button variant="outline" size="sm" disabled={page === 0} onClick={() => onPage(page - 1)}>
          Anterior
        </Button>
        <Button variant="outline" size="sm" disabled={page + 1 >= pages} onClick={() => onPage(page + 1)}>
          Siguiente
        </Button>
      </div>
    </nav>
  )
}
