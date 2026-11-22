import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { apiError } from '@/pages/catalogs/testHelpers'
import { catalogService } from '@/services/api/catalogService'
import { registrationCodeService } from '@/services/api/registrationCodeService'
import { teacherService } from '@/services/api/teacherService'
import type { ApiError } from '@/types/api'
import type { UserRole, UserSummary } from '@/types/auth'
import type { Program } from '@/types/catalog'
import type { RegistrationCode } from '@/types/registrationCode'

import { RegistrationCodesPage } from './RegistrationCodesPage'

vi.mock('@/services/api/registrationCodeService', () => ({
  registrationCodeService: { list: vi.fn(), create: vi.fn(), deactivate: vi.fn() },
}))

vi.mock('@/services/api/catalogService', () => ({
  catalogService: { faculties: vi.fn(), programs: vi.fn() },
}))

vi.mock('@/services/api/teacherService', () => ({
  teacherService: { list: vi.fn() },
}))

const list = vi.mocked(registrationCodeService.list)
const create = vi.mocked(registrationCodeService.create)
const deactivate = vi.mocked(registrationCodeService.deactivate)
const faculties = vi.mocked(catalogService.faculties)
const programs = vi.mocked(catalogService.programs)
const teachers = vi.mocked(teacherService.list)

function program(id: number, name: string): Program {
  return {
    id,
    code: String(id),
    name,
    facultyId: 3,
    facultyName: 'Facultad de Ingenierías',
    currentPeriodMomentCount: 0,
    studentCount: 0,
  }
}

function teacher(id: number, fullName: string, active = true): UserSummary {
  const [firstNames, lastNames] = fullName.split(' ')
  return {
    id,
    firstNames,
    lastNames,
    fullName,
    email: `${firstNames.toLowerCase()}@unicesar.edu.co`,
    role: 'TEACHER',
    active,
    mustChangePassword: false,
  }
}

function code(overrides: Partial<RegistrationCode>): RegistrationCode {
  return {
    id: 1,
    code: 'K7MQ4TZP',
    programId: 1,
    programName: 'Ingeniería de Sistemas',
    teacherId: 12,
    teacherName: 'Marta Díaz',
    expiresOn: '2026-10-15',
    active: true,
    registeredCount: 27,
    ...overrides,
  }
}

const codes: RegistrationCode[] = [
  code({ id: 1 }),
  code({ id: 2, code: 'B8WJ3NQA', expiresOn: '2026-08-30', registeredCount: 12 }),
  code({ id: 3, code: 'T5CZ7KMD', active: false, registeredCount: 3 }),
  code({ id: 4, code: 'R2HX9PLE', teacherId: 20, teacherName: 'Rafael Ospino', registeredCount: 31 }),
]

function renderAs(role: UserRole) {
  const user: UserSummary = {
    id: 12,
    firstNames: 'Marta',
    lastNames: 'Díaz',
    fullName: 'Marta Díaz',
    email: 'marta@unicesar.edu.co',
    role,
    active: true,
    mustChangePassword: false,
  }
  render(
    <AuthContext value={{ state: { status: 'authenticated', user }, user, logout: vi.fn() }}>
      <MemoryRouter>
        <RegistrationCodesPage />
      </MemoryRouter>
    </AuthContext>,
  )
}

function rowOf(codeText: string): HTMLElement {
  return screen.getByText(codeText).closest('tr') as HTMLElement
}

