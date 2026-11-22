import { useCallback, useRef, useState, type ReactNode } from 'react'

import { ConfirmDialog } from '@/components/ui/confirm-dialog'

/** Texto y estilo de una confirmación. */
export interface ConfirmOptions {
  /** La pregunta, p. ej. "¿Eliminar el periodo 2026-2?". */
  title: string
  /** Qué pasa si se confirma. */
  description: string
  /** Texto del botón que confirma, p. ej. "Eliminar". */
  confirmLabel: string
  /** `true` (por defecto) pinta el botón en rojo: borrar, quitar o desactivar. */
  destructive?: boolean
}

/**
 * Reemplazo de `window.confirm` con el `ConfirmDialog` de la app. Devuelve la función
 * `confirm`, que abre el diálogo y se resuelve con `true` o `false`, y el diálogo, que
 * el componente debe renderizar una vez:
 *
 * ```tsx
 * const [confirm, confirmDialog] = useConfirm()
 * const remove = async () => {
 *   if (!(await confirm({ title: '¿Eliminar…?', description: '…', confirmLabel: 'Eliminar' }))) return
 *   …
 * }
 * return <>{confirmDialog}…</>
 * ```
 */
export function useConfirm(): [(options: ConfirmOptions) => Promise<boolean>, ReactNode] {
  const [options, setOptions] = useState<ConfirmOptions | null>(null)
  // Conserva el último texto para que el diálogo no quede vacío mientras se cierra.
  const [shown, setShown] = useState<ConfirmOptions | null>(null)
  const resolver = useRef<((answer: boolean) => void) | null>(null)

  const confirm = useCallback((next: ConfirmOptions) => {
    // Si había una pregunta pendiente, se toma como cancelada.
    resolver.current?.(false)
    setOptions(next)
    setShown(next)
    return new Promise<boolean>((resolve) => {
      resolver.current = resolve
    })
  }, [])

  const answer = (value: boolean) => {
    resolver.current?.(value)
    resolver.current = null
    setOptions(null)
  }

  const dialog = shown && (
    <ConfirmDialog
      open={options !== null}
      title={shown.title}
      description={shown.description}
      confirmLabel={shown.confirmLabel}
      destructive={shown.destructive ?? true}
      onConfirm={() => answer(true)}
      onCancel={() => answer(false)}
    />
  )
  return [confirm, dialog]
}
