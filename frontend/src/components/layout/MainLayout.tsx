import { LogOut } from 'lucide-react'
import { Link, Outlet } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { Button } from '@/components/ui/button'

/**
 * Estructura de las páginas públicas (login, registro y enlaces del correo):
 * encabezado, contenido y pie. Los enlaces del correo se pueden abrir con sesión, por
 * eso el encabezado muestra al usuario y el botón de cerrar sesión cuando la hay. Las
 * páginas con sesión usan `AppLayout`, con el menú de cada rol.
 */
export function MainLayout() {
  const { user, logout } = useAuth()

  return (
    <div className="flex min-h-svh flex-col">
      <header className="border-b">
        <div className="mx-auto flex h-14 max-w-5xl items-center gap-3 px-4">
          <Link to="/" className="text-lg font-bold tracking-tight">
            SIERAP
          </Link>
          <span className="text-muted-foreground hidden text-sm sm:inline">
            Sistema Integral Evaluador de Resultados de Aprendizajes por Programa
          </span>
          {user && (
            <div className="ml-auto flex items-center gap-3">
              <span className="hidden text-sm md:inline">{user.fullName}</span>
              <Button variant="outline" size="sm" onClick={() => void logout()}>
                <LogOut aria-hidden />
                Cerrar sesión
              </Button>
            </div>
          )}
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 py-8">
        <Outlet />
      </main>

      <footer className="text-muted-foreground border-t py-4 text-center text-xs">
        Universidad Popular del Cesar · Ingeniería de Sistemas
      </footer>
    </div>
  )
}
