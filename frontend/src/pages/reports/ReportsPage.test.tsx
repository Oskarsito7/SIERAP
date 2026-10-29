import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { catalogService } from '@/services/api/catalogService'
import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'
import { reportService } from '@/services/api/reportService'
import type { UserSummary } from '@/types/auth'
import type { AcademicPeriod, Program } from '@/types/catalog'
import type { Group } from '@/types/group'
import type {
  ComparativeReportData,
  GroupReportData,
  IndividualReportData,
  MomentReportData,
} from '@/types/report'

import { momentFixture } from '../groups/testData'
import { ReportsPage } from './ReportsPage'

vi.mock('@/services/api/catalogService', () => ({
  catalogService: {
    periods: vi.fn(),
    programs: vi.fn(),
  },
}))

vi.mock('@/services/api/momentService', () => ({
  momentService: {
    list: vi.fn(),
  },
}))

vi.mock('@/services/api/groupService', () => ({
  groupService: {
    list: vi.fn(),
  },
}))

vi.mock('@/services/api/reportService', () => ({
  reportService: {
    individual: vi.fn(),
    group: vi.fn(),
    moment: vi.fn(),
    comparative: vi.fn(),
  },
}))

const adminUser: UserSummary = {
  id: 2,
  firstNames: 'Admin',
  lastNames: 'Facultad',
  fullName: 'Admin Facultad',
  email: 'admin@unicesar.edu.co',
  role: 'ADMIN',
  active: true,
  mustChangePassword: false,
}

const periodsFixture: AcademicPeriod[] = [
  { id: 2, code: '2026-2', startDate: '2026-08-03', endDate: '2026-12-05', upcEvaluationDeadline: '2026-11-28', status: 'CURRENT' },
  { id: 1, code: '2026-1', startDate: '2026-02-01', endDate: '2026-06-30', upcEvaluationDeadline: '2026-06-25', status: 'CLOSED' },
]

const programsFixture: Program[] = [
  {
    id: 1,
    code: '108',
    name: 'Ingeniería de Sistemas',
    facultyId: 1,
    facultyName: 'Facultad de Ingenierías y Tecnologías',
    currentPeriodMomentCount: 1,
    studentCount: 24,
  },
]

const groupsFixture: Group[] = [
  {
    id: 7,
    momentId: 1,
    momentName: 'Momento 1',
    name: 'Grupo 1',
    projectTitle: 'SIERAP',
    teacher: { id: 12, fullName: 'Marta Díaz' },
    memberCount: 2,
    minMembers: 3,
    maxMembers: 5,
    full: false,
    members: [
      { id: 40, fullName: 'Ana Alba', studentCode: '2026101', email: 'ana@unicesar.edu.co' },
      { id: 41, fullName: 'Beto Bravo', studentCode: '2026102', email: 'beto@unicesar.edu.co' },
    ],
    submissionDeadline: '2026-11-15',
    evaluationDate: '2026-11-24',
    evaluators: [],
    myGroup: false,
  },
]

const individualFixture: IndividualReportData = {
  generatedAt: '2026-11-25T10:00:00',
  period: { id: 2, code: '2026-2' },
  program: { id: 1, code: '108', name: 'Ingeniería de Sistemas' },
  moment: { id: 1, momentNumber: 1, name: 'Momento 1', subject: 'Programación II', maxScore: 5 },
  student: { id: 40, studentCode: '2026101', fullName: 'Ana Alba', currentSemester: 3 },
  group: { id: 7, name: 'Grupo 1', projectTitle: 'SIERAP', teacherName: 'Marta Díaz' },
  final: true,
  individualGrade: 4.4,
  groupGrade: 4.52,
  percentage: 88,
  learningOutcomes: [
    {
      id: 10,
      code: 'RAP1',
      description: 'Diseño de software',
      score: 0.48,
      groupScore: 0.6,
      maxScore: 0.6,
      percentage: 80,
    },
  ],
  submittedEvaluations: 2,
  pendingEvaluations: 0,
}

