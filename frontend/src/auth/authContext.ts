import { createContext, useContext } from 'react'

import type { UserSummary } from '@/types/auth'

/**
 * Estado de la sesión: `loading` mientras se intenta recuperarla al cargar la app,
 * `authenticated` con el usuario, o `anonymous` si no hay sesión.
 */
export type AuthState =
  | { status: 'loading' }
  | { status: 'authenticated'; user: UserSummary }
  | { status: 'anonymous' }

/** Valor del contexto de autenticación. */
export interface AuthContextValue {
  state: AuthState
  /** Usuario de la sesión, o `null` si no hay sesión. */
  user: UserSummary | null
  logout: () => Promise<void>
}

export const AuthContext = createContext<AuthContextValue | null>(null)

/** Acceso al estado de la sesión desde cualquier componente bajo el `AuthProvider`. */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth debe usarse dentro de un AuthProvider')
  }
  return context
}
