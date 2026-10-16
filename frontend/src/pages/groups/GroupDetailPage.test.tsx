import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'
import { submissionService } from '@/services/api/submissionService'
import type { ApiError } from '@/types/api'
import type { MomentDetail } from '@/types/moment'

import { GroupDetailPage } from './GroupDetailPage'
import { groupFixture, momentFixture } from './testData'

vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn(), get: vi.fn() } }))
vi.mock('@/services/api/submissionService', () => ({ submissionService: { history: vi.fn() } }))
vi.mock('@/services/api/groupService', () => ({
  groupService: {
    get: vi.fn(),
    update: vi.fn(),
    remove: vi.fn(),
    updateProjectTitle: vi.fn(),
    studentsWithoutGroup: vi.fn(),
    addMember: vi.fn(),
    removeMember: vi.fn(),
    lookupEvaluator: vi.fn(),
    assignEvaluator: vi.fn(),
    removeEvaluator: vi.fn(),
  },
}))

const service = vi.mocked(groupService)
const getMoment = vi.mocked(momentService.get)

const members = [
  { id: 40, fullName: 'Juan Rojas', studentCode: '20261001', email: 'juan@unicesar.edu.co' },
  { id: 41, fullName: 'Ana Gómez', studentCode: '20261014', email: 'ana@unicesar.edu.co' },
]

function momentDetail(overrides = {}): MomentDetail {
  return { ...momentFixture(overrides), maxScore: 5, learningOutcomes: [] }
}

function apiError(status: number, message: string): AxiosError {
  const data: ApiError = { timestamp: '', status, error: '', message, path: '', fieldErrors: {} }
  return new AxiosError(message, 'ERR_BAD_REQUEST', undefined, null, { status, data } as AxiosResponse)
}

