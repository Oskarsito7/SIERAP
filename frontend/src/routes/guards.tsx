import { LoaderCircle } from 'lucide-react'
import { Navigate, Outlet, useLocation } from 'react-router'

import { useAuth } from '@/auth/authContext'

/** Ruta de la pantalla obligatoria mientras la contraseña sea temporal. */
export const CHANGE_PASSWORD_PATH = '/change-password'

/** Estado de navegación con la ruta a la que se quería ir antes del login. */
export interface RedirectState {
  from?: string
}

function SessionLoading() {
  return (
    <p className="text-muted-foreground flex items-center gap-2 text-sm">
      <LoaderCircle className="size-4 animate-spin" aria-hidden />
      Cargando la sesión…
    </p>
  )
}

/**
 * Guarda de las rutas que exigen sesión. Sin sesión lleva al login (recordando a
 * dónde se quería ir); con contraseña temporal solo deja ver "Cambiar contraseña".
 */
export function RequireAuth() {
  const { state } = useAuth()
  const location = useLocation()

  if (state.status === 'loading') {
    return <SessionLoading />
  }
  if (state.status === 'anonymous') {
    const redirect: RedirectState = { from: location.pathname + location.search }
    return <Navigate to="/login" replace state={redirect} />
  }
  const onChangePassword = location.pathname === CHANGE_PASSWORD_PATH
  if (state.user.mustChangePassword && !onChangePassword) {
    return <Navigate to={CHANGE_PASSWORD_PATH} replace />
  }
  return <Outlet />
}

/** Guarda de las rutas solo para quien no ha iniciado sesión (login y registro). */
export function GuestOnly() {
  const { state } = useAuth()

  if (state.status === 'loading') {
    return <SessionLoading />
  }
  if (state.status === 'authenticated') {
    return <Navigate to="/" replace />
  }
  return <Outlet />
}
