import { render, screen } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it, vi } from 'vitest'

import { rutas } from './rutas'

vi.mock('@/services/api/estadoService', () => ({
  estadoService: { consultar: vi.fn(() => new Promise(() => {})) },
}))

function renderizarEn(ruta: string) {
  const router = createMemoryRouter(rutas, { initialEntries: [ruta] })
  render(<RouterProvider router={router} />)
}

describe('rutas', () => {
  it('muestra la página de inicio dentro del layout principal', () => {
    renderizarEn('/')

    expect(screen.getByRole('heading', { name: 'Bienvenido a SIERAP' })).toBeInTheDocument()
    expect(screen.getByText('Universidad Popular del Cesar · Ingeniería de Sistemas')).toBeInTheDocument()
  })

  it('muestra la página de no encontrado en una ruta inexistente', () => {
    renderizarEn('/ruta-que-no-existe')

    expect(screen.getByRole('heading', { name: 'Página no encontrada' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Volver al inicio' })).toHaveAttribute('href', '/')
  })
})
