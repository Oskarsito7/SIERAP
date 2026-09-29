import { ShieldX } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { Button } from '@/components/ui/button'
import type { UserRole } from '@/types/auth'

/**
 * Guarda de las páginas de un rol. Va dentro de `RequireAuth`, así que siempre hay
 * sesión. Si el rol no puede abrir la página muestra un aviso en lugar de su
 * contenido; el backend responde 403 de todas formas, esto solo evita la pantalla
 * rota.
 */
export function RequireRole({ roles, children }: { roles: UserRole[]; children: ReactNode }) {
  const { user } = useAuth()

  if (user && roles.includes(user.role)) {
    return children
  }
  return (
    <div className="flex flex-col items-start gap-4">
      <ShieldX className="text-muted-foreground size-10" aria-hidden />
      <h1 className="text-2xl font-bold tracking-tight">Sin acceso</h1>
      <p className="text-muted-foreground">Esta sección no está disponible para tu rol.</p>
      <Button asChild>
        <Link to="/">Volver al inicio</Link>
      </Button>
    </div>
  )
}
