import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { systemStatusService } from '@/services/api/systemStatusService'

import { HomePage } from './HomePage'

vi.mock('@/services/api/systemStatusService', () => ({
  systemStatusService: { getStatus: vi.fn() },
}))

const getStatus = vi.mocked(systemStatusService.getStatus)

describe('HomePage', () => {
  beforeEach(() => {
    getStatus.mockReset()
  })

  it('shows the backend status when the API responds', async () => {
    getStatus.mockResolvedValue({
      application: 'sierap',
      version: '0.1.0',
      profiles: ['dev'],
      databaseAvailable: true,
      timestamp: '2026-03-15T09:30:00',
    })

    render(<HomePage />)

    expect(await screen.findByText('API conectada')).toBeInTheDocument()
    expect(screen.getByText('Base de datos disponible')).toBeInTheDocument()
    expect(screen.getByText('0.1.0')).toBeInTheDocument()
    expect(screen.getByText('dev')).toBeInTheDocument()
  })

  it('shows the error and allows retrying when the API does not respond', async () => {
    getStatus.mockRejectedValueOnce(new Error('Network Error'))
    getStatus.mockResolvedValueOnce({
      application: 'sierap',
      version: '0.1.0',
      profiles: ['dev'],
      databaseAvailable: false,
      timestamp: '2026-03-15T09:30:00',
    })

    render(<HomePage />)

    expect(await screen.findByText('Sin conexión con la API')).toBeInTheDocument()
    expect(screen.getByText('No fue posible comunicarse con el servidor')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: /reintentar/i }))

    expect(await screen.findByText('Base de datos no disponible')).toBeInTheDocument()
    expect(getStatus).toHaveBeenCalledTimes(2)
  })
})
