import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { rubricService } from '@/services/api/rubricService'
import type { ApiError } from '@/types/api'
import type { LearningOutcome } from '@/types/moment'

import { RubricTab } from './RubricTab'
import { momentDetailFixture } from './testData'

vi.mock('@/services/api/rubricService', () => ({
  rubricService: {
    createOutcome: vi.fn(), updateOutcome: vi.fn(), deleteOutcome: vi.fn(),
    createCriterion: vi.fn(), updateCriterion: vi.fn(), deleteCriterion: vi.fn(),
    createLevel: vi.fn(), updateLevel: vi.fn(), deleteLevel: vi.fn(),
  },
}))

const service = vi.mocked(rubricService)
const onChange = vi.fn<() => Promise<void>>()

const rap1: LearningOutcome = {
  id: 10, code: 'RAP1', description: 'Integra las ciencias básicas', maxScore: 0.6,
  criteria: [{
    id: 20, description: 'Identificación del problema', displayOrder: 1, maxScore: 0.6,
    levels: [
      { id: 31, name: 'Alto', description: 'Causas y consecuencias', score: 0.6, displayOrder: 2 },
      { id: 30, name: 'Bajo', description: null, score: 0, displayOrder: 1 },
    ],
  }],
}
const rap2: LearningOutcome = { id: 11, code: 'RAP2', description: 'Desarrolla software', maxScore: 0, criteria: [] }

function renderTab(overrides = {}) {
  render(<RubricTab moment={momentDetailFixture({ maxScore: 0.6, learningOutcomes: [rap1, rap2], ...overrides })}
    onChange={onChange} />)
}

describe('RubricTab', () => {
  beforeEach(() => {
    Object.values(service).forEach((mock) => mock.mockReset().mockResolvedValue())
    onChange.mockReset().mockResolvedValue()
  })
  afterEach(() => vi.restoreAllMocks())

  it('shows the maximum score and each RAP with its criteria and levels in order', async () => {
    renderTab()

    expect(screen.getByLabelText('Puntaje máximo del momento')).toHaveTextContent('0,60')
    expect(screen.getByText('Puntaje máximo del momento · 2 RAP · 1 criterio')).toBeInTheDocument()
    const criterion = screen.getByRole('region', { name: 'Criterio 1 · Identificación del problema' })
    const levels = within(criterion).getAllByRole('listitem')
    expect(levels.map((item) => item.textContent)).toEqual([
      expect.stringContaining('Bajo'), expect.stringContaining('Alto'),
    ])
    expect(within(criterion).getByLabelText('Puntaje de Alto')).toHaveTextContent('0,60')

    const second = screen.getByRole('button', { name: /RAP2/ })
    expect(second).toHaveAttribute('aria-expanded', 'false')
    expect(second).toHaveTextContent('· 0 criterios')
    await userEvent.click(second)
    expect(screen.getByText('Este RAP todavía no tiene criterios.')).toBeInTheDocument()
  })

  it('adds a RAP and reads the moment again', async () => {
    renderTab()

    await userEvent.click(screen.getByRole('button', { name: 'Agregar RAP' }))
    const form = screen.getByRole('form', { name: 'Nuevo RAP' })
    await userEvent.click(within(form).getByRole('button', { name: 'Agregar RAP' }))
    expect(within(form).getByText('El código es obligatorio')).toBeInTheDocument()

    await userEvent.type(within(form).getByLabelText('Código'), 'RAP3')
    await userEvent.type(within(form).getByLabelText('Descripción del RAP'), 'Construye modelos')
    await userEvent.click(within(form).getByRole('button', { name: 'Agregar RAP' }))

    expect(service.createOutcome).toHaveBeenCalledWith(1, { code: 'RAP3', description: 'Construye modelos' })
    expect(onChange).toHaveBeenCalled()
    expect(screen.queryByRole('form', { name: 'Nuevo RAP' })).not.toBeInTheDocument()
  })

  it('adds a level with a decimal comma and checks the score', async () => {
    renderTab()
    const criterion = screen.getByRole('region', { name: 'Criterio 1 · Identificación del problema' })

    await userEvent.click(within(criterion).getByRole('button', { name: 'Agregar nivel' }))
    const form = within(criterion).getByRole('form', { name: 'Nuevo nivel' })
    await userEvent.type(within(form).getByLabelText('Nombre del nivel'), 'Básico')
    await userEvent.type(within(form).getByLabelText('Puntaje'), '0,365')
    await userEvent.click(within(form).getByRole('button', { name: 'Agregar nivel' }))
    expect(within(form).getByText(/Escribe un puntaje desde 0/)).toBeInTheDocument()

    await userEvent.clear(within(form).getByLabelText('Puntaje'))
    await userEvent.type(within(form).getByLabelText('Puntaje'), '0,36')
    await userEvent.click(within(form).getByRole('button', { name: 'Agregar nivel' }))

    expect(service.createLevel).toHaveBeenCalledWith(20, { name: 'Básico', description: null, score: 0.36 })
  })

  it('edits a level and shows the message of the API when a change fails', async () => {
    const data: ApiError = { timestamp: '', status: 422, error: '', path: '', fieldErrors: {},
      message: 'El momento ya fue lanzado: su configuración no se puede modificar.' }
    service.deleteCriterion.mockRejectedValue(
      new AxiosError('', 'ERR_BAD_REQUEST', undefined, null, { status: 422, data } as AxiosResponse))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderTab()

    await userEvent.click(screen.getByRole('button', { name: 'Editar Alto' }))
    const form = screen.getByRole('form', { name: 'Editar nivel Alto' })
    expect(within(form).getByLabelText('Puntaje')).toHaveValue('0,60')
    await userEvent.clear(within(form).getByLabelText('Puntaje'))
    await userEvent.type(within(form).getByLabelText('Puntaje'), '0.5')
    await userEvent.click(within(form).getByRole('button', { name: 'Guardar' }))
    expect(service.updateLevel).toHaveBeenCalledWith(31,
      { name: 'Alto', description: 'Causas y consecuencias', score: 0.5 })

    await userEvent.click(screen.getByRole('button', { name: 'Quitar criterio' }))
    expect(service.deleteCriterion).toHaveBeenCalledWith(20)
    expect(await screen.findByRole('alert')).toHaveTextContent('El momento ya fue lanzado')
  })

  it('only shows the rubric once the moment was launched', () => {
    renderTab({ status: 'LAUNCHED' })

    expect(screen.queryByRole('button', { name: 'Agregar RAP' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Agregar nivel' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Editar Alto' })).not.toBeInTheDocument()
  })
})
