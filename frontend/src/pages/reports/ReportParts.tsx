import type { ReactNode } from 'react'

import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { formatDateTime } from '@/lib/dates'
import type { ReportMoment, ReportPeriod, ReportProgram } from '@/types/report'

/**
 * Encabezado de un reporte: de qué momento es, si las notas ya son definitivas y
 * cuándo se generó.
 */
export function ReportMeta({
  program,
  moment,
  period,
  final,
  generatedAt,
}: {
  program: ReportProgram
  moment: ReportMoment
  period: ReportPeriod
  final: boolean
  generatedAt: string
}) {
  return (
    <div className="flex flex-col gap-1">
      <div className="flex flex-wrap items-center gap-2 text-sm">
        <span className="font-medium">
          {moment.name} · {moment.subject}
        </span>
        <span className="text-muted-foreground">
          {program.name} · {period.code}
        </span>
        <Badge variant={final ? 'default' : 'secondary'}>{final ? 'Notas definitivas' : 'Vista previa'}</Badge>
      </div>
      <p className="text-muted-foreground text-xs">
        {final ? '' : 'La calificación sigue abierta: las cifras pueden cambiar hasta que cierre. '}
        Generado el {formatDateTime(generatedAt)}.
      </p>
    </div>
  )
}

/** Tarjeta con el título de una gráfica o tabla del reporte. */
export function ReportCard({
  title,
  description,
  children,
}: {
  title: string
  description?: string
  children: ReactNode
}) {
  return (
    <Card className="min-w-0">
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  )
}

/** Tabla simple de un reporte: encabezados y filas de texto, con las cifras a la derecha. */
export function ReportTable({
  label,
  columns,
  rows,
}: {
  label: string
  /** `numeric` alinea la columna a la derecha. */
  columns: { header: string; numeric?: boolean }[]
  rows: { key: string | number; cells: ReactNode[] }[]
}) {
  return (
    <div className="overflow-x-auto">
      <table aria-label={label} className="w-full text-left text-sm">
        <thead className="text-muted-foreground border-b">
          <tr>
            {columns.map((column) => (
              <th key={column.header} className={`py-2 pr-4 font-medium ${column.numeric ? 'text-right' : ''}`}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.key} className="border-b last:border-0">
              {row.cells.map((cell, index) => (
                <td
                  key={columns[index].header}
                  className={`py-2 pr-4 ${columns[index].numeric ? 'text-right tabular-nums' : ''}`}
                >
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
