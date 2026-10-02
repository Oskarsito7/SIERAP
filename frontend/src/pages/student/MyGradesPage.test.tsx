import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { momentService } from '@/services/api/momentService'
import { resultsService } from '@/services/api/resultsService'
import type { StudentResult } from '@/types/results'

import { momentFixture } from '../groups/testData'
import { MyGradesPage } from './MyGradesPage'

vi.mock('@/services/api/resultsService', () => ({ resultsService: { mine: vi.fn() }, RESULTS_API_READY: true }))
vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn() } }))

function resultFixture(overrides: Partial<StudentResult> = {}): StudentResult {
  return {
    momentId: 1,
    momentName: 'Momento 1',
    periodCode: '2026-2',
    groupId: 7,
    groupName: 'Grupo 1',
    projectTitle: 'SIERAP',
    available: true,
    finalGrade: 4.63,
    groupGrade: 4.51,
    maxScore: 5,
    learningOutcomes: [{ code: 'RAP1', description: 'Formula el problema', score: 0.57, maxScore: 0.6 }],
    comments: [{ evaluatorKind: 'Empresario', comment: 'Buen trabajo.' }],
    ...overrides,
  }
}

function renderPage() {
  render(
    <MemoryRouter>
      <MyGradesPage />
    </MemoryRouter>,
  )
}

describe('MyGradesPage', () => {
  beforeEach(() => {
    vi.mocked(resultsService.mine).mockReset()
    vi.mocked(momentService.list).mockResolvedValue([
      momentFixture({ gradingEnd: '2026-11-28' }),
      momentFixture({ id: 2, name: 'Momento 2', gradingEnd: '2027-05-20' }),
    ])
  })

  it('shows the final grade, the score per RAP and the comments', async () => {
    vi.mocked(resultsService.mine).mockResolvedValue([resultFixture()])
    renderPage()

    expect(await screen.findByLabelText('Mi nota final')).toHaveTextContent('4,63')
    expect(screen.getByLabelText('Nota del grupo')).toHaveTextContent('4,51')
    expect(screen.getByText(/un evaluador ajustó tu calificación/)).toBeInTheDocument()
    expect(screen.getByRole('meter', { name: 'RAP1: Formula el problema' })).toHaveAttribute('aria-valuenow', '0.57')
    expect(screen.getByText('0,57 / 0,60')).toBeInTheDocument()
    expect(screen.getByText('Empresario')).toBeInTheDocument()
    expect(screen.getByText('Momento 1 · Programación II · 2026-2')).toBeInTheDocument()
  })

  it('says when the grades will be ready and lets the student pick another moment', async () => {
    vi.mocked(resultsService.mine).mockResolvedValue([
      resultFixture({ momentId: 2, momentName: 'Momento 2', available: false, finalGrade: null, groupGrade: null,
        learningOutcomes: [], comments: [] }),
      resultFixture(),
    ])
    renderPage()

    expect(await screen.findByText('Tus notas del Momento 2 aún no están listas')).toBeInTheDocument()
    expect(screen.getByText(/La calificación cierra el 20\/05\/2027/)).toBeInTheDocument()
    expect(screen.queryByLabelText('Mi nota final')).not.toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText('Momento'), '1')
    expect(await screen.findByLabelText('Mi nota final')).toHaveTextContent('4,63')
  })

  it('invites the student to join a group when there are no results', async () => {
    vi.mocked(resultsService.mine).mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('Todavía no tienes notas')).toBeInTheDocument()
  })
})
