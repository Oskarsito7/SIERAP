import { useEffect, useMemo, useState, type ReactNode } from 'react'

import { authService } from '@/services/api/authService'
import { onSessionChange } from '@/services/api/httpClient'

import { AuthContext, type AuthContextValue, type AuthState } from './authContext'

/**
 * Mantiene el estado de la sesión. Al montarse intenta recuperarla con la cookie de
 * renovación; después se entera de cada cambio (login, refresh, cambio de contraseña,
 * logout o sesión vencida) a través de `onSessionChange` del cliente HTTP.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<AuthState>({ status: 'loading' })

  useEffect(() => {
    const unsubscribe = onSessionChange((session) => {
      setState(session ? { status: 'authenticated', user: session.user } : { status: 'anonymous' })
    })
    // En desarrollo StrictMode ejecuta este efecto dos veces; el cliente HTTP reúsa
    // el refresh en curso, así que la cookie se presenta una sola vez.
    authService.restoreSession().catch(() => {
      // Sin cookie o sesión vencida: el listener ya dejó el estado en `anonymous`.
    })
    return unsubscribe
  }, [])

  const value = useMemo<AuthContextValue>(
    () => ({
      state,
      user: state.status === 'authenticated' ? state.user : null,
      logout: authService.logout,
    }),
    [state],
  )

  return <AuthContext value={value}>{children}</AuthContext>
}
