import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { apiError } from '@/pages/catalogs/testHelpers'
import { groupFixture, momentFixture } from '@/pages/groups/testData'
import { periodFixture, programFixture } from '@/pages/moments/testData'
import { catalogService } from '@/services/api/catalogService'
import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'
import { reportService } from '@/services/api/reportService'
import type { UserRole, UserSummary } from '@/types/auth'

import { ReportsPage } from './ReportsPage'
import {
  comparativeReportFixture,
  groupReportFixture,
  individualReportFixture,
  momentReportFixture,
} from './testData'

vi.mock('@/services/api/catalogService', () => ({ catalogService: { periods: vi.fn(), programs: vi.fn() } }))
vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn() } }))
vi.mock('@/services/api/groupService', () => ({ groupService: { list: vi.fn() } }))
vi.mock('@/services/api/reportService', () => ({
  REPORTS_API_READY: false,
  reportService: { moment: vi.fn(), group: vi.fn(), individual: vi.fn(), comparative: vi.fn() },
}))

const reports = vi.mocked(reportService)
const current = periodFixture({ id: 1, code: '2026-2', startDate: '2026-08-03' })
const previous = periodFixture({ id: 2, code: '2026-1', startDate: '2026-02-02', status: 'CLOSED' })
const older = periodFixture({ id: 3, code: '2025-2', startDate: '2025-08-04', status: 'CLOSED' })

const member = (id: number, fullName: string) => ({ id, fullName, studentCode: String(id), email: null })
const groups = [
  groupFixture({ id: 7, name: 'Grupo 1', members: [member(40, 'Juan Rojas'), member(41, 'Ana Pérez')] }),
  groupFixture({ id: 8, name: 'Grupo 2', members: [member(42, 'Sara Díaz')] }),
]

function renderAt(tab: string, role: UserRole = 'ADMIN') {
  const user: UserSummary = {
    id: 12,
    firstNames: 'Laura',
    lastNames: 'Mendoza',
    fullName: 'Laura Mendoza',
    email: 'laura@unicesar.edu.co',
    role,
    active: true,
    mustChangePassword: false,
  }
  render(
    <AuthContext value={{ state: { status: 'authenticated', user }, user, logout: vi.fn() }}>
      <MemoryRouter initialEntries={[`/reports${tab ? `?tab=${tab}` : ''}`]}>
        <ReportsPage />
      </MemoryRouter>
    </AuthContext>,
  )
}

