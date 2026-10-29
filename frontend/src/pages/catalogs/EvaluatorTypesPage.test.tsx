import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { accountService } from '@/services/api/accountService'
import { catalogService } from '@/services/api/catalogService'

import { EvaluatorTypesPage } from './EvaluatorTypesPage'
import { apiError } from './testHelpers'

vi.mock('@/services/api/accountService', () => ({ accountService: { listEvaluatorTypes: vi.fn() } }))
vi.mock('@/services/api/catalogService', () => ({
  catalogService: { createEvaluatorType: vi.fn(), updateEvaluatorType: vi.fn(), deleteEvaluatorType: vi.fn() },
}))

const service = vi.mocked(catalogService)
const graduate = { id: 2, name: 'Egresado', description: 'Graduado de la UPC', active: true, evaluatorCount: 12 }

describe('EvaluatorTypesPage', () => {
  beforeEach(() => {
    Object.values(service).forEach((mock) => mock.mockReset())
    vi.mocked(accountService.listEvaluatorTypes).mockResolvedValue([
      graduate,
      { id: 3, name: 'Investigador invitado', description: null, active: false, evaluatorCount: 2 },
    ])
  })
  afterEach(() => vi.restoreAllMocks())

  it('lists the types with their evaluators and turns one off sending its name and description', async () => {
    service.updateEvaluatorType.mockResolvedValue({ ...graduate, active: false })
    render(<EvaluatorTypesPage />)

    const row = await screen.findByRole('row', { name: /Egresado/ })
    expect(within(row).getByText('12')).toBeInTheDocument()
    expect(screen.getByRole('switch', { name: 'Investigador invitado activo' })).not.toBeChecked()

    await userEvent.click(within(row).getByRole('switch', { name: 'Egresado activo' }))

    expect(service.updateEvaluatorType).toHaveBeenCalledWith(2,
      { name: 'Egresado', description: 'Graduado de la UPC', active: false })
    expect(await screen.findByRole('switch', { name: 'Egresado activo' })).not.toBeChecked()
  })

  it('adds a type and clears the form', async () => {
    service.createEvaluatorType.mockResolvedValue({ id: 4, name: 'Docente de otra universidad', description: null,
      active: true, evaluatorCount: 0 })
    render(<EvaluatorTypesPage />)

    const form = await screen.findByRole('form', { name: 'Nuevo tipo' })
    await userEvent.type(within(form).getByLabelText('Nombre'), 'Docente de otra universidad')
    await userEvent.click(within(form).getByRole('button', { name: 'Agregar tipo' }))

    expect(service.createEvaluatorType).toHaveBeenCalledWith({ name: 'Docente de otra universidad', description: null })
    expect(await screen.findByRole('row', { name: /Docente de otra universidad/ })).toBeInTheDocument()
    expect(within(screen.getByRole('form', { name: 'Nuevo tipo' })).getByLabelText('Nombre')).toHaveValue('')
  })

  it('explains why a type in use cannot be deleted', async () => {
    service.deleteEvaluatorType.mockRejectedValue(apiError(409, 'Hay evaluadores de este tipo; desactívelo.'))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    render(<EvaluatorTypesPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Eliminar Egresado' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No se puede eliminar Egresado: Hay evaluadores')
  })
})
