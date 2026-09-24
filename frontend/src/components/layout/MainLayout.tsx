import { Link, Outlet } from 'react-router'

/**
 * Estructura común de todas las páginas: encabezado, contenido y pie.
 * En la Etapa 2 se reemplaza por un layout por rol (menú distinto según `UserRole`).
 */
export function MainLayout() {
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
