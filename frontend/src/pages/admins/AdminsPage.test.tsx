import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { accountService } from '@/services/api/accountService'
import { adminService } from '@/services/api/adminService'
import { catalogService } from '@/services/api/catalogService'
import type { UserSummary } from '@/types/auth'

import { apiError } from '../catalogs/testHelpers'
import { AdminsPage } from './AdminsPage'

vi.mock('@/services/api/adminService', () => ({
  adminService: { list: vi.fn(), create: vi.fn(), setActive: vi.fn() },
}))
vi.mock('@/services/api/accountService', () => ({ accountService: { resendPasswordLink: vi.fn() } }))
vi.mock('@/services/api/catalogService', () => ({ catalogService: { faculties: vi.fn() } }))

const service = vi.mocked(adminService)
const engineering = { id: 4, code: '04', name: 'Facultad de Ingenierías y Tecnologías', programCount: 2 }
const health = { id: 2, code: '02', name: 'Facultad de Ciencias de la Salud', programCount: 4 }

function admin(overrides: Partial<UserSummary> = {}): UserSummary {
  return {
    id: 1, firstNames: 'Laura', lastNames: 'Mendoza', fullName: 'Laura Mendoza',
    email: 'laura.mendoza@unicesar.edu.co', role: 'ADMIN', active: true, mustChangePassword: false, ...overrides,
  }
}

const carmen = admin({ id: 5, firstNames: 'Carmen', lastNames: 'Pérez', fullName: 'Carmen Pérez',
  email: 'carmen.perez@unicesar.edu.co' })

describe('AdminsPage', () => {
  beforeEach(() => {
    Object.values(service).forEach((mock) => mock.mockReset())
    vi.mocked(accountService.resendPasswordLink).mockReset()
    vi.mocked(catalogService.faculties).mockResolvedValue([engineering, health])
    service.list.mockImplementation(async (facultyId) => (facultyId === 4 ? [admin()] : [carmen]))
  })
  afterEach(() => vi.restoreAllMocks())

  it('lists the admins of the chosen faculty', async () => {
    render(<AdminsPage />)

    expect(await screen.findByRole('row', { name: /Laura Mendoza/ })).toBeInTheDocument()
    expect(service.list).toHaveBeenCalledWith(4)

    await userEvent.selectOptions(screen.getByLabelText('Facultad'), '2')
    expect(await screen.findByRole('row', { name: /Carmen Pérez/ })).toBeInTheDocument()
    expect(screen.queryByRole('row', { name: /Laura Mendoza/ })).not.toBeInTheDocument()
    expect(service.list).toHaveBeenLastCalledWith(2)
  })

  it('creates an admin in the chosen faculty and adds it to the list', async () => {
    service.create.mockResolvedValue(admin({ id: 9, firstNames: 'Andrés', lastNames: 'Gutiérrez',
      fullName: 'Andrés Gutiérrez', email: 'andres.gutierrez@unicesar.edu.co' }))
    render(<AdminsPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo administrador' }))
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))
    expect(screen.getByText('Los nombres son obligatorios')).toBeInTheDocument()
    expect(service.create).not.toHaveBeenCalled()

    await userEvent.type(screen.getByLabelText('Nombres'), 'Andrés')
    await userEvent.type(screen.getByLabelText('Apellidos'), 'Gutiérrez')
    await userEvent.type(screen.getByLabelText('Número de documento'), '77184552')
    await userEvent.type(screen.getByLabelText('Correo'), 'andres.gutierrez@unicesar.edu.co')
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))

    expect(service.create).toHaveBeenCalledWith(4, { firstNames: 'Andrés', lastNames: 'Gutiérrez',
      documentType: 'CC', documentNumber: '77184552', email: 'andres.gutierrez@unicesar.edu.co' })
    expect(await screen.findByRole('status')).toHaveTextContent('Se creó a Andrés Gutiérrez.')
    expect(screen.getByRole('row', { name: /Andrés Gutiérrez/ })).toBeInTheDocument()
  })

  it('shows the message of the API when the admin cannot be created', async () => {
    service.create.mockRejectedValue(apiError(409, 'Ya existe una cuenta con ese correo.'))
    render(<AdminsPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo administrador' }))
    await userEvent.type(screen.getByLabelText('Nombres'), 'Laura')
    await userEvent.type(screen.getByLabelText('Apellidos'), 'Mendoza')
    await userEvent.type(screen.getByLabelText('Número de documento'), '1')
    await userEvent.type(screen.getByLabelText('Correo'), 'laura.mendoza@unicesar.edu.co')
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe una cuenta con ese correo.')
  })

  it('deactivates an admin after confirming and resends the link', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    service.setActive.mockResolvedValue(admin({ active: false }))
    vi.mocked(accountService.resendPasswordLink).mockResolvedValue()
    render(<AdminsPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Reenviar enlace a Laura Mendoza' }))
    expect(accountService.resendPasswordLink).toHaveBeenCalledWith(1)
    expect(await screen.findByRole('status'))
      .toHaveTextContent('Se envió de nuevo el enlace a laura.mendoza@unicesar.edu.co.')

    await userEvent.click(screen.getByRole('button', { name: 'Desactivar a Laura Mendoza' }))
    expect(service.setActive).toHaveBeenCalledWith(1, false)
    const laura = screen.getByRole('row', { name: /Laura Mendoza/ })
    expect(within(laura).getByText('Desactivado')).toBeInTheDocument()
  })
})