describe('ReportsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(catalogService.periods).mockResolvedValue([older, current, previous])
    vi.mocked(catalogService.programs).mockResolvedValue([programFixture()])
    vi.mocked(momentService.list).mockImplementation(async (periodId) => [
      momentFixture({ id: periodId === 1 ? 1 : 4, periodId: periodId ?? 1 }),
    ])
    vi.mocked(groupService.list).mockResolvedValue(groups)
    reports.moment.mockResolvedValue(momentReportFixture())
    reports.group.mockResolvedValue(groupReportFixture())
    reports.individual.mockResolvedValue(individualReportFixture())
    reports.comparative.mockResolvedValue(comparativeReportFixture())
  })

  it('opens the report of a moment of the current period, with its figures and RAP', async () => {
    renderAt('')

    expect(await screen.findByText('4,12')).toBeInTheDocument()
    expect(momentService.list).toHaveBeenCalledWith(1)
    expect(reports.moment).toHaveBeenCalledWith(1)
    expect(screen.getByRole('button', { name: 'Por momento' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('note')).toHaveTextContent('Datos de ejemplo')
    expect(screen.getByText('Notas definitivas')).toBeInTheDocument()
    expect(screen.getByText('20 de 24')).toBeInTheDocument()
    expect(screen.getByText('18 de 20 estudiantes calificados')).toBeInTheDocument()

    const bars = screen.getByRole('list', { name: 'Logro promedio por RAP' })
    expect(within(bars).getAllByRole('listitem')[0]).toHaveTextContent('RAP178,33 %')
    const distribution = screen.getByRole('list', { name: 'Distribución de notas' })
    expect(within(distribution).getAllByRole('listitem')[2]).toHaveTextContent('Alto3,80 – 4,498 · 40 %')
    expect(within(screen.getByRole('table', { name: 'Detalle por RAP' })).getAllByRole('row')).toHaveLength(3)
  })

  it('says so when a moment has no graded students yet', async () => {
    reports.moment.mockResolvedValue(momentReportFixture({ gradedStudents: 0, final: false }))
    renderAt('moment')

    expect(await screen.findByText(/Todavía no hay estudiantes calificados en este momento/)).toBeInTheDocument()
    expect(screen.getByText('Vista previa')).toBeInTheDocument()
    expect(screen.queryByRole('list', { name: 'Logro promedio por RAP' })).not.toBeInTheDocument()
  })

  it('shows the group report with each member and their adjustment, and changes group', async () => {
    renderAt('group')

    const table = await screen.findByRole('table', { name: 'Integrantes' })
    expect(reports.group).toHaveBeenCalledWith(7)
    expect(screen.getByText('de 5,00 · 90,40 %')).toBeInTheDocument()
    const juan = within(table).getByRole('row', { name: /Juan Rojas/ })
    expect(juan).toHaveTextContent('Con ajuste')
    expect(juan).toHaveTextContent('4,40')
    expect(within(table).getByRole('row', { name: /Ana Pérez/ })).not.toHaveTextContent('Con ajuste')

    await userEvent.selectOptions(screen.getByLabelText('Grupo'), 'Grupo 2')
    expect(reports.group).toHaveBeenLastCalledWith(8)
  })

  it('shows the individual report of the chosen student', async () => {
    renderAt('individual')

    expect(await screen.findByText('4,40')).toBeInTheDocument()
    // Los integrantes van por nombre: Ana Pérez es la primera del Grupo 1.
    expect(reports.individual).toHaveBeenCalledWith(1, 41)
    expect(screen.getByRole('table', { name: 'Detalle por RAP' })).toHaveTextContent('0,480,600,6080 %')

    await userEvent.selectOptions(screen.getByLabelText('Estudiante'), 'Juan Rojas')
    expect(reports.individual).toHaveBeenLastCalledWith(1, 40)
  })

  it('tells a teacher when they have no groups in the moment', async () => {
    vi.mocked(groupService.list).mockResolvedValue([])
    renderAt('group', 'TEACHER')

    expect(await screen.findByText('No tienes grupos en este momento.')).toBeInTheDocument()
    expect(reports.group).not.toHaveBeenCalled()
  })

  it('compares the previous period with the current one, only with aggregates', async () => {
    renderAt('comparative')

    const table = await screen.findByRole('table', { name: 'Cambio por RAP' })
    expect(reports.comparative).toHaveBeenCalledWith({ programId: 1, momentNumber: 1, periodAId: 2, periodBId: 1 })
    expect(within(table).getByRole('row', { name: /RAP1/ })).toHaveTextContent('72 %78 %+6')
    expect(within(table).getByRole('row', { name: /RAP4/ })).toHaveTextContent('70 %69 %−1')
    expect(within(table).getByRole('row', { name: /RAP6/ })).toHaveTextContent('—50 %—')
    expect(screen.getByText('+0,34 frente a 2026-1 (3,78)')).toBeInTheDocument()
    expect(screen.getByRole('listitem', { name: 'RAP1: 2026-1 72 %, 2026-2 78 %' })).toBeInTheDocument()
    expect(screen.queryByText('Juan Rojas')).not.toBeInTheDocument()
  })

  it('does not compare a period with itself', async () => {
    renderAt('comparative')

    await screen.findByRole('table', { name: 'Cambio por RAP' })
    await userEvent.selectOptions(screen.getByLabelText('Periodo base'), '2026-2')

    expect(screen.getByRole('alert')).toHaveTextContent('Elige dos periodos distintos para comparar.')
    expect(reports.comparative).toHaveBeenCalledTimes(1)
  })

  it('shows the message of the API when a report cannot be generated', async () => {
    reports.moment.mockRejectedValue(apiError(403, 'Este momento está fuera de su alcance.'))
    renderAt('moment')

    expect(await screen.findByText('Este momento está fuera de su alcance.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /reintentar/i })).toBeInTheDocument()
  })

  it('switches tabs and keeps the chosen tab in the address', async () => {
    renderAt('moment')

    await screen.findByText('4,12')
    await userEvent.click(screen.getByRole('button', { name: 'Grupal' }))

    expect(screen.getByRole('button', { name: 'Grupal' })).toHaveAttribute('aria-current', 'page')
    expect(await screen.findByRole('table', { name: 'Integrantes' })).toBeInTheDocument()
  })
})
