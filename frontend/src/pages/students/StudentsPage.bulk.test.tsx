import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { AuthContext } from '@/auth/authContext'
import { saveFile } from '@/lib/files'
import { apiError } from '@/pages/catalogs/testHelpers'
import { catalogService } from '@/services/api/catalogService'
import { momentService } from '@/services/api/momentService'
import { studentService } from '@/services/api/studentService'
import { userService } from '@/services/api/userService'
import { answerConfirm } from '@/test/confirm'
import type { Student, StudentImportPreview } from '@/types/account'
import type { UserSummary } from '@/types/auth'
import type { Program } from '@/types/catalog'

import { StudentsPage } from './StudentsPage'

vi.mock('@/services/api/studentService', () => ({
  studentService: {
    list: vi.fn(),
    listPage: vi.fn(),
    create: vi.fn(),
    setActive: vi.fn(),
    setSemester: vi.fn(),
    previewImport: vi.fn(),
    importStudents: vi.fn(),
  },
}))
vi.mock('@/services/api/userService', () => ({ userService: { update: vi.fn() } }))
vi.mock('@/services/api/accountService', () => ({ accountService: { resendPasswordLink: vi.fn() } }))
vi.mock('@/services/api/catalogService', () => ({ catalogService: { programs: vi.fn() } }))
vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn() } }))
vi.mock('@/lib/files', async (importOriginal) => ({
  ...(await importOriginal<typeof import('@/lib/files')>()),
  saveFile: vi.fn(),
}))

const service = vi.mocked(studentService)

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
    documentType: 'TI',
    documentNumber: `10${id}`,
    studentCode: `2026${id}`,
    currentSemester: 2,
    registrationCode: null,
    createdBy: { id: 12, fullName: 'Marta Díaz' },
    manageable: true,
    ...overrides,
  }
}

const camila = student(1, 'Camila', 'Torres')
const andres = student(2, 'Andrés', 'Molina')
const luis = student(3, 'Luis', 'Pérez', { manageable: false })

function renderPage() {
  render(
    <AuthContext value={{ state: { status: 'authenticated', user: teacher }, user: teacher, logout: vi.fn() }}>
      <MemoryRouter>
        <StudentsPage />
      </MemoryRouter>
    </AuthContext>,
  )
}

function csvFile(name = 'estudiantes.csv') {
  return new File(['nombres,apellidos\nAna,Pérez\n'], name, { type: 'text/csv' })
}

const previewWithErrors: StudentImportPreview = {
  valid: 2,
  invalid: 1,
  rows: [
    {
      row: 2, firstNames: 'Ana', lastNames: 'Pérez', documentType: 'CC', documentNumber: '1001',
      email: 'ana@unicesar.edu.co', studentCode: 'A1', currentSemester: '3', errors: [],
    },
    {
      row: 3, firstNames: 'Luis', lastNames: 'Gómez', documentType: 'XX', documentNumber: '1002',
      email: 'luis@unicesar.edu.co', studentCode: 'A2', currentSemester: '0',
      errors: ['El tipo de documento «XX» no es válido (use CC, TI o CE).', 'El semestre debe ser 1 o mayor.'],
    },
    {
      row: 4, firstNames: 'Eva', lastNames: 'Ruiz', documentType: 'CE', documentNumber: '1003',
      email: 'eva@unicesar.edu.co', studentCode: 'A3', currentSemester: '5', errors: [],
    },
  ],
}

