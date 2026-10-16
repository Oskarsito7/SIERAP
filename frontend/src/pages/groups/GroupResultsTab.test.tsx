import { render, screen, within } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { evaluationService } from '@/services/api/evaluationService'
import { resultsService } from '@/services/api/resultsService'
import type { GroupResults } from '@/types/results'

import { GroupResultsTab } from './GroupResultsTab'
import { momentFixture } from './testData'

vi.mock('@/services/api/resultsService', () => ({ resultsService: { group: vi.fn() }, RESULTS_API_READY: true }))
vi.mock('@/services/api/evaluationService', () => ({ evaluationService: { list: vi.fn() } }))

const outcomes = (first: number, second: number) => [
  { id: 10, code: 'RAP1', score: first, maxScore: 1 },
  { id: 11, code: 'RAP2', score: second, maxScore: 2 },
]

function resultsFixture(overrides: Partial<GroupResults> = {}): GroupResults {
  return {
    groupId: 7,
    final: false,
    maxScore: 3,
    groupGrade: 2.5,
    groupLearningOutcomes: outcomes(0.5, 2),
    members: [
      { studentId: 50, fullName: 'Ana Gómez', individualGrade: 3, learningOutcomes: outcomes(1, 2) },
      { studentId: 51, fullName: 'Luis Martínez', individualGrade: 2.5, learningOutcomes: outcomes(0.5, 2) },
    ],
    submittedEvaluations: 1,
    pendingEvaluations: 1,
    ...overrides,
  }
}

describe('GroupResultsTab', () => {
  beforeEach(() => {
    vi.mocked(resultsService.group).mockReset()
    vi.mocked(evaluationService.list).mockResolvedValue([
      { id: 1, evaluator: { id: 12, fullName: 'Marta Díaz', kind: 'Docente' }, status: 'SUBMITTED',
        evaluatedAt: '2026-11-24T10:00:00', totalScore: 2.5, comment: null },
      { id: 2, evaluator: { id: 70, fullName: 'Carlos Pérez', kind: 'Egresado' }, status: 'DRAFT',
        evaluatedAt: '2026-11-24T11:00:00', totalScore: 1, comment: null },
    ])
  })

  it('shows a preview with the group grade, the evaluations and the individual grades', async () => {
    vi.mocked(resultsService.group).mockResolvedValue(resultsFixture())
    render(<GroupResultsTab groupId={7} moment={momentFixture({ gradingEnd: '2026-11-28' })} />)

    expect(await screen.findByText('Vista previa')).toBeInTheDocument()
    expect(screen.getByText(/definitivas al cerrar la calificación, el 28\/11\/2026/)).toBeInTheDocument()
    expect(screen.getByLabelText('Nota grupal')).toHaveTextContent('2,50')
    expect(screen.getByText('1 enviada · 1 pendiente')).toBeInTheDocument()

    const draft = screen.getByRole('row', { name: /Carlos Pérez/ })
    expect(within(draft).getByText('Borrador')).toBeInTheDocument()
    expect(within(draft).getByText('—')).toBeInTheDocument()

    const ana = screen.getByRole('row', { name: /Ana Gómez/ })
    expect(within(ana).getByText('Ajustado')).toBeInTheDocument()
    expect(within(ana).getByText('3,00')).toBeInTheDocument()
    expect(within(screen.getByRole('row', { name: /Luis Martínez/ })).queryByText('Ajustado')).not.toBeInTheDocument()
  })

  it('shows dashes while no evaluation was submitted', async () => {
    vi.mocked(resultsService.group).mockResolvedValue(resultsFixture({
      final: true,
      groupGrade: null,
      submittedEvaluations: 0,
      members: [{ studentId: 50, fullName: 'Ana Gómez', individualGrade: null, learningOutcomes: outcomes(0, 0) }],
    }))
    render(<GroupResultsTab groupId={7} moment={momentFixture()} />)

    expect(await screen.findByText('Notas definitivas')).toBeInTheDocument()
    expect(screen.getByLabelText('Nota grupal')).toHaveTextContent('—')
    expect(screen.getByText('Todavía no hay evaluaciones enviadas.')).toBeInTheDocument()
    expect(within(screen.getByRole('row', { name: /Ana Gómez/ })).getAllByText('—')).toHaveLength(3)
  })
})
