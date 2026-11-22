import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { saveFile } from '@/lib/files'
import { momentService } from '@/services/api/momentService'
import { reportService } from '@/services/api/reportService'
import { resultsService } from '@/services/api/resultsService'
import type { StudentResult } from '@/types/results'

import { momentFixture } from '../groups/testData'
import { MyGradesPage } from './MyGradesPage'

vi.mock('@/services/api/resultsService', () => ({ resultsService: { mine: vi.fn() } }))
vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn() } }))
vi.mock('@/services/api/reportService', () => ({ reportService: { individualPdf: vi.fn() } }))
vi.mock('@/lib/files', () => ({ saveFile: vi.fn() }))

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
    learningOutcomes: [
      {
        code: 'RAP1',
        description: 'Formula el problema',
        score: 0.57,
        maxScore: 0.6,
        criteria: [{ description: 'Identifica el problema', score: 0.37, maxScore: 0.4 }],
      },
    ],
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

  it('shows a dash in a RAP that has no score that counts', async () => {
    vi.mocked(resultsService.mine).mockResolvedValue([
      resultFixture({
        learningOutcomes: [{ code: 'RAP1', description: 'Formula el problema', score: null, maxScore: 0.6, criteria: [] }],
      }),
    ])
    renderPage()

    const meter = await screen.findByRole('meter', { name: 'RAP1: Formula el problema' })
    expect(meter).not.toHaveAttribute('aria-valuenow')
    expect(screen.getByText('— / 0,60')).toBeInTheDocument()
  })

  it('shows the consolidated score per criterion inside each RAP, with a dash when there is none', async () => {
    vi.mocked(resultsService.mine).mockResolvedValue([
      resultFixture({
        learningOutcomes: [
          {
            code: 'RAP1',
            description: 'Formula el problema',
            score: 0.57,
            maxScore: 0.6,
            criteria: [
              { description: 'Identifica el problema', score: 0.37, maxScore: 0.4 },
              { description: 'Justifica la solución', score: null, maxScore: 0.2 },
            ],
          },
        ],
      }),
    ])
    renderPage()

    await userEvent.click(await screen.findByText('Ver criterios'))
    expect(screen.getByText('Identifica el problema')).toBeInTheDocument()
    expect(screen.getByText('0,37 / 0,40')).toBeInTheDocument()
    expect(screen.getByText('— / 0,20')).toBeInTheDocument()
    expect(screen.getByRole('meter', { name: 'Criterio: Identifica el problema' })).toHaveAttribute(
      'aria-valuenow',
      '0.37',
    )
    expect(screen.getByRole('meter', { name: 'Criterio: Justifica la solución' })).not.toHaveAttribute('aria-valuenow')
    expect(screen.getByText(/Es el puntaje que cuenta en tu nota/)).toBeInTheDocument()
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

  it('downloads the bulletin of the moment once the grades are available', async () => {
    vi.mocked(resultsService.mine).mockResolvedValue([resultFixture()])
    const blob = new Blob(['%PDF'])
    vi.mocked(reportService.individualPdf).mockResolvedValue({ blob, fileName: 'boletin-2026001.pdf' })
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Descargar mi boletín en PDF' }))

    expect(reportService.individualPdf).toHaveBeenCalledWith(1)
    expect(saveFile).toHaveBeenCalledWith(blob, 'boletin-2026001.pdf')
  })

  it('does not offer the bulletin while the grades are not ready', async () => {
    vi.mocked(resultsService.mine).mockResolvedValue([resultFixture({ available: false, finalGrade: null })])
    renderPage()

    expect(await screen.findByText(/aún no están listas/)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /boletín/ })).not.toBeInTheDocument()
  })
})
