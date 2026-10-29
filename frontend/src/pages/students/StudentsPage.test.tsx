import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { apiError } from '@/pages/catalogs/testHelpers'
import { accountService } from '@/services/api/accountService'
import { catalogService } from '@/services/api/catalogService'
import { studentService } from '@/services/api/studentService'
import type { UserSummary } from '@/types/auth'
import type { Program } from '@/types/catalog'

import { StudentsPage } from './StudentsPage'

vi.mock('@/services/api/studentService', () => ({
  studentService: { list: vi.fn(), create: vi.fn(), setActive: vi.fn() },
}))
vi.mock('@/services/api/accountService', () => ({ accountService: { resendPasswordLink: vi.fn() } }))
vi.mock('@/services/api/catalogService', () => ({ catalogService: { programs: vi.fn() } }))

const service = vi.mocked(studentService)

function program(id: number, name: string): Program {
  return {
    id,
    code: String(id),
    name,
    facultyId: 4,
    facultyName: 'Facultad de Ingenierías y Tecnologías',
    currentPeriodMomentCount: 1,
    studentCount: 2,
  }
}

const systems = program(1, 'Ingeniería de Sistemas')
const electronics = program(2, 'Ingeniería Electrónica')

function student(id: number, firstNames: string, lastNames: string, overrides: Partial<UserSummary> = {}): UserSummary {
  return {
    id,
    firstNames,
    lastNames,
    fullName: `${firstNames} ${lastNames}`,
    email: `${firstNames.toLowerCase()}.${lastNames.toLowerCase()}@unicesar.edu.co`,
    role: 'STUDENT',
    active: true,
    mustChangePassword: false,
    ...overrides,
  }
}

const camila = student(1, 'Camila', 'Torres')
const andres = student(2, 'Andrés', 'Molina')

function renderPage() {
  render(
    <MemoryRouter>
      <StudentsPage />
    </MemoryRouter>,
  )
}

async function fillNewStudent(semester: string) {
  await userEvent.type(screen.getByLabelText('Nombres'), 'Valentina')
  await userEvent.type(screen.getByLabelText('Apellidos'), 'Rivas')
  await userEvent.type(screen.getByLabelText('Número de documento'), '1065000123')
  await userEvent.type(screen.getByLabelText('Correo'), 'valentina.rivas@unicesar.edu.co')
  await userEvent.type(screen.getByLabelText('Código estudiantil'), '20261068')
  await userEvent.type(screen.getByLabelText('Semestre'), semester)
}

describe('StudentsPage', () => {
  beforeEach(() => {
    Object.values(service).forEach((mock) => mock.mockReset())
    vi.mocked(accountService.resendPasswordLink).mockReset()
    vi.mocked(catalogService.programs).mockResolvedValue([systems, electronics])
    service.list.mockResolvedValue([camila, andres])
  })
  afterEach(() => vi.restoreAllMocks())

  it('lists the students of a program by last name, filters them and changes program', async () => {
    renderPage()

    await screen.findByRole('row', { name: /Camila Torres/ })
    expect(service.list).toHaveBeenCalledWith(1)
    const rows = screen.getAllByRole('row')
    expect(rows[1]).toHaveTextContent('Andrés Molina')
    expect(rows[2]).toHaveTextContent('Camila Torres')

    await userEvent.type(screen.getByLabelText('Buscar por nombre o correo'), 'torres')
    expect(screen.queryByRole('row', { name: /Andrés Molina/ })).not.toBeInTheDocument()

    service.list.mockResolvedValue([])
    await userEvent.selectOptions(screen.getByLabelText('Programa'), 'Ingeniería Electrónica')
    expect(service.list).toHaveBeenLastCalledWith(2)
    expect(await screen.findByText('Ingeniería Electrónica todavía no tiene estudiantes.')).toBeInTheDocument()
  })

  it('checks the fields and creates a student in the chosen program', async () => {
    service.create.mockResolvedValue(student(3, 'Valentina', 'Rivas'))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo estudiante' }))
    const form = screen.getByRole('form', { name: 'Nuevo estudiante' })
    await userEvent.click(within(form).getByRole('button', { name: 'Crear y enviar enlace' }))
    expect(within(form).getByText('El código estudiantil es obligatorio')).toBeInTheDocument()
    expect(within(form).getByText('El semestre es obligatorio')).toBeInTheDocument()

    await fillNewStudent('0')
    await userEvent.click(within(form).getByRole('button', { name: 'Crear y enviar enlace' }))
    expect(within(form).getByText('El semestre debe ser 1 o mayor')).toBeInTheDocument()
    expect(service.create).not.toHaveBeenCalled()

    await userEvent.clear(within(form).getByLabelText('Semestre'))
    await userEvent.type(within(form).getByLabelText('Semestre'), '1')
    await userEvent.click(within(form).getByRole('button', { name: 'Crear y enviar enlace' }))

    expect(service.create).toHaveBeenCalledWith({
      firstNames: 'Valentina',
      lastNames: 'Rivas',
      documentType: 'CC',
      documentNumber: '1065000123',
      email: 'valentina.rivas@unicesar.edu.co',
      programId: 1,
      studentCode: '20261068',
      currentSemester: 1,
    })
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Se creó a Valentina Rivas en Ingeniería de Sistemas.',
    )
    expect(screen.getByRole('row', { name: /Valentina Rivas/ })).toBeInTheDocument()
  })

  it('does not add to the list a student created in another program', async () => {
    service.create.mockResolvedValue(student(3, 'Valentina', 'Rivas'))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo estudiante' }))
    const form = screen.getByRole('form', { name: 'Nuevo estudiante' })
    await fillNewStudent('2')
    await userEvent.selectOptions(within(form).getByLabelText('Programa'), 'Ingeniería Electrónica')
    await userEvent.click(within(form).getByRole('button', { name: 'Crear y enviar enlace' }))

    expect(service.create).toHaveBeenCalledWith(expect.objectContaining({ programId: 2, currentSemester: 2 }))
    expect(await screen.findByRole('status')).toHaveTextContent('en Ingeniería Electrónica')
    expect(screen.queryByRole('row', { name: /Valentina Rivas/ })).not.toBeInTheDocument()
  })

  it('shows the message of the API when the student cannot be created', async () => {
    service.create.mockRejectedValue(apiError(409, 'Ya existe un estudiante con el código 20261068.'))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo estudiante' }))
    await fillNewStudent('1')
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un estudiante con el código 20261068.')
  })

  it('deactivates a student after confirming', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    service.setActive.mockResolvedValue({ ...camila, active: false })
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Desactivar a Camila Torres' }))

    expect(service.setActive).toHaveBeenCalledWith(1, false)
    const row = screen.getByRole('row', { name: /Camila Torres/ })
    expect(within(row).getByText('Desactivado')).toBeInTheDocument()
    expect(within(row).getByRole('button', { name: 'Reenviar enlace a Camila Torres' })).toBeDisabled()
  })

  it('shows the API message for a student the teacher does not manage', async () => {
    vi.mocked(accountService.resendPasswordLink).mockRejectedValue(
      apiError(403, 'Esta cuenta está fuera de su alcance.'),
    )
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Reenviar enlace a Andrés Molina' }))

    expect(accountService.resendPasswordLink).toHaveBeenCalledWith(2)
    expect(await screen.findByRole('alert')).toHaveTextContent('Esta cuenta está fuera de su alcance.')
  })

  it('says so when the faculty has no programs', async () => {
    vi.mocked(catalogService.programs).mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('Tu facultad todavía no tiene programas.')).toBeInTheDocument()
    expect(service.list).not.toHaveBeenCalled()
  })
})
