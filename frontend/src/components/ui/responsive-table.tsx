import { useLayoutEffect, useRef, type ComponentProps } from 'react'

import { cn } from '@/lib/utils'

/**
 * Tabla que se adapta al teléfono: en pantallas anchas es una tabla normal (con
 * desplazamiento horizontal si no cabe) y por debajo de `sm` cada fila se muestra como
 * una tarjeta, con el título de la columna encima de cada valor.
 *
 * Los títulos se copian de `<thead>` a cada celda (`data-label`) después de cada
 * render, así las páginas escriben la tabla como siempre. Las columnas cuyo título
 * solo existe para lectores de pantalla (p. ej. "Acciones") no muestran etiqueta.
 */
export function ResponsiveTable({ className, children, ...props }: ComponentProps<'table'>) {
  const ref = useRef<HTMLTableElement>(null)

  useLayoutEffect(() => {
    const table = ref.current
    if (!table) return
    const labels = Array.from(table.querySelectorAll('thead tr:last-child th')).map(visibleText)
    table.querySelectorAll('tbody tr').forEach((row) => {
      let column = 0
      Array.from(row.children).forEach((cell) => {
        cell.setAttribute('data-label', labels[column] ?? '')
        column += (cell as HTMLTableCellElement).colSpan || 1
      })
    })
  })

  return (
    <div className="relative overflow-x-auto">
      <table ref={ref} className={cn('responsive-table w-full text-left text-sm', className)} {...props}>
        {children}
      </table>
    </div>
  )
}

/** Texto visible de un encabezado, sin lo que es solo para lectores de pantalla. */
function visibleText(header: Element): string {
  const copy = header.cloneNode(true) as Element
  copy.querySelectorAll('.sr-only').forEach((hidden) => hidden.remove())
  return copy.textContent?.trim() ?? ''
}
