import { render, screen } from '@testing-library/react'
import { ChartColumn } from 'lucide-react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import type { MenuPage } from '@/components/layout/roleMenus'
import type { UserRole, UserSummary } from '@/types/auth'

import { MenuPageRoute } from './MenuPageRoute'

/** Página compartida por dos roles, cada uno con su texto. */
const sharedPage: MenuPage = {
  path: '/shared',
  roles: ['ADMIN', 'TEACHER'],
  items: {
    ADMIN: { path: '/shared', label: 'Compartida', icon: ChartColumn, description: 'Lo de la facultad', stage: 8 },
    TEACHER: { path: '/shared', label: 'Compartida', icon: ChartColumn, description: 'Lo de tus grupos', stage: 8 },
  },
}

function renderAs(role: UserRole) {
  const user: UserSummary = {
    id: 1,
    firstNames: 'Marta',
    lastNames: 'Díaz',
    fullName: 'Marta Díaz',
    email: 'marta@unicesar.edu.co',
    role,
    active: true,
    mustChangePassword: false,
  }
  render(
    <AuthContext value={{ state: { status: 'authenticated', user }, user, logout: vi.fn() }}>
      <MemoryRouter>
        <MenuPageRoute page={sharedPage} />
      </MemoryRouter>
    </AuthContext>,
  )
}

describe('MenuPageRoute', () => {
  it('uses the text of the role when two roles share a page', () => {
    renderAs('TEACHER')

    expect(screen.getByRole('heading', { name: 'Próximamente' })).toBeInTheDocument()
    expect(screen.getByText('Lo de tus grupos llega en la Etapa 8.')).toBeInTheDocument()
  })

  it('does not open the page for another role', () => {
    renderAs('STUDENT')

    expect(screen.getByRole('heading', { name: 'Sin acceso' })).toBeInTheDocument()
  })
})
