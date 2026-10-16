import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { routes } from '@/routes/routes'
import type { UserRole, UserSummary } from '@/types/auth'

vi.mock('@/services/api/systemStatusService', () => ({
  systemStatusService: { getStatus: vi.fn(() => new Promise(() => {})) },
}))

function userWith(role: UserRole, overrides: Partial<UserSummary> = {}): UserSummary {
  return {
    id: 1,
    firstNames: 'Marta',
    lastNames: 'Díaz',
    fullName: 'Marta Díaz',
    email: 'marta@unicesar.edu.co',
    role,
    active: true,
    mustChangePassword: false,
    ...overrides,
  }
}

function renderAt(path: string, user: UserSummary, logout = vi.fn()) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(
    <AuthContext value={{ state: { status: 'authenticated', user }, user, logout }}>
      <RouterProvider router={router} />
    </AuthContext>,
  )
  return router
}

function menuLabels(): string[] {
  const menu = screen.getByRole('navigation', { name: 'Menú principal' })
  return within(menu)
    .getAllByRole('link')
    .map((link) => link.textContent ?? '')
}

describe('AppLayout', () => {
  it.each<[UserRole, string[]]>([
    [
      'SUPER_ADMIN',
      ['Facultades y programas', 'Periodos académicos', 'Parámetros globales', 'Tipos de evaluador', 'Administradores'],
    ],
    ['ADMIN', ['Momentos', 'Docentes', 'Códigos de registro', 'Reportes']],
    ['TEACHER', ['Mis grupos', 'Mis estudiantes', 'Evaluadores externos', 'Códigos de registro', 'Reportes']],
    ['STUDENT', ['Grupos del momento', 'Mi grupo y entregas', 'Mis notas']],
    ['EXTERNAL_EVALUATOR', ['Grupos asignados']],
  ])('shows the approved menu of %s', (role, expected) => {
    renderAt('/', userWith(role))

    expect(menuLabels()).toEqual(expected)
    expect(screen.getByRole('link', { name: 'Mi cuenta' })).toHaveAttribute('href', '/account')
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument()
  })

  it('shows the user name and role in the top bar', () => {
    renderAt('/', userWith('TEACHER'))

    expect(screen.getByText('Marta Díaz')).toBeInTheDocument()
    expect(screen.getByText('Docente')).toBeInTheDocument()
  })

  it('opens a menu option without a page as coming soon, with its stage', async () => {
    renderAt('/', userWith('TEACHER'))

    await userEvent.click(screen.getByRole('link', { name: 'Mis estudiantes' }))

    expect(screen.getByRole('heading', { name: 'Próximamente' })).toBeInTheDocument()
    expect(screen.getByText('La lista de tus estudiantes llega en la Etapa 2.')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Mis estudiantes' })).toHaveAttribute('aria-current', 'page')
  })

  it('uses the text of the role when two roles share a page', () => {
    renderAt('/reports', userWith('TEACHER'))

    expect(screen.getByText('Los reportes de tus grupos llega en la Etapa 7.')).toBeInTheDocument()
  })

  it('does not open a page of another role', () => {
    renderAt('/faculties', userWith('STUDENT'))

    expect(screen.getByRole('heading', { name: 'Sin acceso' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Próximamente' })).not.toBeInTheDocument()
  })

  it('hides the menu while the password is temporary', () => {
    const router = renderAt('/groups', userWith('TEACHER', { mustChangePassword: true }))

    expect(router.state.location.pathname).toBe('/change-password')
    expect(screen.queryByRole('navigation', { name: 'Menú principal' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Mi cuenta' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cerrar sesión' })).toBeInTheDocument()
  })

  it('shows the account data and the change password form', () => {
    renderAt('/account', userWith('STUDENT'))

    expect(screen.getByRole('heading', { name: 'Mi cuenta' })).toBeInTheDocument()
    const data = screen.getByText('Mis datos').closest('[data-slot="card"]') as HTMLElement
    expect(within(data).getByText('marta@unicesar.edu.co')).toBeInTheDocument()
    expect(within(data).getByText('Estudiante')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Cambiar contraseña' })).toBeInTheDocument()
  })

  it('logs out from the menu', async () => {
    const logout = vi.fn(() => Promise.resolve())
    renderAt('/', userWith('ADMIN'), logout)

    await userEvent.click(screen.getByRole('button', { name: 'Cerrar sesión' }))

    expect(logout).toHaveBeenCalledOnce()
  })
})
