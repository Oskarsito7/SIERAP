import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { accountService } from '@/services/api/accountService'
import { catalogService } from '@/services/api/catalogService'
import { teacherService } from '@/services/api/teacherService'
import type { ApiError } from '@/types/api'
import type { UserSummary } from '@/types/auth'

import { TeachersPage } from './TeachersPage'

vi.mock('@/services/api/teacherService', () => ({
  teacherService: { list: vi.fn(), create: vi.fn(), setActive: vi.fn() },
}))
vi.mock('@/services/api/accountService', () => ({ accountService: { resendPasswordLink: vi.fn() } }))
vi.mock('@/services/api/catalogService', () => ({ catalogService: { faculties: vi.fn() } }))

const service = vi.mocked(teacherService)
const faculty = { id: 4, code: '04', name: 'Facultad de Ingenierías y Tecnologías', programCount: 2 }

function teacher(overrides: Partial<UserSummary> = {}): UserSummary {
  return {
    id: 1, firstNames: 'Marta', lastNames: 'Díaz', fullName: 'Marta Díaz', email: 'marta.diaz@unicesar.edu.co',
    role: 'TEACHER', active: true, mustChangePassword: false, ...overrides,
  }
}

const rafael = teacher({ id: 2, firstNames: 'Rafael', lastNames: 'Ospino', fullName: 'Rafael Ospino',
  email: 'rafael.ospino@unicesar.edu.co' })

function apiError(status: number, message: string): AxiosError {
  const data: ApiError = { timestamp: '', status, error: '', message, path: '', fieldErrors: {} }
  return new AxiosError(message, 'ERR_BAD_REQUEST', undefined, null, { status, data } as AxiosResponse)
}

describe('TeachersPage', () => {
  beforeEach(() => {
    Object.values(service).forEach((mock) => mock.mockReset())
    vi.mocked(accountService.resendPasswordLink).mockReset()
    vi.mocked(catalogService.faculties).mockResolvedValue([faculty])
    service.list.mockResolvedValue([teacher(), rafael])
  })
  afterEach(() => vi.restoreAllMocks())

  it('lists the teachers of the admin faculty and filters them', async () => {
    render(<TeachersPage />)

    const marta = await screen.findByRole('row', { name: /Marta Díaz/ })
    expect(within(marta).getByText('Activo')).toBeInTheDocument()
    expect(service.list).toHaveBeenCalledWith(4)
    expect(screen.getByText(/Los docentes de la Facultad de Ingenierías y Tecnologías/)).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Buscar por nombre o correo'), 'ospino')
    expect(screen.queryByRole('row', { name: /Marta Díaz/ })).not.toBeInTheDocument()
    expect(screen.getByRole('row', { name: /Rafael Ospino/ })).toBeInTheDocument()
  })

  it('creates a teacher in the admin faculty and adds it to the list', async () => {
    service.create.mockResolvedValue(teacher({ id: 3, firstNames: 'Diana', lastNames: 'Quintero',
      fullName: 'Diana Quintero', email: 'diana@unicesar.edu.co' }))
    render(<TeachersPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo docente' }))
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))
    expect(screen.getByText('Los nombres son obligatorios')).toBeInTheDocument()
    expect(service.create).not.toHaveBeenCalled()

    await userEvent.type(screen.getByLabelText('Nombres'), 'Diana')
    await userEvent.type(screen.getByLabelText('Apellidos'), 'Quintero')
    await userEvent.type(screen.getByLabelText('Número de documento'), '36555010')
    await userEvent.type(screen.getByLabelText('Correo'), 'diana@unicesar.edu.co')
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))

    expect(service.create).toHaveBeenCalledWith(4, { firstNames: 'Diana', lastNames: 'Quintero',
      documentType: 'CC', documentNumber: '36555010', email: 'diana@unicesar.edu.co' })
    expect(await screen.findByRole('status')).toHaveTextContent('Se creó a Diana Quintero.')
    expect(screen.getByRole('row', { name: /Diana Quintero/ })).toBeInTheDocument()
  })

  it('shows the message of the API when the teacher cannot be created', async () => {
    service.create.mockRejectedValue(apiError(409, 'Ya existe una cuenta con ese correo.'))
    render(<TeachersPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo docente' }))
    await userEvent.type(screen.getByLabelText('Nombres'), 'Marta')
    await userEvent.type(screen.getByLabelText('Apellidos'), 'Díaz')
    await userEvent.type(screen.getByLabelText('Número de documento'), '1')
    await userEvent.type(screen.getByLabelText('Correo'), 'marta.diaz@unicesar.edu.co')
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe una cuenta con ese correo.')
  })

  it('deactivates a teacher after confirming and can reactivate them', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    service.setActive.mockResolvedValueOnce(teacher({ active: false }))
    service.setActive.mockResolvedValueOnce(teacher({ active: true }))
    render(<TeachersPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Desactivar a Marta Díaz' }))
    expect(service.setActive).toHaveBeenCalledWith(1, false)
    const marta = screen.getByRole('row', { name: /Marta Díaz/ })
    expect(within(marta).getByText('Desactivado')).toBeInTheDocument()
    expect(within(marta).getByRole('button', { name: 'Reenviar enlace a Marta Díaz' })).toBeDisabled()

    await userEvent.click(within(marta).getByRole('button', { name: 'Reactivar a Marta Díaz' }))
    expect(service.setActive).toHaveBeenLastCalledWith(1, true)
    expect(await within(marta).findByText('Activo')).toBeInTheDocument()
  })

  it('resends the password link', async () => {
    vi.mocked(accountService.resendPasswordLink).mockResolvedValue()
    render(<TeachersPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Reenviar enlace a Rafael Ospino' }))

    expect(accountService.resendPasswordLink).toHaveBeenCalledWith(2)
    expect(await screen.findByRole('status'))
      .toHaveTextContent('Se envió de nuevo el enlace a rafael.ospino@unicesar.edu.co.')
  })
})
