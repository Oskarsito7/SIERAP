import { useState } from 'react'
import { LogOut, Menu, UserRound, X } from 'lucide-react'
import { Link, NavLink, Outlet, useLocation } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { CHANGE_PASSWORD_PATH } from '@/routes/guards'
import type { UserSummary } from '@/types/auth'

import { ACCOUNT_PATH, ROLE_LABELS, ROLE_MENUS, type MenuItem } from './roleMenus'

/**
 * Layout de las páginas con sesión: menú lateral según el rol del usuario (aprobado en
 * `docs/06`), barra superior con el título de la sección y el usuario, y el contenido.
 * En pantallas pequeñas el menú se abre con un botón.
 *
 * Mientras la contraseña sea temporal no hay menú: la única pantalla disponible es
 * "Cambiar contraseña", que trae su propio botón de cerrar sesión.
 */
export function AppLayout() {
  const { user, logout } = useAuth()
  const location = useLocation()
  const [menuOpen, setMenuOpen] = useState(false)

  if (!user) {
    return null
  }

  if (user.mustChangePassword) {
    return (
      <div className="flex min-h-svh flex-col">
        <header className="flex h-16 items-center gap-3 border-b px-4 md:px-8">
          <span className="text-lg font-bold tracking-tight">SIERAP</span>
          <UserBadge user={user} />
        </header>
        <main className="w-full flex-1 px-4 py-8">
          <Outlet />
        </main>
      </div>
    )
  }

  const items = ROLE_MENUS[user.role]
  const title = sectionTitle(items, location.pathname)
  const closeMenu = () => setMenuOpen(false)

  return (
    <div className="flex min-h-svh">
      {menuOpen && (
        <div className="fixed inset-0 z-30 bg-black/40 md:hidden" onClick={closeMenu} aria-hidden />
      )}
      <aside
        className={cn(
          'bg-sidebar text-sidebar-foreground border-sidebar-border fixed inset-y-0 left-0 z-40 flex w-64 flex-col border-r transition-transform md:sticky md:top-0 md:h-svh md:translate-x-0',
          menuOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex items-center justify-between px-5 py-4">
          <Link to="/" className="flex flex-col" onClick={closeMenu}>
            <span className="text-lg font-bold tracking-tight">SIERAP</span>
            <span className="text-muted-foreground text-xs">Universidad Popular del Cesar</span>
          </Link>
          <Button variant="ghost" size="icon" className="md:hidden" onClick={closeMenu} aria-label="Cerrar menú">
            <X aria-hidden />
          </Button>
        </div>

        <nav aria-label="Menú principal" className="flex flex-1 flex-col gap-1 overflow-y-auto px-3">
          {items.map((item) => (
            <MenuLink key={item.path} to={item.path} icon={item.icon} onClick={closeMenu}>
              {item.label}
            </MenuLink>
          ))}
        </nav>

        <div className="border-sidebar-border flex flex-col gap-1 border-t px-3 py-3">
          <MenuLink to={ACCOUNT_PATH} icon={UserRound} onClick={closeMenu}>
            Mi cuenta
          </MenuLink>
          <button
            type="button"
            onClick={() => void logout()}
            className="hover:bg-sidebar-accent flex items-center gap-3 rounded-md px-3 py-2 text-left text-sm"
          >
            <LogOut className="size-4" aria-hidden />
            Cerrar sesión
          </button>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="flex h-16 items-center gap-3 border-b px-4 md:px-8">
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            onClick={() => setMenuOpen(true)}
            aria-label="Abrir menú"
            aria-expanded={menuOpen}
          >
            <Menu aria-hidden />
          </Button>
          <p className="truncate text-lg font-semibold">{title}</p>
          <UserBadge user={user} />
        </header>

        <main className="w-full max-w-6xl flex-1 px-4 py-8 md:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}

/** Opción del menú lateral, resaltada cuando su página (o una subpágina) está abierta. */
function MenuLink({
  to,
  icon: Icon,
  onClick,
  children,
}: {
  to: string
  icon: MenuItem['icon']
  onClick: () => void
  children: string
}) {
  return (
    <NavLink
      to={to}
      onClick={onClick}
      className={({ isActive }) =>
        cn(
          'hover:bg-sidebar-accent flex items-center gap-3 rounded-md px-3 py-2 text-sm',
          isActive && 'bg-sidebar-accent text-sidebar-accent-foreground font-medium',
        )
      }
    >
      <Icon className="size-4" aria-hidden />
      {children}
    </NavLink>
  )
}

/** Nombre, rol e iniciales del usuario en la barra superior. */
function UserBadge({ user }: { user: UserSummary }) {
  const initials = (user.firstNames.charAt(0) + user.lastNames.charAt(0)).toUpperCase()
  return (
    <div className="ml-auto flex items-center gap-3">
      <div className="hidden text-right sm:block">
        <p className="text-sm font-medium">{user.fullName}</p>
        <p className="text-muted-foreground text-xs">{ROLE_LABELS[user.role]}</p>
      </div>
      <div
        className="bg-muted grid size-9 shrink-0 place-items-center rounded-full text-sm font-semibold"
        aria-hidden
      >
        {initials}
      </div>
    </div>
  )
}

/** Título de la barra superior: la opción del menú de la página abierta. */
function sectionTitle(items: MenuItem[], pathname: string): string {
  if (pathname === ACCOUNT_PATH) return 'Mi cuenta'
  if (pathname === CHANGE_PASSWORD_PATH) return 'Cambiar contraseña'
  const item = items.find((candidate) => pathname === candidate.path || pathname.startsWith(`${candidate.path}/`))
  return item?.label ?? 'Inicio'
}