describe('StudentsPage: edit, bulk semester and CSV upload', () => {
  beforeEach(() => {
    Object.values(service).forEach((mock) => mock.mockReset())
    vi.mocked(userService.update).mockReset()
    vi.mocked(saveFile).mockReset()
    vi.mocked(catalogService.programs).mockResolvedValue([systems, electronics])
    vi.mocked(momentService.list).mockResolvedValue([])
    service.listPage.mockResolvedValue({ items: [camila, andres, luis], total: 3 })
  })
  afterEach(() => vi.restoreAllMocks())

  describe('editing a student', () => {
    it('opens the form with the current data and only sends what changed', async () => {
      vi.mocked(userService.update).mockResolvedValue({ ...camila, email: 'camila.nueva@unicesar.edu.co' })
      renderPage()

      await userEvent.click(await screen.findByRole('button', { name: 'Editar a Camila Torres' }))
      expect(screen.getByLabelText('Nombres')).toHaveValue('Camila')
      expect(screen.getByLabelText('Número de documento')).toHaveValue('101')
      expect(screen.getByLabelText('Código estudiantil')).toHaveValue('20261')
      expect(screen.getByLabelText('Semestre')).toHaveValue(2)
      expect(screen.queryByText(/se cerrarán las sesiones/)).not.toBeInTheDocument()

      await userEvent.clear(screen.getByLabelText('Correo'))
      await userEvent.type(screen.getByLabelText('Correo'), 'camila.nueva@unicesar.edu.co')
      expect(screen.getByRole('status')).toHaveTextContent(
        'Al cambiar el correo se cerrarán las sesiones abiertas de esta cuenta y deberá iniciar sesión con el correo nuevo.',
      )
      await userEvent.clear(screen.getByLabelText('Semestre'))
      await userEvent.type(screen.getByLabelText('Semestre'), '5')
      await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

      expect(userService.update).toHaveBeenCalledWith(1, {
        email: 'camila.nueva@unicesar.edu.co',
        currentSemester: 5,
      })
      expect(await screen.findByText(/Se actualizaron los datos de Camila Torres\./)).toBeInTheDocument()
      expect(screen.getByText(/deberá iniciar sesión con el correo nuevo/)).toBeInTheDocument()
      expect(service.listPage).toHaveBeenCalledTimes(2)
      expect(screen.queryByRole('form', { name: 'Editar a Camila Torres' })).not.toBeInTheDocument()
    })

    it('does not offer to edit students the user cannot manage', async () => {
      renderPage()

      await screen.findByRole('row', { name: /Luis Pérez/ })
      expect(screen.getByRole('button', { name: 'Editar a Camila Torres' })).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: 'Editar a Luis Pérez' })).not.toBeInTheDocument()
    })

    it('asks for changes before saving and shows the errors of the API', async () => {
      vi.mocked(userService.update).mockRejectedValue(
        apiError(422, 'Ya existe un estudiante con el código 20262.'),
      )
      renderPage()

      await userEvent.click(await screen.findByRole('button', { name: 'Editar a Camila Torres' }))
      await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))
      expect(screen.getByRole('alert')).toHaveTextContent('No hay cambios para guardar.')
      expect(userService.update).not.toHaveBeenCalled()

      await userEvent.clear(screen.getByLabelText('Código estudiantil'))
      await userEvent.type(screen.getByLabelText('Código estudiantil'), '20262')
      await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

      expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe un estudiante con el código 20262.')
    })
  })

  describe('setting the semester of several students', () => {
    it('selects manageable students, confirms and reloads the list', async () => {
      service.setSemester.mockResolvedValue(2)
      renderPage()

      await userEvent.click(await screen.findByLabelText('Seleccionar a Camila Torres'))
      await userEvent.click(screen.getByLabelText('Seleccionar a Andrés Molina'))
      expect(screen.queryByLabelText('Seleccionar a Luis Pérez')).not.toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Fijar semestre' })).toBeDisabled()

      await userEvent.type(screen.getByLabelText('Semestre para los seleccionados'), '4')
      await userEvent.click(screen.getByRole('button', { name: 'Fijar semestre' }))
      expect(await screen.findByRole('alertdialog')).toHaveTextContent('¿Fijar el semestre 4 a 2 estudiantes?')
      await answerConfirm('Fijar semestre')

      expect(service.setSemester).toHaveBeenCalledWith({ studentIds: [2, 1], currentSemester: 4 })
      expect(await screen.findByText('Se actualizó el semestre de 2 estudiantes.')).toBeInTheDocument()
      expect(service.listPage).toHaveBeenCalledTimes(2)
      expect(screen.getByLabelText('Seleccionar a Camila Torres')).not.toBeChecked()
    })

    it('selects every visible manageable student at once', async () => {
      renderPage()

      await userEvent.click(await screen.findByLabelText('Seleccionar todos los visibles'))
      expect(screen.getByLabelText('Seleccionar a Camila Torres')).toBeChecked()
      expect(screen.getByLabelText('Seleccionar a Andrés Molina')).toBeChecked()
      expect(screen.getByText('2 estudiantes seleccionados')).toBeInTheDocument()

      await userEvent.click(screen.getByLabelText('Seleccionar todos los visibles'))
      expect(screen.getByText('0 estudiantes seleccionados')).toBeInTheDocument()
    })

    it('does nothing when the confirmation is cancelled and shows API errors', async () => {
      service.setSemester.mockRejectedValue(apiError(403, 'Esta cuenta está fuera de su alcance.'))
      renderPage()

      await userEvent.click(await screen.findByLabelText('Seleccionar a Camila Torres'))
      await userEvent.type(screen.getByLabelText('Semestre para los seleccionados'), '3')
      await userEvent.click(screen.getByRole('button', { name: 'Fijar semestre' }))
      await answerConfirm('Cancelar')
      expect(service.setSemester).not.toHaveBeenCalled()

      await userEvent.click(screen.getByRole('button', { name: 'Fijar semestre' }))
      await answerConfirm('Fijar semestre')
      expect(await screen.findByRole('alert')).toHaveTextContent('Esta cuenta está fuera de su alcance.')
    })

    it('rejects a semester that is not a positive integer without calling the API', async () => {
      renderPage()

      await userEvent.click(await screen.findByLabelText('Seleccionar a Camila Torres'))
      await userEvent.type(screen.getByLabelText('Semestre para los seleccionados'), '0')
      await userEvent.click(screen.getByRole('button', { name: 'Fijar semestre' }))

      expect(screen.getByRole('alert')).toHaveTextContent('El semestre debe ser un número entero de 1 o mayor.')
      expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
      expect(service.setSemester).not.toHaveBeenCalled()
    })
  })

  describe('CSV upload', () => {
    it('previews the file, shows the errors per row and then creates the valid ones', async () => {
      service.previewImport.mockResolvedValue(previewWithErrors)
      service.importStudents.mockResolvedValue({ created: 2, errors: [] })
      renderPage()

      await userEvent.click(await screen.findByRole('button', { name: 'Cargar estudiantes (CSV)' }))
      const file = csvFile()
      await userEvent.upload(screen.getByLabelText('Archivo CSV'), file)
      await userEvent.click(screen.getByRole('button', { name: 'Revisar archivo' }))

      expect(service.previewImport).toHaveBeenCalledWith(1, file)
      expect(await screen.findByText('2 válidas, 1 con errores')).toBeInTheDocument()
      const bad = screen.getByRole('row', { name: /Luis Gómez/ })
      expect(bad).toHaveTextContent('El tipo de documento «XX» no es válido (use CC, TI o CE).')
      expect(bad).toHaveTextContent('El semestre debe ser 1 o mayor.')
      expect(within(screen.getByRole('row', { name: /Ana Pérez/ })).getByText('Válida')).toBeInTheDocument()
      expect(service.importStudents).not.toHaveBeenCalled()

      await userEvent.click(screen.getByRole('button', { name: 'Crear 2 estudiantes' }))

      expect(service.importStudents).toHaveBeenCalledWith(1, file)
      expect(await screen.findByText(
        'Se crearon 2 estudiantes. Cada uno recibirá un correo para crear su contraseña.',
      )).toBeInTheDocument()
      expect(service.listPage).toHaveBeenCalledTimes(2)
    })

    it('uses the program chosen in the page and lets the user pick another one', async () => {
      service.previewImport.mockResolvedValue(previewWithErrors)
      renderPage()

      await userEvent.click(await screen.findByRole('button', { name: 'Cargar estudiantes (CSV)' }))
      expect(screen.getByLabelText('Programa de los estudiantes')).toHaveValue('1')
      await userEvent.selectOptions(screen.getByLabelText('Programa de los estudiantes'), '2')
      const file = csvFile()
      await userEvent.upload(screen.getByLabelText('Archivo CSV'), file)
      await userEvent.click(screen.getByRole('button', { name: 'Revisar archivo' }))

      expect(service.previewImport).toHaveBeenCalledWith(2, file)
    })

    it('disables the creation when no row is valid', async () => {
      service.previewImport.mockResolvedValue({
        valid: 0,
        invalid: 1,
        rows: [{ ...previewWithErrors.rows[1] }],
      })
      renderPage()

      await userEvent.click(await screen.findByRole('button', { name: 'Cargar estudiantes (CSV)' }))
      await userEvent.upload(screen.getByLabelText('Archivo CSV'), csvFile())
      await userEvent.click(screen.getByRole('button', { name: 'Revisar archivo' }))

      expect(await screen.findByText('0 válidas, 1 con errores')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Crear 0 estudiantes' })).toBeDisabled()
    })

    it('asks for a file and shows the errors of the whole file', async () => {
      service.previewImport.mockRejectedValue(apiError(422, 'El archivo no tiene estudiantes.'))
      renderPage()

      await userEvent.click(await screen.findByRole('button', { name: 'Cargar estudiantes (CSV)' }))
      await userEvent.click(screen.getByRole('button', { name: 'Revisar archivo' }))
      expect(screen.getByText('Elige el archivo CSV con los estudiantes.')).toBeInTheDocument()
      expect(service.previewImport).not.toHaveBeenCalled()

      await userEvent.upload(screen.getByLabelText('Archivo CSV'), csvFile())
      await userEvent.click(screen.getByRole('button', { name: 'Revisar archivo' }))
      expect(await screen.findByRole('alert')).toHaveTextContent('El archivo no tiene estudiantes.')
    })

    it('reports the rows that were not created', async () => {
      service.previewImport.mockResolvedValue(previewWithErrors)
      service.importStudents.mockResolvedValue({
        created: 1,
        errors: [{ row: 3, message: 'Ya existe una cuenta con el correo luis@unicesar.edu.co.' }],
      })
      renderPage()

      await userEvent.click(await screen.findByRole('button', { name: 'Cargar estudiantes (CSV)' }))
      await userEvent.upload(screen.getByLabelText('Archivo CSV'), csvFile())
      await userEvent.click(screen.getByRole('button', { name: 'Revisar archivo' }))
      await userEvent.click(await screen.findByRole('button', { name: 'Crear 2 estudiantes' }))

      expect(await screen.findByText(/Se creó 1 estudiante\./)).toBeInTheDocument()
      expect(screen.getByRole('alert')).toHaveTextContent(
        'Fila 3: Ya existe una cuenta con el correo luis@unicesar.edu.co.',
      )
    })

    it('downloads the template generated in the browser', async () => {
      renderPage()

      await userEvent.click(await screen.findByRole('button', { name: 'Cargar estudiantes (CSV)' }))
      await userEvent.click(screen.getByRole('button', { name: 'Descargar plantilla' }))

      expect(saveFile).toHaveBeenCalledWith(expect.any(Blob), 'plantilla_estudiantes.csv')
    })
  })
})
