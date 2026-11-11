import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { apiError } from '@/pages/catalogs/testHelpers'
import { momentFixture } from '@/pages/groups/testData'
import { accountService } from '@/services/api/accountService'
import { catalogService } from '@/services/api/catalogService'
import { momentService } from '@/services/api/momentService'
import { studentService } from '@/services/api/studentService'
import type { Student } from '@/types/account'
import type { UserSummary } from '@/types/auth'
import type { Program } from '@/types/catalog'

import { StudentsPage } from './StudentsPage'

vi.mock('@/services/api/studentService', () => ({
  studentService: { list: vi.fn(), create: vi.fn(), setActive: vi.fn() },
}))
vi.mock('@/services/api/accountService', () => ({ accountService: { resendPasswordLink: vi.fn() } }))
vi.mock('@/services/api/catalogService', () => ({ catalogService: { programs: vi.fn() } }))
vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn() } }))

const service = vi.mocked(studentService)

/** La docente que inició sesión. */
const teacher: UserSummary = {
  id: 12,
  firstNames: 'Marta',
  lastNames: 'Díaz',
  fullName: 'Marta Díaz',
  email: 'marta.diaz@unicesar.edu.co',
  role: 'TEACHER',
  active: true,
  mustChangePassword: false,
}

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

function student(id: number, firstNames: string, lastNames: string, overrides: Partial<Student> = {}): Student {
  return {
    id,
    firstNames,
    lastNames,
    fullName: `${firstNames} ${lastNames}`,
    email: `${firstNames.toLowerCase()}.${lastNames.toLowerCase()}@unicesar.edu.co`,
    role: 'STUDENT',
    active: true,
    mustChangePassword: false,
    studentCode: `2026${id}`,
    currentSemester: 2,
    registrationCode: null,
    createdBy: { id: 12, fullName: 'Marta Díaz' },
    manageable: true,
    ...overrides,
  }
}

const camila = student(1, 'Camila', 'Torres', { studentCode: '20261053', registrationCode: 'K7MQ4TZP' })
const andres = student(2, 'Andrés', 'Molina', { studentCode: '20252210', currentSemester: 3 })
const luis = student(3, 'Luis', 'Pérez', { createdBy: { id: 30, fullName: 'Rafael Ospino' }, manageable: false })

function renderPage() {
  render(
    <AuthContext value={{ state: { status: 'authenticated', user: teacher }, user: teacher, logout: vi.fn() }}>
      <MemoryRouter>
        <StudentsPage />
      </MemoryRouter>
    </AuthContext>,
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
    vi.mocked(momentService.list).mockResolvedValue([
      momentFixture({ id: 1, name: 'Momento 1', programId: 1 }),
      momentFixture({ id: 9, name: 'Momento 1', programId: 2, programName: 'Ingeniería Electrónica' }),
    ])
    service.list.mockResolvedValue([camila, andres, luis])
  })
  afterEach(() => vi.restoreAllMocks())

  it('lists the students of a program with their code, semester and how they arrived', async () => {
    renderPage()

    const camilaRow = await screen.findByRole('row', { name: /Camila Torres/ })
    expect(service.list).toHaveBeenCalledWith(1, undefined)
    expect(camilaRow).toHaveTextContent('20261053')
    expect(within(camilaRow).getByText('Código K7MQ4TZP')).toBeInTheDocument()
    const andresRow = screen.getByRole('row', { name: /Andrés Molina/ })
    expect(within(andresRow).getByText('3')).toBeInTheDocument()
    expect(within(andresRow).getByText('Creado por mí')).toBeInTheDocument()
    const luisRow = screen.getByRole('row', { name: /Luis Pérez/ })
    expect(within(luisRow).getByText('Creado por Rafael Ospino')).toBeInTheDocument()

    const rows = screen.getAllByRole('row')
    expect(rows[1]).toHaveTextContent('Andrés Molina')
    expect(rows[3]).toHaveTextContent('Camila Torres')
  })

  it('hides the actions on the students the teacher does not manage', async () => {
    renderPage()

    const luisRow = await screen.findByRole('row', { name: /Luis Pérez/ })
    expect(within(luisRow).queryByRole('button')).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Desactivar a Camila Torres' })).toBeInTheDocument()
  })

  it('searches by name, code or email and changes program', async () => {
    renderPage()

    await screen.findByRole('row', { name: /Camila Torres/ })
    await userEvent.type(screen.getByLabelText('Buscar por nombre, código o correo'), '20252210')
    expect(screen.queryByRole('row', { name: /Camila Torres/ })).not.toBeInTheDocument()
    expect(screen.getByRole('row', { name: /Andrés Molina/ })).toBeInTheDocument()

    service.list.mockResolvedValue([])
    await userEvent.clear(screen.getByLabelText('Buscar por nombre, código o correo'))
    await userEvent.selectOptions(screen.getByLabelText('Programa'), 'Ingeniería Electrónica')
    expect(service.list).toHaveBeenLastCalledWith(2, undefined)
    expect(await screen.findByText('Ingeniería Electrónica todavía no tiene estudiantes.')).toBeInTheDocument()
  })

  it('shows only the students without a group in a moment of the program', async () => {
    renderPage()

    await screen.findByRole('row', { name: /Camila Torres/ })
    const filter = screen.getByLabelText('Mostrar')
    expect(within(filter).getAllByRole('option').map((option) => option.textContent)).toEqual([
      'Todos los estudiantes',
      'Sin grupo en Momento 1',
    ])

    service.list.mockResolvedValue([])
    await userEvent.selectOptions(filter, 'Sin grupo en Momento 1')

    expect(service.list).toHaveBeenLastCalledWith(1, 1)
    expect(await screen.findByText('No hay estudiantes sin grupo en Momento 1.')).toBeInTheDocument()
  })

  it('checks the fields and creates a student in the chosen program', async () => {
    service.create.mockResolvedValue(student(4, 'Valentina', 'Rivas', { studentCode: '20261068', currentSemester: 1 }))
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
    expect(screen.getByRole('row', { name: /Valentina Rivas/ })).toHaveTextContent('20261068')
  })

  it('does not add to the list a student created in another program', async () => {
    service.create.mockResolvedValue(student(4, 'Valentina', 'Rivas'))
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

  it('reloads the list after creating a student while the "without a group" filter is on', async () => {
    service.create.mockResolvedValue(student(4, 'Valentina', 'Rivas'))
    renderPage()

    await screen.findByRole('row', { name: /Camila Torres/ })
    await userEvent.selectOptions(screen.getByLabelText('Mostrar'), 'Sin grupo en Momento 1')
    const calls = service.list.mock.calls.length
    await userEvent.click(screen.getByRole('button', { name: 'Nuevo estudiante' }))
    await fillNewStudent('2')
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))

    await screen.findByRole('status')
    expect(service.list.mock.calls.length).toBe(calls + 1)
    expect(service.list).toHaveBeenLastCalledWith(1, 1)
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

  it('resends the password link', async () => {
    vi.mocked(accountService.resendPasswordLink).mockResolvedValue()
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Reenviar enlace a Andrés Molina' }))

    expect(accountService.resendPasswordLink).toHaveBeenCalledWith(2)
    expect(await screen.findByRole('status')).toHaveTextContent(
      'Se envió de nuevo el enlace a andrés.molina@unicesar.edu.co.',
    )
  })

  it('says so when the faculty has no programs', async () => {
    vi.mocked(catalogService.programs).mockResolvedValue([])
    renderPage()

    expect(await screen.findByText('Tu facultad todavía no tiene programas.')).toBeInTheDocument()
    expect(service.list).not.toHaveBeenCalled()
  })
})
