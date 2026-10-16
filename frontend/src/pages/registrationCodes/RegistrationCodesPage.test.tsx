import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { registrationCodeService } from '@/services/api/registrationCodeService'
import type { ApiError } from '@/types/api'
import type { UserRole, UserSummary } from '@/types/auth'
import type { RegistrationCode } from '@/types/registrationCode'

import { RegistrationCodesPage } from './RegistrationCodesPage'

vi.mock('@/services/api/registrationCodeService', () => ({
  registrationCodeService: { list: vi.fn(), create: vi.fn(), deactivate: vi.fn() },
}))

const list = vi.mocked(registrationCodeService.list)
const deactivate = vi.mocked(registrationCodeService.deactivate)

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
    deactivate.mockReset()
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
})
