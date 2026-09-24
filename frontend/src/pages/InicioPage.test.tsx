import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { estadoService } from '@/services/api/estadoService'

import { InicioPage } from './InicioPage'

vi.mock('@/services/api/estadoService', () => ({
  estadoService: { consultar: vi.fn() },
}))

const consultar = vi.mocked(estadoService.consultar)

describe('InicioPage', () => {
  beforeEach(() => {
    consultar.mockReset()
  })

  it('muestra el estado del backend cuando la API responde', async () => {
    consultar.mockResolvedValue({
      aplicacion: 'sierap',
      version: '0.1.0',
      perfiles: ['dev'],
      baseDatosDisponible: true,
      fechaHora: '2026-03-15T09:30:00',
    })

    render(<InicioPage />)

    expect(await screen.findByText('API conectada')).toBeInTheDocument()
    expect(screen.getByText('Base de datos disponible')).toBeInTheDocument()
    expect(screen.getByText('0.1.0')).toBeInTheDocument()
    expect(screen.getByText('dev')).toBeInTheDocument()
  })

  it('muestra el error y permite reintentar cuando la API no responde', async () => {
    consultar.mockRejectedValueOnce(new Error('Network Error'))
    consultar.mockResolvedValueOnce({
      aplicacion: 'sierap',
      version: '0.1.0',
      perfiles: ['dev'],
      baseDatosDisponible: false,
      fechaHora: '2026-03-15T09:30:00',
    })

    render(<InicioPage />)

    expect(await screen.findByText('Sin conexión con la API')).toBeInTheDocument()
    expect(screen.getByText('No fue posible comunicarse con el servidor')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /reintentar/i }))

    expect(await screen.findByText('Base de datos no disponible')).toBeInTheDocument()
    expect(consultar).toHaveBeenCalledTimes(2)
  })
})
