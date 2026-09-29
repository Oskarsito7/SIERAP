import { useAuth } from '@/auth/authContext'
import type { MenuPage } from '@/components/layout/roleMenus'
import { ComingSoonPage } from '@/pages/ComingSoonPage'

import { RequireRole } from './RequireRole'

/**
 * Página de una opción del menú, protegida por rol. Mientras la página real no exista
 * muestra "Próximamente" con el texto de la opción del rol del usuario. Cuando llegue
 * una página, se agrega a `MENU_PAGES` en `routes.tsx` y deja de pasar por aquí.
 */
export function MenuPageRoute({ page }: { page: MenuPage }) {
  const { user } = useAuth()
  const item = user ? page.items[user.role] : undefined

  return (
    <RequireRole roles={page.roles}>
      {item && <ComingSoonPage description={item.description} stage={item.stage} />}
    </RequireRole>
  )
}
