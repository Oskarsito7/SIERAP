import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { catalogService } from '@/services/api/catalogService'

import { periodFixture } from '../moments/testData'
import { PeriodsPage } from './PeriodsPage'
import { apiError } from './testHelpers'

vi.mock('@/services/api/catalogService', () => ({
  catalogService: { periods: vi.fn(), createPeriod: vi.fn(), updatePeriod: vi.fn(), deletePeriod: vi.fn() },
}))

const service = vi.mocked(catalogService)

describe('PeriodsPage', () => {
  beforeEach(() => {
    Object.values(service).forEach((mock) => mock.mockReset())
    service.periods.mockResolvedValue([
      periodFixture(),
      periodFixture({ id: 2, code: '2026-1', startDate: '2026-02-02', endDate: '2026-06-10',
        upcEvaluationDeadline: '2026-06-01', status: 'CLOSED' }),
    ])
  })
  afterEach(() => vi.restoreAllMocks())

  it('lists the periods with their UPC deadline and state', async () => {
    render(<PeriodsPage />)

    const current = await screen.findByRole('row', { name: /2026-2/ })
    expect(within(current).getByText('28/11/2026')).toBeInTheDocument()
    expect(within(current).getByText('Actual')).toBeInTheDocument()
    expect(within(screen.getByRole('row', { name: /2026-1/ })).getByText('Cerrado')).toBeInTheDocument()
  })

  it('creates a period, checking that the UPC deadline is inside it', async () => {
    service.createPeriod.mockResolvedValue(periodFixture({ id: 3, code: '2027-1', startDate: '2027-02-01',
      endDate: '2027-06-05', upcEvaluationDeadline: '2027-06-01', status: 'UPCOMING' }))
    render(<PeriodsPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo periodo' }))
    const form = screen.getByRole('form', { name: 'Nuevo periodo' })
    await userEvent.type(within(form).getByLabelText('Código'), '2027-1')
    await userEvent.type(within(form).getByLabelText('Inicio'), '2027-02-01')
    await userEvent.type(within(form).getByLabelText('Fin'), '2027-06-05')
    await userEvent.type(within(form).getByLabelText('Límite de evaluación de la UPC'), '2027-06-12')
    await userEvent.click(within(form).getByRole('button', { name: 'Crear periodo' }))
    expect(within(form).getByText('El límite de evaluación de la UPC debe estar dentro del periodo'))
      .toBeInTheDocument()
    expect(service.createPeriod).not.toHaveBeenCalled()

    await userEvent.clear(within(form).getByLabelText('Límite de evaluación de la UPC'))
    await userEvent.type(within(form).getByLabelText('Límite de evaluación de la UPC'), '2027-06-01')
    await userEvent.click(within(form).getByRole('button', { name: 'Crear periodo' }))

    expect(service.createPeriod).toHaveBeenCalledWith({ code: '2027-1', startDate: '2027-02-01',
      endDate: '2027-06-05', upcEvaluationDeadline: '2027-06-01' })
    expect(await screen.findByText('Próximo')).toBeInTheDocument()
  })

  it('edits a period sending its four fields and explains why it cannot be deleted', async () => {
    service.updatePeriod.mockResolvedValue(periodFixture({ upcEvaluationDeadline: '2026-11-30' }))
    service.deletePeriod.mockRejectedValue(apiError(409, 'El periodo tiene momentos.'))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<PeriodsPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Editar 2026-2' }))
    const form = screen.getByRole('form', { name: 'Editar 2026-2' })
    await userEvent.clear(within(form).getByLabelText('Límite de evaluación de la UPC'))
    await userEvent.type(within(form).getByLabelText('Límite de evaluación de la UPC'), '2026-11-30')
    await userEvent.click(within(form).getByRole('button', { name: 'Guardar' }))
    expect(service.updatePeriod).toHaveBeenCalledWith(1, expect.objectContaining({
      code: '2026-2', startDate: '2026-08-03', endDate: '2026-12-05', upcEvaluationDeadline: '2026-11-30',
    }))

    await userEvent.click(await screen.findByRole('button', { name: 'Eliminar 2026-2' }))
    expect(await screen.findByRole('alert'))
      .toHaveTextContent('No se puede eliminar 2026-2: El periodo tiene momentos.')
  })
})
