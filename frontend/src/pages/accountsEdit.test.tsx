import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { accountService } from '@/services/api/accountService'
import { adminService } from '@/services/api/adminService'
import { catalogService } from '@/services/api/catalogService'
import { teacherService } from '@/services/api/teacherService'
import { userService } from '@/services/api/userService'
import type { UserSummary } from '@/types/auth'

import { AdminsPage } from './admins/AdminsPage'
import { apiError } from './catalogs/testHelpers'
import { ExternalEvaluatorsPage } from './externalEvaluators/ExternalEvaluatorsPage'
import { TeachersPage } from './teachers/TeachersPage'

vi.mock('@/services/api/adminService', () => ({ adminService: { list: vi.fn(), create: vi.fn(), setActive: vi.fn() } }))
vi.mock('@/services/api/teacherService', () => ({
  teacherService: { list: vi.fn(), create: vi.fn(), setActive: vi.fn() },
}))
vi.mock('@/services/api/accountService', () => ({
  accountService: {
    listExternalEvaluatorsPage: vi.fn(),
    createExternalEvaluator: vi.fn(),
    resendPasswordLink: vi.fn(),
    listEvaluatorTypes: vi.fn(),
  },
}))
vi.mock('@/services/api/catalogService', () => ({ catalogService: { faculties: vi.fn() } }))
vi.mock('@/services/api/userService', () => ({ userService: { update: vi.fn() } }))

const faculty = { id: 4, code: '04', name: 'Facultad de Ingenierías y Tecnologías', programCount: 2 }

function user(role: UserSummary['role'], overrides: Partial<UserSummary> = {}): UserSummary {
  return {
    id: 1,
    firstNames: 'Laura',
    lastNames: 'Mendoza',
    fullName: 'Laura Mendoza',
    email: 'laura.mendoza@unicesar.edu.co',
    role,
    active: true,
    mustChangePassword: false,
    documentType: 'CC',
    documentNumber: '36500100',
    ...overrides,
  }
}

describe('editing accounts from the lists', () => {
  beforeEach(() => {
    vi.mocked(userService.update).mockReset()
    vi.mocked(catalogService.faculties).mockResolvedValue([faculty])
    vi.mocked(teacherService.list).mockReset().mockResolvedValue([user('TEACHER')])
    vi.mocked(adminService.list).mockReset().mockResolvedValue([user('ADMIN')])
    vi.mocked(accountService.listExternalEvaluatorsPage).mockReset().mockResolvedValue({ items: [user('EXTERNAL_EVALUATOR')], total: 1 })
  })

  it.each([
    ['teachers', <TeachersPage key="t" />, teacherService.list],
    ['admins', <AdminsPage key="a" />, adminService.list],
    ['external evaluators', <ExternalEvaluatorsPage key="e" />, accountService.listExternalEvaluatorsPage],
  ])('lets the user edit the data of an account in the list of %s', async (_name, page, list) => {
    vi.mocked(userService.update).mockResolvedValue(user('TEACHER', { firstNames: 'Laura Patricia' }))
    render(<MemoryRouter>{page}</MemoryRouter>)

    await userEvent.click(await screen.findByRole('button', { name: 'Editar a Laura Mendoza' }))
    expect(screen.getByLabelText('Nombres')).toHaveValue('Laura')
    expect(screen.getByLabelText('Número de documento')).toHaveValue('36500100')
    expect(screen.getByLabelText('Correo')).toHaveValue('laura.mendoza@unicesar.edu.co')
    expect(screen.queryByLabelText('Código estudiantil')).not.toBeInTheDocument()

    await userEvent.clear(screen.getByLabelText('Nombres'))
    await userEvent.type(screen.getByLabelText('Nombres'), 'Laura Patricia')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(userService.update).toHaveBeenCalledWith(1, { firstNames: 'Laura Patricia' })
    expect(await screen.findByRole('status')).toHaveTextContent('Se actualizaron los datos de Laura Mendoza.')
    expect(list).toHaveBeenCalledTimes(2)
    expect(screen.queryByLabelText('Nombres')).not.toBeInTheDocument()
  })

  it('warns that the sessions are closed when the email changes and shows API errors in the form', async () => {
    vi.mocked(userService.update).mockRejectedValue(
      apiError(422, 'Ya existe una cuenta con el correo otro@unicesar.edu.co.'),
    )
    render(<TeachersPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Editar a Laura Mendoza' }))
    await userEvent.clear(screen.getByLabelText('Correo'))
    await userEvent.type(screen.getByLabelText('Correo'), 'otro@unicesar.edu.co')
    expect(screen.getByText(/se cerrarán las sesiones abiertas/)).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(userService.update).toHaveBeenCalledWith(1, { email: 'otro@unicesar.edu.co' })
    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe una cuenta con el correo otro@unicesar.edu.co.')
    // El formulario sigue abierto para corregir.
    expect(screen.getByLabelText('Correo')).toBeInTheDocument()
  })

  it('validates required fields in the form before calling the API', async () => {
    render(<TeachersPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Editar a Laura Mendoza' }))
    await userEvent.clear(screen.getByLabelText('Apellidos'))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(screen.getByText('Los apellidos son obligatorios')).toBeInTheDocument()
    expect(userService.update).not.toHaveBeenCalled()
  })
})
