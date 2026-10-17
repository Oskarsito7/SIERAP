import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { catalogService } from '@/services/api/catalogService'

import { ParametersPage } from './ParametersPage'

vi.mock('@/services/api/catalogService', () => ({ catalogService: { parameters: vi.fn(), saveParameter: vi.fn() } }))

const service = vi.mocked(catalogService)

describe('ParametersPage', () => {
  beforeEach(() => {
    service.parameters.mockReset()
    service.saveParameter.mockReset()
  })

  it('explains that there are no parameters yet and creates one', async () => {
    service.parameters.mockResolvedValue([])
    service.saveParameter.mockResolvedValue({ key: 'institution_name', value: 'Universidad Popular del Cesar',
      description: null })
    render(<ParametersPage />)

    expect(await screen.findByText(/ninguna regla del sistema los usa/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Nuevo parámetro' }))
    const form = screen.getByRole('form', { name: 'Nuevo parámetro' })
    await userEvent.type(within(form).getByLabelText('Clave'), 'institution_name')
    await userEvent.type(within(form).getByLabelText('Valor'), 'Universidad Popular del Cesar')
    await userEvent.click(within(form).getByRole('button', { name: 'Crear parámetro' }))

    expect(service.saveParameter).toHaveBeenCalledWith('institution_name', 'Universidad Popular del Cesar', null)
    expect(await screen.findByRole('row', { name: /institution_name/ })).toBeInTheDocument()
  })

  it('edits the value of a parameter without changing its key', async () => {
    service.parameters.mockResolvedValue([{ key: 'zip_max_mb', value: '100', description: 'Límite del .zip' }])
    service.saveParameter.mockResolvedValue({ key: 'zip_max_mb', value: '120', description: 'Límite del .zip' })
    render(<ParametersPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Editar zip_max_mb' }))
    const form = screen.getByRole('form', { name: 'Editar zip_max_mb' })
    expect(within(form).getByLabelText('Clave')).toBeDisabled()
    await userEvent.clear(within(form).getByLabelText('Valor'))
    await userEvent.type(within(form).getByLabelText('Valor'), '120')
    await userEvent.click(within(form).getByRole('button', { name: 'Guardar' }))

    expect(service.saveParameter).toHaveBeenCalledWith('zip_max_mb', '120', 'Límite del .zip')
    expect(await screen.findByText('120')).toBeInTheDocument()
  })
})