function renderPage(path = '/groups/7') {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/groups/:id" element={<GroupDetailPage />} />
        <Route path="/groups/:id/submissions" element={<GroupDetailPage tab="submissions" />} />
        <Route path="/groups" element={<p>Lista de grupos</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

function section(title: string): HTMLElement {
  return screen.getByText(title, { selector: '[data-slot="card-title"]' }).closest('[data-slot="card"]') as HTMLElement
}

describe('GroupDetailPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ now: new Date('2026-09-28T10:00:00'), toFake: ['Date'] })
    Object.values(service).forEach((mock) => mock.mockReset())
    getMoment.mockReset()
    getMoment.mockResolvedValue(momentDetail())
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('shows the members, the dates and the evaluators of the group', async () => {
    service.get.mockResolvedValue(groupFixture({
      memberCount: 2, members, submissionDeadline: '2026-10-15',
      evaluators: [{ id: 51, fullName: 'Sofía Vega', evaluatorType: 'Empresario' }],
    }))
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Grupo 1' })).toBeInTheDocument()
    expect(within(section('Integrantes (2 de 5)')).getByText('Juan Rojas')).toBeInTheDocument()
    expect(screen.getByLabelText('Fecha límite de entrega')).toHaveValue('2026-10-15')
    expect(within(section('Evaluadores externos')).getByText('Sofía Vega')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Eliminar grupo' })).not.toBeInTheDocument()
  })

  it('saves only the dates that changed', async () => {
    service.get.mockResolvedValue(groupFixture({ submissionDeadline: '2026-10-15' }))
    service.update.mockResolvedValue(groupFixture({ submissionDeadline: '2026-10-15', evaluationDate: '2026-11-10' }))
    renderPage()

    const evaluationDate = await screen.findByLabelText('Fecha de evaluación')
    expect(evaluationDate).toHaveAttribute('min', '2026-11-02')
    expect(evaluationDate).toHaveAttribute('max', '2026-11-20')
    await userEvent.type(evaluationDate, '2026-11-10')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar fechas' }))

    expect(service.update).toHaveBeenCalledWith(7, { evaluationDate: '2026-11-10' })
    expect(await screen.findByText('Fechas guardadas.')).toBeInTheDocument()
  })

  it('shows the reason when the API rejects a date', async () => {
    service.get.mockResolvedValue(groupFixture())
    service.update.mockRejectedValue(apiError(422, 'La fecha de evaluación debe estar dentro de la ventana de calificación.'))
    renderPage()

    await userEvent.type(await screen.findByLabelText('Fecha de evaluación'), '2026-11-25')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar fechas' }))

    expect(await within(section('Fechas del grupo')).findByRole('alert')).toHaveTextContent('ventana de calificación')
  })

  it('adds a student without a group', async () => {
    service.get.mockResolvedValue(groupFixture())
    service.studentsWithoutGroup.mockResolvedValue([
      { id: 42, fullName: 'Luis Martínez', studentCode: '20261022', currentSemester: 2 },
    ])
    service.addMember.mockResolvedValue(groupFixture({
      memberCount: 1, members: [{ id: 42, fullName: 'Luis Martínez', studentCode: '20261022', email: 'l@u.co' }],
    }))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Agregar estudiante sin grupo' }))
    await userEvent.selectOptions(await screen.findByRole('combobox', { name: 'Estudiante sin grupo' }), '42')
    await userEvent.click(screen.getByRole('button', { name: 'Agregar' }))

    expect(service.studentsWithoutGroup).toHaveBeenCalledWith(1)
    expect(service.addMember).toHaveBeenCalledWith(7, 42)
    expect(await screen.findByText('Integrantes (1 de 5)')).toBeInTheDocument()
  })

  it('removes a member after confirming', async () => {
    service.get.mockResolvedValueOnce(groupFixture({ memberCount: 2, members }))
    service.get.mockResolvedValueOnce(groupFixture({ memberCount: 1, members: [members[1]] }))
    service.removeMember.mockResolvedValue()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Quitar a Juan Rojas' }))

    expect(service.removeMember).toHaveBeenCalledWith(7, 40)
    expect(await screen.findByText('Integrantes (1 de 5)')).toBeInTheDocument()
    expect(screen.queryByText('Juan Rojas')).not.toBeInTheDocument()
  })

  it('finds an external evaluator by exact email and assigns them', async () => {
    service.get.mockResolvedValue(groupFixture())
    service.lookupEvaluator.mockResolvedValue({
      id: 51, firstNames: 'Sofía', lastNames: 'Vega', fullName: 'Sofía Vega', email: 'sofia@tecnosoft.co',
      role: 'EXTERNAL_EVALUATOR', active: true, mustChangePassword: false,
    })
    service.assignEvaluator.mockResolvedValue(groupFixture({
      evaluators: [{ id: 51, fullName: 'Sofía Vega', evaluatorType: 'Empresario' }],
    }))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Asignar' }))
    await userEvent.type(screen.getByLabelText('Correo o número de documento completos'), 'sofia@tecnosoft.co')
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    const result = (await screen.findByText('Sofía Vega')).closest('div') as HTMLElement
    await userEvent.click(within(result).getByRole('button', { name: 'Asignar' }))

    expect(service.lookupEvaluator).toHaveBeenCalledWith('sofia@tecnosoft.co')
    expect(service.assignEvaluator).toHaveBeenCalledWith(7, 51)
    expect(await screen.findByText('· Empresario')).toBeInTheDocument()
  })

  it('says when no evaluator matches the search', async () => {
    service.get.mockResolvedValue(groupFixture())
    service.lookupEvaluator.mockRejectedValue(apiError(404, 'No hay un evaluador externo activo con ese correo o documento.'))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Asignar' }))
    await userEvent.type(screen.getByLabelText('Correo o número de documento completos'), 'nadie@correo.co')
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No hay un evaluador externo activo')
  })

  it('disables every change after the grading window closed', async () => {
    getMoment.mockResolvedValue(momentDetail({ status: 'LAUNCHED', gradingEnd: '2026-09-20',
      gradingStart: '2026-09-10', submissionStart: '2026-09-01', submissionEnd: '2026-09-05' }))
    service.get.mockResolvedValue(groupFixture({ memberCount: 2, members }))
    renderPage()

    expect(await screen.findByText('La calificación ya cerró: el grupo no se puede modificar.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Agregar estudiante sin grupo' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Quitar a Juan Rojas' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Asignar' })).toBeDisabled()
    expect(screen.getByLabelText('Título del proyecto')).toBeDisabled()
  })

  it('deletes an empty group before the launch and goes back to the list', async () => {
    service.get.mockResolvedValue(groupFixture())
    service.remove.mockResolvedValue()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Eliminar grupo' }))

    expect(service.remove).toHaveBeenCalledWith(7)
    expect(await screen.findByText('Lista de grupos')).toBeInTheDocument()
  })
  it('shows the submissions of the group in their tab', async () => {
    service.get.mockResolvedValue(groupFixture({ members, memberCount: 2 }))
    vi.mocked(submissionService.history).mockResolvedValue({
      versions: [{ id: 90, groupId: 7, versionNumber: 1, submittedAt: '2026-10-20T11:30:00', late: false,
        zipFileName: 'sierap-v1.zip', zipSize: 1024, presentationFileName: null, presentationSize: null,
        toGrade: true }],
      currentPresentation: null,
    })
    renderPage('/groups/7/submissions')

    expect(await screen.findByText('sierap-v1.zip')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Entregas' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Mi calificación' })).toHaveAttribute('href', '/groups/7/evaluation')
    expect(screen.queryByText('Proyecto de aula')).not.toBeInTheDocument()
  })
})
