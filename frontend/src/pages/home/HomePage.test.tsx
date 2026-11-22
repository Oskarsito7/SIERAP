import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { groupFixture, momentFixture } from '@/pages/groups/testData'
import { periodFixture } from '@/pages/moments/testData'
import { catalogService } from '@/services/api/catalogService'
import { evaluationService } from '@/services/api/evaluationService'
import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'
import { systemStatusService } from '@/services/api/systemStatusService'
import type { UserRole, UserSummary } from '@/types/auth'
import type { EvaluationSummary } from '@/types/evaluation'

import { HomePage } from './HomePage'
import { momentPhase } from './momentPhase'

vi.mock('@/services/api/groupService', () => ({ groupService: { list: vi.fn(), myGroups: vi.fn() } }))
vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn() } }))
vi.mock('@/services/api/evaluationService', () => ({ evaluationService: { mine: vi.fn() } }))
vi.mock('@/services/api/catalogService', () => ({ catalogService: { periods: vi.fn(), faculties: vi.fn() } }))
vi.mock('@/services/api/systemStatusService', () => ({ systemStatusService: { getStatus: vi.fn() } }))

const launched = (overrides = {}) =>
  momentFixture({ status: 'LAUNCHED', groupFormationOpen: false, ...overrides })

function evaluation(groupId: number, status: 'DRAFT' | 'SUBMITTED'): EvaluationSummary {
  return { id: groupId, groupId, evaluator: { id: 60, fullName: 'Sofía Vega', kind: 'Empresario' }, status,
    evaluatedAt: '2026-11-24T16:40:00', totalScore: 3, comment: null }
}

function renderAs(role: UserRole) {
  const user: UserSummary = { id: 12, firstNames: 'Laura', lastNames: 'Mendoza', fullName: 'Laura Mendoza',
    email: 'laura@unicesar.edu.co', role, active: true, mustChangePassword: false }
  render(
    <AuthContext value={{ state: { status: 'authenticated', user }, user, logout: vi.fn() }}>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </AuthContext>,
  )
}