describe('RegistrationCodesPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ now: new Date('2026-09-29T10:00:00'), toFake: ['Date'] })
    list.mockReset()
    create.mockReset()
    deactivate.mockReset()
    programs.mockResolvedValue([program(1, 'Ingeniería de Sistemas'), program(2, 'Ingeniería Electrónica')])
    faculties.mockResolvedValue([{ id: 3, code: '04', name: 'Facultad de Ingenierías', programCount: 2 }])
    teachers.mockResolvedValue([
      teacher(20, 'Rafael Ospino'),
      teacher(12, 'Marta Díaz'),
      teacher(30, 'Luis Pérez', false),
    ])
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('shows each code with its status: active, expired or deactivated', async () => {
    list.mockResolvedValue(codes.slice(0, 3))
    renderAs('TEACHER')

    expect(await screen.findByText('K7MQ4TZP')).toBeInTheDocument()
    expect(within(rowOf('K7MQ4TZP')).getByText('Activo')).toBeInTheDocument()
    expect(within(rowOf('K7MQ4TZP')).getByText('15/10/2026')).toBeInTheDocument()
    expect(within(rowOf('B8WJ3NQA')).getByText('Vencido')).toBeInTheDocument()
    expect(within(rowOf('T5CZ7KMD')).getByText('Desactivado')).toBeInTheDocument()
  })

  it('only offers copy and deactivate on active codes', async () => {
    list.mockResolvedValue(codes.slice(0, 3))
    renderAs('TEACHER')

    await screen.findByText('K7MQ4TZP')
    expect(within(rowOf('K7MQ4TZP')).getByRole('button', { name: /desactivar/i })).toBeInTheDocument()
    expect(within(rowOf('B8WJ3NQA')).queryByRole('button')).not.toBeInTheDocument()
    expect(within(rowOf('T5CZ7KMD')).queryByRole('button')).not.toBeInTheDocument()
  })

  it('hides the teacher column for a teacher and shows it with a filter for an admin', async () => {
    list.mockResolvedValue(codes)
    renderAs('ADMIN')

    await screen.findByText('K7MQ4TZP')
    expect(screen.getByRole('columnheader', { name: 'Docente' })).toBeInTheDocument()

    await userEvent.selectOptions(screen.getByRole('combobox', { name: 'Docente' }), 'Rafael Ospino')

    expect(screen.getByText('R2HX9PLE')).toBeInTheDocument()
    expect(screen.queryByText('K7MQ4TZP')).not.toBeInTheDocument()
  })

  it('does not show the teacher column to a teacher', async () => {
    list.mockResolvedValue(codes.slice(0, 1))
    renderAs('TEACHER')

    await screen.findByText('K7MQ4TZP')
    expect(screen.queryByRole('columnheader', { name: 'Docente' })).not.toBeInTheDocument()
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument()
  })

  it('copies an active code', async () => {
    list.mockResolvedValue(codes.slice(0, 1))
    const writeText = vi.fn(() => Promise.resolve())
    vi.stubGlobal('navigator', { ...navigator, clipboard: { writeText } })
    renderAs('TEACHER')

    await userEvent.click(await screen.findByRole('button', { name: 'Copiar el código K7MQ4TZP' }))

    expect(writeText).toHaveBeenCalledWith('K7MQ4TZP')
    expect(screen.getByText('Copiado')).toBeInTheDocument()
    vi.unstubAllGlobals()
  })

  it('deactivates a code after confirming', async () => {
    list.mockResolvedValue(codes.slice(0, 1))
    deactivate.mockResolvedValue(code({ id: 1, active: false }))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderAs('TEACHER')

    await userEvent.click(await screen.findByRole('button', { name: 'Desactivar el código K7MQ4TZP' }))

    expect(deactivate).toHaveBeenCalledWith(1)
    expect(await within(rowOf('K7MQ4TZP')).findByText('Desactivado')).toBeInTheDocument()
  })

  it('does nothing if the deactivation is not confirmed', async () => {
    list.mockResolvedValue(codes.slice(0, 1))
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderAs('TEACHER')

    await userEvent.click(await screen.findByRole('button', { name: 'Desactivar el código K7MQ4TZP' }))

    expect(deactivate).not.toHaveBeenCalled()
  })

  it('shows the API message when the deactivation fails', async () => {
    list.mockResolvedValue(codes.slice(0, 1))
    const data: ApiError = {
      timestamp: '',
      status: 403,
      error: 'Forbidden',
      message: 'No puede gestionar este código.',
      path: '',
      fieldErrors: {},
    }
    deactivate.mockRejectedValue(new AxiosError('403', 'ERR_BAD_REQUEST', undefined, null, { status: 403, data } as AxiosResponse))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderAs('TEACHER')

    await userEvent.click(await screen.findByRole('button', { name: 'Desactivar el código K7MQ4TZP' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No puede gestionar este código.')
  })

  it('offers to retry when the list cannot be loaded', async () => {
    list.mockRejectedValueOnce(new Error('Network Error'))
    list.mockResolvedValueOnce([])
    renderAs('TEACHER')

    expect(await screen.findByText('No fue posible comunicarse con el servidor')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /reintentar/i }))

    expect(await screen.findByText('Todavía no hay códigos de registro.')).toBeInTheDocument()
  })

  it('lets a teacher generate a code for a program of their faculty', async () => {
    list.mockResolvedValue(codes.slice(1, 2))
    create.mockResolvedValue(
      code({ id: 9, code: 'N3WC0DEX', programName: 'Ingeniería Electrónica', registeredCount: 0 }),
    )
    renderAs('TEACHER')

    await screen.findByText('B8WJ3NQA')
    await userEvent.click(screen.getByRole('button', { name: 'Generar código' }))
    const form = await screen.findByRole('form', { name: 'Generar código de registro' })
    expect(within(form).queryByLabelText('Docente')).not.toBeInTheDocument()
    await userEvent.selectOptions(within(form).getByLabelText('Programa'), 'Ingeniería Electrónica')
    await userEvent.type(within(form).getByLabelText('Vence el'), '2026-10-30')
    await userEvent.click(within(form).getByRole('button', { name: 'Generar' }))

    expect(create).toHaveBeenCalledWith({ programId: 2, expiresOn: '2026-10-30' })
    expect(teachers).not.toHaveBeenCalled()
    expect(await screen.findByRole('status')).toHaveTextContent('Se generó el código N3WC0DEX')
    expect(screen.queryByRole('form')).not.toBeInTheDocument()
    const rows = screen.getAllByRole('row')
    expect(rows[1]).toHaveTextContent('N3WC0DEX')
  })

  it('asks an admin for the teacher and only offers the active teachers of the faculty', async () => {
    list.mockResolvedValue([])
    create.mockResolvedValue(code({ id: 9, code: 'N3WC0DEX', teacherId: 20, teacherName: 'Rafael Ospino' }))
    renderAs('ADMIN')

    await screen.findByText('Todavía no hay códigos de registro.')
    await userEvent.click(screen.getByRole('button', { name: 'Generar código' }))
    const form = await screen.findByRole('form', { name: 'Generar código de registro' })
    const teacherSelect = within(form).getByLabelText('Docente')
    expect(within(teacherSelect).getAllByRole('option').map((option) => option.textContent)).toEqual([
      'Elige un docente',
      'Marta Díaz',
      'Rafael Ospino',
    ])
    expect(teachers).toHaveBeenCalledWith(3)

    await userEvent.selectOptions(teacherSelect, 'Rafael Ospino')
    await userEvent.selectOptions(within(form).getByLabelText('Programa'), 'Ingeniería de Sistemas')
    await userEvent.type(within(form).getByLabelText('Vence el'), '2026-10-30')
    await userEvent.click(within(form).getByRole('button', { name: 'Generar' }))

    expect(create).toHaveBeenCalledWith({ programId: 1, expiresOn: '2026-10-30', teacherId: 20 })
    expect(await screen.findByText('N3WC0DEX')).toBeInTheDocument()
  })

  it('checks the fields before generating a code', async () => {
    list.mockResolvedValue([])
    programs.mockResolvedValue([program(1, 'Ingeniería de Sistemas')])
    renderAs('ADMIN')

    await screen.findByText('Todavía no hay códigos de registro.')
    await userEvent.click(screen.getByRole('button', { name: 'Generar código' }))
    const form = await screen.findByRole('form', { name: 'Generar código de registro' })
    expect(within(form).getByLabelText('Programa')).toHaveValue('1')
    await userEvent.type(within(form).getByLabelText('Vence el'), '2026-09-28')
    await userEvent.click(within(form).getByRole('button', { name: 'Generar' }))

    expect(within(form).getByText('Elige el docente')).toBeInTheDocument()
    expect(within(form).getByText('La fecha de vencimiento no puede estar en el pasado')).toBeInTheDocument()
    expect(create).not.toHaveBeenCalled()
  })

  it('shows the API message when the code cannot be generated', async () => {
    list.mockResolvedValue([])
    create.mockRejectedValue(apiError(422, 'El programa no pertenece a la facultad del docente.'))
    renderAs('TEACHER')

    await screen.findByText('Todavía no hay códigos de registro.')
    await userEvent.click(screen.getByRole('button', { name: 'Generar código' }))
    const form = await screen.findByRole('form', { name: 'Generar código de registro' })
    await userEvent.selectOptions(within(form).getByLabelText('Programa'), 'Ingeniería de Sistemas')
    await userEvent.type(within(form).getByLabelText('Vence el'), '2026-10-30')
    await userEvent.click(within(form).getByRole('button', { name: 'Generar' }))

    expect(await within(form).findByRole('alert')).toHaveTextContent(
      'El programa no pertenece a la facultad del docente.',
    )
  })
})
