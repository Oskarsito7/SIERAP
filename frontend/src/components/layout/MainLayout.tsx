import { LogOut } from 'lucide-react'
import { Link, Outlet } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { Button } from '@/components/ui/button'

import { UpcLogo } from './UpcLogo'

/**
 * Estructura de las páginas públicas (login, registro y enlaces del correo):
 * encabezado, contenido y pie. Los enlaces del correo se pueden abrir con sesión, por
 * eso el encabezado muestra al usuario y el botón de cerrar sesión cuando la hay. Las
 * páginas con sesión usan `AppLayout`, con el menú de cada rol.
 *
 * El encabezado usa el logosímbolo horizontal con el nombre del sistema a la derecha,
 * separado por una línea, como pide el manual para las dependencias (ADR-019).
 */
export function MainLayout() {
  const { user, logout } = useAuth()

  return (
    <div className="flex min-h-svh flex-col">
      <header className="bg-card border-b">
        <div className="mx-auto flex h-20 max-w-5xl items-center gap-4 px-4">
          <Link to="/" className="flex items-center gap-4" aria-label="SIERAP, inicio">
            <UpcLogo className="h-11" />
            <span className="border-upc-green-700 flex flex-col border-l pl-4">
              <span className="font-display text-upc-green-800 text-lg leading-tight font-bold">SIERAP</span>
              <span className="text-muted-foreground hidden text-xs leading-snug sm:block">
                Sistema Integral Evaluador de Resultados de Aprendizajes por Programa
              </span>
            </span>
          </Link>
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

      <footer className="bg-upc-green-800 py-5 text-center text-xs text-white">
        <p className="font-semibold">Universidad Popular del Cesar · Educación y Futuro</p>
        <p className="mt-1 text-white/80">Programa de Ingeniería de Sistemas · Valledupar, Cesar</p>
      </footer>
    </div>
  )
}
