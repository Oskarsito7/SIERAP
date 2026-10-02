import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'
import type { ApiError } from '@/types/api'
import type { Group } from '@/types/group'

import { groupFixture, momentFixture } from '../groups/testData'
import { MomentGroupsPage } from './MomentGroupsPage'
import { studentUser } from './testData'

vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn(), get: vi.fn() } }))
vi.mock('@/services/api/groupService', () => ({
  groupService: { list: vi.fn(), join: vi.fn(), leave: vi.fn() },
}))

const listMoments = vi.mocked(momentService.list)
const listGroups = vi.mocked(groupService.list)
const join = vi.mocked(groupService.join)
const leave = vi.mocked(groupService.leave)

const me = { id: 40, fullName: 'Juan Rojas', studentCode: '2026001', email: 'juan@unicesar.edu.co' }
const other = (id: number, fullName: string) => ({ id, fullName, studentCode: null, email: null })

function myGroup(overrides: Partial<Group> = {}): Group {
  return groupFixture({
    id: 1,
    name: 'Grupo 1',
    projectTitle: 'SIERAP',
    members: [me, other(41, 'Ana Gómez'), other(42, 'Luis Martínez')],
    memberCount: 3,
    myGroup: true,
    ...overrides,
  })
}

function renderPage() {
  render(
    <AuthContext.Provider
      value={{ state: { status: 'authenticated', user: studentUser }, user: studentUser, logout: vi.fn() }}
    >
      <MemoryRouter initialEntries={['/moment-groups']}>
        <MomentGroupsPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  )
}

function card(name: string): HTMLElement {
  return screen.getByText(name, { selector: '[data-slot="card-title"]' }).closest('[data-slot="card"]') as HTMLElement
}

describe('MomentGroupsPage', () => {
  beforeEach(() => {
    listMoments.mockReset()
    listGroups.mockReset()
    join.mockReset()
    leave.mockReset()
  })

  it('shows every group of the moment with its status and marks the student in their own group', async () => {
    listMoments.mockResolvedValue([momentFixture()])
    listGroups.mockResolvedValue([
      myGroup(),
      groupFixture({ id: 2, name: 'Grupo 2', members: [other(50, 'Laura Beltrán')], memberCount: 1 }),
      groupFixture({ id: 4, name: 'Grupo 4', memberCount: 5, full: true,
        teacher: { id: 20, fullName: 'Rafael Ospino' } }),
    ])
    renderPage()

    expect(await screen.findByText('Juan Rojas (tú)')).toBeInTheDocument()
    expect(listGroups).toHaveBeenCalledWith(1)
    expect(screen.getByText('Conformación abierta hasta el 30/09/2026')).toBeInTheDocument()
    expect(screen.getByText('Estás en el Grupo 1')).toBeInTheDocument()
    expect(within(card('Grupo 1')).getByText('Mi grupo')).toBeInTheDocument()
    expect(within(card('Grupo 1')).getByRole('button', { name: 'Salir del grupo' })).toBeEnabled()
    expect(within(card('Grupo 2')).getByText('Laura Beltrán')).toBeInTheDocument()
    expect(within(card('Grupo 2')).getByRole('button', { name: 'Cambiarme a este grupo' })).toBeEnabled()
    expect(within(card('Grupo 4')).getByText('Completo')).toBeInTheDocument()
    expect(within(card('Grupo 4')).getByRole('button', { name: 'Grupo completo' })).toBeDisabled()
  })

  it('joins a group directly when the student has none', async () => {
    listMoments.mockResolvedValue([momentFixture()])
    listGroups.mockResolvedValue([groupFixture({ id: 2, name: 'Grupo 2' })])
    join.mockResolvedValue(groupFixture({ id: 2, name: 'Grupo 2', members: [me], memberCount: 1, myGroup: true }))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Unirme' }))

    expect(join).toHaveBeenCalledWith(2)
    expect(await screen.findByText('Estás en el Grupo 2')).toBeInTheDocument()
    expect(within(card('Grupo 2')).getByText('1 de 5')).toBeInTheDocument()
  })

  it('asks for confirmation before switching and leaves the previous group in the same step', async () => {
    listMoments.mockResolvedValue([momentFixture()])
    listGroups.mockResolvedValue([myGroup(), groupFixture({ id: 2, name: 'Grupo 2', projectTitle: 'Biblioteca' })])
    join.mockResolvedValue(groupFixture({ id: 2, name: 'Grupo 2', members: [me], memberCount: 1, myGroup: true }))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Cambiarme a este grupo' }))
    const dialog = screen.getByRole('alertdialog')
    expect(within(dialog).getByText('¿Cambiarte al Grupo 2?')).toBeInTheDocument()
    expect(within(dialog).getByText(/Saldrás del Grupo 1 \(SIERAP\)/)).toBeInTheDocument()
    expect(join).not.toHaveBeenCalled()

    await userEvent.click(within(dialog).getByRole('button', { name: 'Cambiarme al Grupo 2' }))

    expect(join).toHaveBeenCalledWith(2)
    expect(await screen.findByText('Estás en el Grupo 2')).toBeInTheDocument()
    expect(within(card('Grupo 1')).getByText('2 de 5')).toBeInTheDocument()
    expect(within(card('Grupo 1')).queryByText('Juan Rojas (tú)')).not.toBeInTheDocument()
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })

  it('leaves the group', async () => {
    listMoments.mockResolvedValue([momentFixture()])
    listGroups.mockResolvedValue([myGroup()])
    leave.mockResolvedValue()
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Salir del grupo' }))

    expect(leave).toHaveBeenCalledWith(1)
    expect(await within(card('Grupo 1')).findByRole('button', { name: 'Unirme' })).toBeInTheDocument()
    expect(screen.queryByText('Estás en el Grupo 1')).not.toBeInTheDocument()
  })

  it('disables the actions once the group formation closed', async () => {
    listMoments.mockResolvedValue([momentFixture({ status: 'LAUNCHED', groupFormationOpen: false })])
    listGroups.mockResolvedValue([myGroup(), groupFixture({ id: 2, name: 'Grupo 2' })])
    renderPage()

    expect(await screen.findByText('Conformación cerrada: el momento ya fue lanzado')).toBeInTheDocument()
    expect(within(card('Grupo 1')).getByRole('button', { name: 'Salir del grupo' })).toBeDisabled()
    expect(within(card('Grupo 2')).getByRole('button', { name: 'Cambiarme a este grupo' })).toBeDisabled()
  })

  it('shows the message of the API when joining fails', async () => {
    listMoments.mockResolvedValue([momentFixture()])
    listGroups.mockResolvedValue([groupFixture({ id: 2, name: 'Grupo 2' })])
    join.mockRejectedValue(apiError(422, 'El grupo ya tiene el máximo de 5 integrantes.'))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Unirme' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El grupo ya tiene el máximo de 5 integrantes.')
  })

  it('says so when there are no moments for the student', async () => {
    listMoments.mockResolvedValue([])
    renderPage()

    expect(
      await screen.findByText('No hay momentos de tu programa para tu semestre en el periodo actual.'),
    ).toBeInTheDocument()
  })
})

function apiError(status: number, message: string): AxiosError<ApiError> {
  const data: ApiError = { timestamp: '', status, error: '', message, path: '', fieldErrors: {} }
  return new AxiosError('error', String(status), undefined, undefined, { status, data } as AxiosResponse<ApiError>)
}
