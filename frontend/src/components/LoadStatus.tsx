import { LoaderCircle, RefreshCw } from 'lucide-react'

import { FormError } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import type { LoadState } from '@/lib/useLoad'

/**
 * Muestra "cargando" o el error con el botón de reintentar de una carga; no muestra
 * nada cuando ya hay datos.
 */
export function LoadStatus<T>({ state, onRetry, label }: { state: LoadState<T>; onRetry: () => void; label: string }) {
  if (state.kind === 'loading') {
    return (
      <p className="text-muted-foreground flex items-center gap-2 text-sm">
        <LoaderCircle className="size-4 animate-spin" aria-hidden />
        {label}
      </p>
    )
  }
  if (state.kind === 'error') {
    return (
      <div className="flex flex-col items-start gap-3">
        <FormError message={state.message} />
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RefreshCw aria-hidden />
          Reintentar
        </Button>
      </div>
    )
  }
  return null
}