describe('HomePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 10, 10, 0))
    return () => vi.useRealTimers()
  })

  it('shows the student their group, its dates and the upload while submissions are open', async () => {
    vi.mocked(groupService.myGroups).mockResolvedValue([
      groupFixture({ projectTitle: 'SIERAP', submissionDeadline: '2026-10-25', evaluationDate: '2026-11-12' }),
    ])
    vi.mocked(momentService.list).mockResolvedValue([
      launched({ submissionOpen: true }),
      momentFixture({ id: 2, name: 'Momento 2', status: 'LAUNCHED', groupFormationOpen: true }),
    ])
    renderAs('STUDENT')

    expect(await screen.findByRole('heading', { name: 'Hola, Laura' })).toBeInTheDocument()
    expect(await screen.findByText('Grupo 1 · Momento 1')).toBeInTheDocument()
    expect(screen.getByText('Entregas · hasta el 31/10/2026')).toBeInTheDocument()
    expect(screen.getByText('12/11/2026')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Subir entrega' })).toHaveAttribute('href', '/my-group?groupId=7')
    expect(screen.getByRole('link', { name: /Inscríbete en un grupo de Momento 2/ })).toHaveAttribute(
      'href',
      '/moment-groups',
    )
    expect(screen.queryByText('API conectada')).not.toBeInTheDocument()
  })

  it('shows the teacher what to grade and which groups lack dates', async () => {
    vi.mocked(groupService.list).mockResolvedValue([
      groupFixture({ id: 7, name: 'Grupo 1', submissionDeadline: '2026-10-25', evaluationDate: '2026-11-12' }),
      groupFixture({ id: 8, name: 'Grupo 2', submissionDeadline: '2026-10-25', evaluationDate: '2026-11-12' }),
      groupFixture({ id: 9, name: 'Grupo 3', submissionDeadline: null, evaluationDate: null }),
    ])
    vi.mocked(momentService.list).mockResolvedValue([launched({ gradingOpen: true })])
    vi.mocked(evaluationService.mine).mockResolvedValue([evaluation(7, 'SUBMITTED'), evaluation(8, 'DRAFT')])
    renderAs('TEACHER')

    expect(await screen.findByText('1 de 3')).toBeInTheDocument()
    const toGrade = screen.getByText('Por calificar').closest('[data-slot="card"]') as HTMLElement
    expect(within(toGrade).getByRole('link', { name: /Grupo 2 · Momento 1/ })).toHaveAttribute(
      'href',
      '/groups/8/evaluation',
    )
    expect(within(toGrade).queryByText(/Grupo 1 · /)).not.toBeInTheDocument()
    const dates = screen.getByText('Requieren fechas').closest('[data-slot="card"]') as HTMLElement
    expect(within(dates).getByText('Sin fecha de entrega · Sin sustentación')).toBeInTheDocument()
  })

  it('lists the pending groups of the external evaluator by evaluation date', async () => {
    vi.mocked(groupService.list).mockResolvedValue([
      groupFixture({ id: 1, name: 'Grupo A', evaluationDate: '2026-11-20' }),
      groupFixture({ id: 2, name: 'Grupo B', evaluationDate: '2026-11-12' }),
      groupFixture({ id: 3, name: 'Grupo C', evaluationDate: '2026-11-10' }),
    ])
    vi.mocked(evaluationService.mine).mockResolvedValue([evaluation(3, 'SUBMITTED'), evaluation(1, 'DRAFT')])
    renderAs('EXTERNAL_EVALUATOR')

    const pending = await screen.findAllByRole('link', { name: /^Grupo [ABC]/ })
    expect(pending.map((link) => link.getAttribute('href'))).toEqual(['/assigned-groups/2', '/assigned-groups/1'])
    expect(pending[1]).toHaveTextContent('Borrador guardado')
  })

  it('counts the moments of the faculty by phase for the admin', async () => {
    vi.mocked(momentService.list).mockResolvedValue([
      momentFixture({ id: 1 }),
      launched({ id: 2, name: 'Momento 2', submissionOpen: true, groupCount: 4 }),
      launched({ id: 3, name: 'Momento 3', gradingEnd: '2026-11-01' }),
    ])
    vi.mocked(catalogService.periods).mockResolvedValue([periodFixture()])
    renderAs('ADMIN')

    expect(await screen.findByText('Límite UPC: 28/11/2026')).toBeInTheDocument()
    expect(screen.getByText('1 cerrado')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Momento 2 · Ingeniería de Sistemas/ })).toHaveAttribute('href', '/moments/2')
    expect(screen.getByRole('link', { name: /Nuevo momento/ })).toHaveAttribute('href', '/moments/new')
  })

  it('gives the super_admin the institutional summary and the system status', async () => {
    vi.mocked(catalogService.faculties).mockResolvedValue([
      { id: 1, code: 'ING', name: 'Ingenierías', programCount: 3 },
      { id: 2, code: 'CON', name: 'Ciencias Contables', programCount: 2 },
    ])
    vi.mocked(catalogService.periods).mockResolvedValue([periodFixture()])
    vi.mocked(momentService.list).mockResolvedValue([])
    vi.mocked(systemStatusService.getStatus).mockResolvedValue({ application: 'sierap', version: '0.1.0',
      databaseAvailable: true, timestamp: '2026-11-10T10:00:00' })
    renderAs('SUPER_ADMIN')

    expect(await screen.findByText('5')).toBeInTheDocument()
    expect(screen.getByText('2026-2')).toBeInTheDocument()
    expect(await screen.findByText('API conectada')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Reportes/ })).toHaveAttribute('href', '/reports')
  })
})

describe('momentPhase', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 10, 25, 10, 0))
    return () => vi.useRealTimers()
  })

  it('takes the open window from the flags of the API and closes after grading', () => {
    expect(momentPhase(momentFixture()).label).toBe('En configuración')
    expect(momentPhase(launched({ groupFormationOpen: true })).label).toBe('Conformación de grupos')
    expect(momentPhase(launched({ gradingOpen: true }))).toEqual({
      label: 'Calificación', until: '20/11/2026', variant: 'default' })
    expect(momentPhase(launched()).label).toBe('Cerrado')
    expect(momentPhase(launched({ gradingEnd: '2026-12-01' })).label).toBe('Lanzado')
  })
})
