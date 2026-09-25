import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it, vi } from 'vitest'

import { routes } from './routes'

vi.mock('@/services/api/systemStatusService', () => ({
  systemStatusService: { getStatus: vi.fn(() => new Promise(() => {})) },
}))

function renderAt(path: string) {
  const router = createMemoryRouter(routes, { initialEntries: [path] })
  render(<RouterProvider router={router} />)
}

describe('routes', () => {
  it('shows the home page inside the main layout', () => {
    renderAt('/')

    expect(screen.getByRole('heading', { name: 'Bienvenido a SIERAP' })).toBeInTheDocument()
    expect(screen.getByText('Universidad Popular del Cesar · Ingeniería de Sistemas')).toBeInTheDocument()
  })

  it('shows the not found page on an unknown path', () => {
    renderAt('/path-that-does-not-exist')

    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/')
  })
})