const groupReportFixture: GroupReportData = {
  generatedAt: '2026-11-25T10:00:00',
  period: { id: 2, code: '2026-2' },
  program: { id: 1, code: '108', name: 'Ingeniería de Sistemas' },
  moment: { id: 1, momentNumber: 1, name: 'Momento 1', subject: 'Programación II', maxScore: 5 },
  group: { id: 7, name: 'Grupo 1', projectTitle: 'SIERAP', teacherName: 'Marta Díaz' },
  final: true,
  groupGrade: 4.52,
  percentage: 90.4,
  learningOutcomes: [
    { id: 10, code: 'RAP1', description: 'Diseño de software', score: 0.6, maxScore: 0.6, percentage: 100 },
  ],
  members: [
    {
      studentId: 40,
      studentCode: '2026101',
      fullName: 'Ana Alba',
      individualGrade: 4.4,
      adjusted: true,
      learningOutcomes: [{ id: 10, code: 'RAP1', score: 0.48, maxScore: 0.6, percentage: 80 }],
    },
    {
      studentId: 41,
      studentCode: '2026102',
      fullName: 'Beto Bravo',
      individualGrade: 4.52,
      adjusted: false,
      learningOutcomes: [{ id: 10, code: 'RAP1', score: 0.6, maxScore: 0.6, percentage: 100 }],
    },
  ],
  submittedEvaluations: 2,
  pendingEvaluations: 0,
}

const momentReportFixture: MomentReportData = {
  generatedAt: '2026-11-25T10:00:00',
  period: { id: 2, code: '2026-2' },
  program: { id: 1, code: '108', name: 'Ingeniería de Sistemas' },
  moment: { id: 1, momentNumber: 1, name: 'Momento 1', subject: 'Programación II', maxScore: 5 },
  final: true,
  totalGroups: 6,
  gradedGroups: 5,
  totalStudents: 24,
  gradedStudents: 20,
  averageGroupGrade: 4.18,
  averageGrade: 4.12,
  minGrade: 2.65,
  maxGrade: 4.85,
  passingCount: 18,
  passingPercentage: 90,
  learningOutcomes: [
    { id: 10, code: 'RAP1', description: 'Diseño de software', averageScore: 0.47, maxScore: 0.6, percentage: 78.33 },
  ],
  distribution: [
    { range: '0.00 - 2.99', label: 'Bajo', min: 0, max: 2.99, count: 2, percentage: 10 },
    { range: '3.00 - 3.79', label: 'Básico', min: 3, max: 3.79, count: 6, percentage: 30 },
    { range: '3.80 - 4.49', label: 'Alto', min: 3.8, max: 4.49, count: 8, percentage: 40 },
    { range: '4.50 - 5.00', label: 'Sobresaliente', min: 4.5, max: 5, count: 4, percentage: 20 },
  ],
}

const comparativeReportFixture: ComparativeReportData = {
  generatedAt: '2026-11-25T10:00:00',
  program: { id: 1, code: '108', name: 'Ingeniería de Sistemas' },
  momentNumber: 1,
  periodA: {
    period: { id: 1, code: '2026-1' },
    momentId: 4,
    momentName: 'Momento 1',
    subject: 'Programación II',
    maxScore: 5,
    gradedGroups: 5,
    gradedStudents: 19,
    averageGrade: 3.78,
    passingPercentage: 84.21,
    distribution: [
      { range: '0.00 - 2.99', label: 'Bajo', min: 0, max: 2.99, count: 3, percentage: 15.79 },
      { range: '3.00 - 3.79', label: 'Básico', min: 3, max: 3.79, count: 7, percentage: 36.84 },
      { range: '3.80 - 4.49', label: 'Alto', min: 3.8, max: 4.49, count: 6, percentage: 31.58 },
      { range: '4.50 - 5.00', label: 'Sobresaliente', min: 4.5, max: 5, count: 3, percentage: 15.79 },
    ],
  },
  periodB: {
    period: { id: 2, code: '2026-2' },
    momentId: 1,
    momentName: 'Momento 1',
    subject: 'Programación II',
    maxScore: 5,
    gradedGroups: 5,
    gradedStudents: 20,
    averageGrade: 4.12,
    passingPercentage: 90,
    distribution: [
      { range: '0.00 - 2.99', label: 'Bajo', min: 0, max: 2.99, count: 2, percentage: 10 },
      { range: '3.00 - 3.79', label: 'Básico', min: 3, max: 3.79, count: 6, percentage: 30 },
      { range: '3.80 - 4.49', label: 'Alto', min: 3.8, max: 4.49, count: 8, percentage: 40 },
      { range: '4.50 - 5.00', label: 'Sobresaliente', min: 4.5, max: 5, count: 4, percentage: 20 },
    ],
  },
  deltaAverageGrade: 0.34,
  deltaPassingPercentage: 5.79,
  learningOutcomes: [
    {
      code: 'RAP1',
      description: 'Diseño de software',
      averageScoreA: 0.43,
      maxScoreA: 0.6,
      percentageA: 71.67,
      averageScoreB: 0.47,
      maxScoreB: 0.6,
      percentageB: 78.33,
      deltaPercentage: 6.66,
    },
  ],
}

