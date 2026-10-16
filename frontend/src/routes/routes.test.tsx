import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it, vi } from 'vitest'

import { AuthContext, type AuthState } from '@/auth/authContext'
import type { UserSummary } from '@/types/auth'

import { routes } from './routes'

vi.mock('@/services/api/systemStatusService', () => ({
  systemStatusService: { getStatus: vi.fn(() => new Promise(() => {})) },
}))

const user: UserSummary = {
  id: 1,
  firstNames: 'Administrador',
  lastNames: 'SIERAP',
  fullName: 'Administrador SIERAP',
  email: 'sierap@unicesar.edu.co',
  role: 'SUPER_ADMIN',
  active: true,
  mustChangePassword: false,
}

function renderAt(path: string, state: AuthState) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  const value = {
    state,
    user: state.status === 'authenticated' ? state.user : null,
    logout: vi.fn(),
  }
  render(
    <AuthContext value={value}>
      <RouterProvider router={router} />
    </AuthContext>,
  )
  return router
}

describe('routes', () => {
  it('shows the home page inside the role layout when there is a session', () => {
    renderAt('/', { status: 'authenticated', user })

    expect(screen.getByRole('heading', { name: 'Bienvenido a SIERAP' })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Menú principal' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /cerrar sesión/i })).toBeInTheDocument()
  })

  it('waits while the session is being restored', () => {
    renderAt('/', { status: 'loading' })

    expect(screen.getByText('Cargando la sesión…')).toBeInTheDocument()
  })

  it('sends to the login page when there is no session', () => {
    const router = renderAt('/', { status: 'anonymous' })

    expect(screen.getByRole('heading', { name: 'Iniciar sesión' })).toBeInTheDocument()
    expect(router.state.location.state).toEqual({ from: '/' })
  })

  it('only allows the change password page while the password is temporary', () => {
    const router = renderAt('/', { status: 'authenticated', user: { ...user, mustChangePassword: true } })

    expect(router.state.location.pathname).toBe('/change-password')
    expect(screen.getByRole('heading', { name: 'Cambiar contraseña' })).toBeInTheDocument()
    expect(screen.getByText('Tu contraseña es temporal. Debes cambiarla antes de continuar.')).toBeInTheDocument()
  })

  it('takes a signed-in user away from the login page', () => {
    const router = renderAt('/login', { status: 'authenticated', user })

    expect(router.state.location.pathname).toBe('/')
  })

  it('opens the email link page without a session', () => {
    renderAt('/set-password?token=abc', { status: 'anonymous' })

    expect(screen.getByRole('heading', { name: 'Crear contraseña' })).toBeInTheDocument()
  })

  it('shows the not found page on an unknown path', () => {
    renderAt('/path-that-does-not-exist', { status: 'anonymous' })

    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/')
  })
})
