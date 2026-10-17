import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { accountService } from '@/services/api/accountService'
import { momentService } from '@/services/api/momentService'

import { momentDetailFixture } from './testData'
import { WeightsTab } from './WeightsTab'

vi.mock('@/services/api/accountService', () => ({ accountService: { listEvaluatorTypes: vi.fn() } }))
vi.mock('@/services/api/momentService', () => ({ momentService: { updateWeights: vi.fn() } }))

const updateWeights = vi.mocked(momentService.updateWeights)
const onChange = vi.fn<() => Promise<void>>()

describe('WeightsTab', () => {
  beforeEach(() => {
    vi.mocked(accountService.listEvaluatorTypes).mockResolvedValue([
      { id: 1, name: 'Empresario', description: null, active: true },
      { id: 2, name: 'Egresado', description: null, active: true },
      { id: 3, name: 'Investigador', description: null, active: false },
    ])
    updateWeights.mockReset().mockResolvedValue()
    onChange.mockReset().mockResolvedValue()
  })

  it('shows the weights with their total and the example of the redistribution', async () => {
    render(<WeightsTab moment={momentDetailFixture({ teacherWeight: 60, totalWeight: 100, evaluatorWeights: [
      { evaluatorTypeId: 1, evaluatorTypeName: 'Empresario', weight: 20 },
      { evaluatorTypeId: 2, evaluatorTypeName: 'Egresado', weight: 20 },
    ] })} onChange={onChange} />)

    expect(await screen.findByLabelText('Peso de Docente del grupo')).toHaveValue('60')
    expect(screen.getByLabelText('Peso de Egresado')).toHaveValue('20')
    expect(screen.queryByLabelText('Peso de Investigador')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Total de los pesos')).toHaveTextContent('100 %')
    expect(screen.getByText(/no tiene evaluación enviada de un Egresado, su 20 %/)).toBeInTheDocument()
    expect(screen.getByText('60 % → 75 %')).toBeInTheDocument()
    expect(screen.getByText('20 % → 25 %')).toBeInTheDocument()
  })

  it('saves the weights, leaving out the types without weight', async () => {
    render(<WeightsTab moment={momentDetailFixture()} onChange={onChange} />)

    const teacher = await screen.findByLabelText('Peso de Docente del grupo')
    expect(teacher).toHaveValue('100')
    await userEvent.clear(teacher)
    await userEvent.type(teacher, '70')
    await userEvent.type(screen.getByLabelText('Peso de Empresario'), '0')
    expect(screen.getByRole('note')).toHaveTextContent('hoy suman 70 %')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar pesos' }))
    expect(screen.getByText('Escribe un porcentaje mayor que 0 y hasta 100')).toBeInTheDocument()
    expect(updateWeights).not.toHaveBeenCalled()

    await userEvent.clear(screen.getByLabelText('Peso de Empresario'))
    await userEvent.type(screen.getByLabelText('Peso de Empresario'), '30')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar pesos' }))

    expect(updateWeights).toHaveBeenCalledWith(1, {
      teacherWeight: 70, evaluatorWeights: [{ evaluatorTypeId: 1, weight: 30 }],
    })
    expect(onChange).toHaveBeenCalled()
    expect(await screen.findByRole('status')).toHaveTextContent('Se guardaron los pesos de la nota.')
  })

  it('is read only once the moment was launched', async () => {
    render(<WeightsTab moment={momentDetailFixture({ status: 'LAUNCHED' })} onChange={onChange} />)

    expect(await screen.findByLabelText('Peso de Docente del grupo')).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Guardar pesos' })).not.toBeInTheDocument()
  })
})
