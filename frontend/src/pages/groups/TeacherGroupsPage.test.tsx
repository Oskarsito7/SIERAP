import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'

import { TeacherGroupsPage } from './TeacherGroupsPage'
import { groupFixture, momentFixture } from './testData'

vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn(), get: vi.fn() } }))
vi.mock('@/services/api/groupService', () => ({ groupService: { list: vi.fn(), create: vi.fn() } }))

const listMoments = vi.mocked(momentService.list)
const listGroups = vi.mocked(groupService.list)
const createGroup = vi.mocked(groupService.create)

function renderPage(path = '/groups') {
  render(
    <MemoryRouter initialEntries={[path]}>
      <TeacherGroupsPage />
    </MemoryRouter>,
  )
}

function card(name: string): HTMLElement {
  return screen.getByText(name, { selector: '[data-slot="card-title"]' }).closest('[data-slot="card"]') as HTMLElement
}

describe('TeacherGroupsPage', () => {
  beforeEach(() => {
    listMoments.mockReset()
    listGroups.mockReset()
    createGroup.mockReset()
  })

  it('shows the groups of the moment with their status and dates', async () => {
    listMoments.mockResolvedValue([momentFixture()])
    listGroups.mockResolvedValue([
      groupFixture({ id: 1, name: 'Grupo 1', projectTitle: 'SIERAP', memberCount: 5, full: true,
        submissionDeadline: '2026-10-15' }),
      groupFixture({ id: 2, name: 'Grupo 2', memberCount: 4,
        evaluators: [{ id: 51, fullName: 'Sofía Vega', evaluatorType: 'Empresario', countsInGrade: true }] }),
      groupFixture({ id: 3, name: 'Grupo 3', memberCount: 2 }),
    ])
    renderPage()

    expect(await screen.findByText('SIERAP')).toBeInTheDocument()
    expect(listGroups).toHaveBeenCalledWith(1)
    expect(within(card('Grupo 1')).getByText('Completo')).toBeInTheDocument()
    expect(within(card('Grupo 1')).getByText('15/10/2026')).toBeInTheDocument()
    expect(within(card('Grupo 2')).getByText('Con cupo')).toBeInTheDocument()
    expect(within(card('Grupo 2')).getByText('1 asignado')).toBeInTheDocument()
    expect(within(card('Grupo 3')).getByText('Bajo el mínimo')).toBeInTheDocument()
    expect(within(card('Grupo 3')).getByText('Sin título todavía')).toBeInTheDocument()
    expect(within(card('Grupo 3')).getByText('Ninguno')).toBeInTheDocument()
    expect(within(card('Grupo 1')).getByRole('link', { name: 'Abrir grupo' })).toHaveAttribute('href', '/groups/1')
  })

  it('enables a new group while the moment is in configuration', async () => {
    listMoments.mockResolvedValue([momentFixture()])
    listGroups.mockResolvedValue([])
    createGroup.mockResolvedValue(groupFixture({ id: 9, name: 'Grupo 4' }))
    renderPage()

    await userEvent.type(await screen.findByLabelText('Nombre del grupo'), ' Grupo 4 ')
    await userEvent.click(screen.getByRole('button', { name: 'Habilitar' }))

    expect(createGroup).toHaveBeenCalledWith(1, 'Grupo 4')
    expect(await screen.findByText('Grupo 4', { selector: '[data-slot="card-title"]' })).toBeInTheDocument()
  })

  it('does not offer to enable groups once the moment was launched', async () => {
    listMoments.mockResolvedValue([momentFixture({ status: 'LAUNCHED' })])
    listGroups.mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('El momento ya fue lanzado: no se pueden habilitar grupos nuevos.')).toBeInTheDocument()
    expect(screen.queryByLabelText('Nombre del grupo')).not.toBeInTheDocument()
  })

  it('switches between the moments of the period', async () => {
    listMoments.mockResolvedValue([momentFixture(), momentFixture({ id: 2, name: 'Momento 2' })])
    listGroups.mockResolvedValue([])
    renderPage('/groups?momentId=2')

    expect(await screen.findByRole('combobox', { name: 'Momento' })).toHaveValue('2')
    await waitFor(() => expect(listGroups).toHaveBeenCalledWith(2))

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Momento' }), '1')

    await waitFor(() => expect(listGroups).toHaveBeenLastCalledWith(1))
  })

  it('explains when there are no moments in the current period', async () => {
    listMoments.mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('No hay momentos de tu facultad en el periodo actual.')).toBeInTheDocument()
  })
})
