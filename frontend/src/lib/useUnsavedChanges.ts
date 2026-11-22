import { useEffect } from 'react'
import { useBlocker, type Blocker } from 'react-router'

/**
 * Protege un formulario con cambios sin guardar:
 *
 * - al cerrar o recargar la pestaña, el navegador muestra su aviso estándar
 *   (`beforeunload`; el texto lo pone el navegador, no la app);
 * - al ir a otra página de la app, la navegación queda en pausa y el `Blocker`
 *   devuelto pasa a `state === 'blocked'` para que la página pregunte con un
 *   `ConfirmDialog` y luego llame a `proceed()` o `reset()`.
 *
 * Requiere un router de datos (`createBrowserRouter` / `createMemoryRouter`).
 *
 * @param dirty `true` mientras haya cambios que se perderían
 */
export function useUnsavedChanges(dirty: boolean): Blocker {
  useEffect(() => {
    if (!dirty) return
    const warn = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      // Navegadores antiguos solo muestran el aviso si se asigna `returnValue`.
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', warn)
    return () => window.removeEventListener('beforeunload', warn)
  }, [dirty])

  return useBlocker(
    ({ currentLocation, nextLocation }) => dirty && currentLocation.pathname !== nextLocation.pathname,
  )
}
