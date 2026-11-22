import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { evaluationService } from '@/services/api/evaluationService'
import { groupService } from '@/services/api/groupService'

import { groupFixture } from '../groups/testData'
import { AssignedGroupsPage } from './AssignedGroupsPage'

vi.mock('@/services/api/groupService', () => ({ groupService: { list: vi.fn() } }))
vi.mock('@/services/api/evaluationService', () => ({ evaluationService: { list: vi.fn(), mine: vi.fn() } }))

const listGroups = vi.mocked(groupService.list)
const listEvaluations = vi.mocked(evaluationService.list)
const myEvaluations = vi.mocked(evaluationService.mine)

function evaluation(groupId: number, status: 'DRAFT' | 'SUBMITTED', totalScore: number) {
  return { id: groupId, groupId, evaluator: { id: 60, fullName: 'Sofía Vega', kind: 'Empresario' }, status, totalScore,
    evaluatedAt: '2026-11-24T16:40:00', comment: null }
}

describe('AssignedGroupsPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 25, 10, 0))
    listGroups.mockReset()
    listEvaluations.mockReset()
    myEvaluations.mockReset()
    myEvaluations.mockResolvedValue([])
    return () => vi.useRealTimers()
  })

  it('lists the assigned groups with the state of the own evaluation', async () => {
    listGroups.mockResolvedValue([
      groupFixture({ id: 1, name: 'Grupo 1', projectTitle: 'SIERAP', evaluationDate: '2026-11-24' }),
      groupFixture({ id: 2, name: 'Grupo 2', evaluationDate: '2026-11-25',
        teacher: { id: 20, fullName: 'Rafael Ospino' } }),
      groupFixture({ id: 4, name: 'Grupo 4', evaluationDate: '2026-11-27' }),
    ])
    // Trae también una evaluación de un grupo que ya no está asignado: no se muestra.
    myEvaluations.mockResolvedValue([evaluation(2, 'DRAFT', 2.5), evaluation(1, 'SUBMITTED', 4.24),
      evaluation(9, 'DRAFT', 1)])
    render(
      <MemoryRouter>
        <AssignedGroupsPage />
      </MemoryRouter>,
    )

    const rows = await screen.findAllByRole('row')
    expect(within(rows[1]).getByText('SIERAP')).toBeInTheDocument()
    expect(within(rows[1]).getByText('Enviada · 4,24')).toBeInTheDocument()
    expect(within(rows[1]).getByRole('link', { name: 'Ver o corregir Grupo 1' }))
      .toHaveAttribute('href', '/assigned-groups/1')
    expect(within(rows[2]).getByText('Rafael Ospino')).toBeInTheDocument()
    expect(within(rows[2]).getByText('Borrador · 2,50')).toBeInTheDocument()
    expect(within(rows[2]).getByRole('link', { name: 'Continuar Grupo 2' })).toBeInTheDocument()
    expect(within(rows[3]).getByText('Sin empezar')).toBeInTheDocument()
    expect(within(rows[3]).getByText('Desde el 27/11/2026')).toBeInTheDocument()
    expect(within(rows[3]).queryByRole('link')).not.toBeInTheDocument()
    expect(rows).toHaveLength(4)
    expect(myEvaluations).toHaveBeenCalledTimes(1)
    expect(listEvaluations).not.toHaveBeenCalled()
  })

  it('says so when no group was assigned yet', async () => {
    listGroups.mockResolvedValue([])
    render(
      <MemoryRouter>
        <AssignedGroupsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Todavía no te han asignado grupos para calificar.')).toBeInTheDocument()
  })
})