function renderReports(initialEntry = '/reports') {
  render(
    <AuthContext value={{ state: { status: 'authenticated', user: adminUser }, user: adminUser, logout: vi.fn() }}>
      <MemoryRouter initialEntries={[initialEntry]}>
        <ReportsPage />
      </MemoryRouter>
    </AuthContext>,
  )
}

describe('ReportsPage', () => {
  beforeEach(() => {
    vi.mocked(catalogService.periods).mockResolvedValue(periodsFixture)
    vi.mocked(catalogService.programs).mockResolvedValue(programsFixture)
    vi.mocked(momentService.list).mockResolvedValue([momentFixture({ id: 1, programId: 1, periodId: 2, number: 1 })])
    vi.mocked(groupService.list).mockResolvedValue(groupsFixture)
    vi.mocked(reportService.individual).mockResolvedValue(individualFixture)
    vi.mocked(reportService.group).mockResolvedValue(groupReportFixture)
    vi.mocked(reportService.moment).mockResolvedValue(momentReportFixture)
    vi.mocked(reportService.comparative).mockResolvedValue(comparativeReportFixture)
  })

  it('renders the individual report with RAP progress bars and allows changing student', async () => {
    renderReports()

    expect(await screen.findByLabelText('Nota individual')).toHaveTextContent('4,40')
    expect(screen.getByLabelText('Nota del grupo')).toHaveTextContent('4,52')
    expect(screen.getByText('Ajuste individual')).toBeInTheDocument()
    expect(screen.getByRole('meter', { name: 'RAP1 individual' })).toHaveAttribute('aria-valuenow', '0.48')

    await userEvent.selectOptions(screen.getByLabelText('Estudiante'), '41')
    expect(reportService.individual).toHaveBeenCalledWith(1, 41)
  })

  it('switches to the group report tab and shows member grades and adjustments', async () => {
    renderReports()

    await userEvent.click(await screen.findByRole('tab', { name: 'Grupal' }))

    expect(await screen.findByLabelText('Nota grupal del reporte')).toHaveTextContent('4,52')
    expect(screen.getByText('1 integrante con ajuste individual')).toBeInTheDocument()
    expect(screen.getByText('Ana Alba')).toBeInTheDocument()
    expect(screen.getByText('Beto Bravo')).toBeInTheDocument()
    expect(screen.getByText('Ajustado')).toBeInTheDocument()
  })

  it('switches to the consolidated moment report tab and shows averages and distribution', async () => {
    renderReports()

    await userEvent.click(await screen.findByRole('tab', { name: 'Consolidado' }))

    expect(await screen.findByLabelText('Promedio general del momento')).toHaveTextContent('4,12')
    expect(screen.getByLabelText('Tasa de aprobación')).toHaveTextContent('90,00 %')
    expect(screen.getByRole('meter', { name: 'Nivel Alto' })).toHaveAttribute('aria-valuenow', '40')
    expect(screen.getByText('Sobresaliente')).toBeInTheDocument()
  })

  it('switches to the comparative report tab (AD-08), renders paired bars and validates distinct periods', async () => {
    renderReports()

    await userEvent.click(await screen.findByRole('tab', { name: 'Comparativo' }))

    expect(await screen.findByText('Datos agregados por cohorte')).toBeInTheDocument()
    expect(screen.getByLabelText('Comparación de promedio general')).toHaveTextContent('3,78 → 4,12')
    expect(screen.getByText('+0,34')).toBeInTheDocument()
    expect(screen.getByText('+6,66 pp')).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByLabelText('Frente a periodo (B)'), '1')
    expect(await screen.findByText('Debe elegir dos periodos académicos distintos para comparar.')).toBeInTheDocument()
  })
})
