import { Button } from '@/components/ui/button'

interface PaginationBarProps {
  /** Página actual, desde 0. */
  page: number
  size: number
  /** Total de elementos sin paginar. */
  total: number
  onPageChange: (page: number) => void
}

/**
 * Pie de las listas con paginación del servidor: "Mostrando 1–50 de 320" y los botones
 * Anterior / Siguiente (solo si hay más de una página). No muestra nada si no hay elementos.
 */
export function PaginationBar({ page, size, total, onPageChange }: PaginationBarProps) {
  if (total === 0) return null
  const from = page * size + 1
  const to = Math.min((page + 1) * size, total)
  const lastPage = Math.ceil(total / size) - 1
  return (
    <nav aria-label="Paginación" className="flex flex-wrap items-center justify-between gap-2 text-sm">
      <p aria-live="polite" className="text-muted-foreground">
        Mostrando {from}–{to} de {total}
      </p>
      {total > size && (
        <div className="flex gap-2">
          <Button variant="outline" size="sm" disabled={page <= 0} onClick={() => onPageChange(page - 1)}>
            Anterior
          </Button>
          <Button variant="outline" size="sm" disabled={page >= lastPage} onClick={() => onPageChange(page + 1)}>
            Siguiente
          </Button>
        </div>
      )}
    </nav>
  )
}
