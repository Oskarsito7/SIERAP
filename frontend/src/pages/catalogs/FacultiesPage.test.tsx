import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { catalogService } from '@/services/api/catalogService'

import { programFixture } from '../moments/testData'
import { FacultiesPage } from './FacultiesPage'
import { apiError } from './testHelpers'

vi.mock('@/services/api/catalogService', () => ({
  catalogService: {
    faculties: vi.fn(), createFaculty: vi.fn(), updateFaculty: vi.fn(), deleteFaculty: vi.fn(),
    programs: vi.fn(), createProgram: vi.fn(), updateProgram: vi.fn(), deleteProgram: vi.fn(),
  },
}))

const service = vi.mocked(catalogService)
const engineering = { id: 1, code: '04', name: 'Facultad de Ingenierías y Tecnologías', programCount: 2 }
const health = { id: 2, code: '02', name: 'Facultad de Ciencias de la Salud', programCount: 0 }

function renderPage() {
  render(
    <MemoryRouter>
      <FacultiesPage />
    </MemoryRouter>,
  )
}

describe('FacultiesPage', () => {
  beforeEach(() => {
    Object.values(service).forEach((mock) => mock.mockReset())
    service.faculties.mockResolvedValue([health, engineering])
    service.programs.mockImplementation(async (facultyId) => facultyId === 2 ? [] : [
      programFixture({ studentCount: 214 }),
      programFixture({ id: 2, code: '111', name: 'Ingeniería Electrónica', currentPeriodMomentCount: 0 }),
    ])
  })
  afterEach(() => vi.restoreAllMocks())

  it('shows the programs of the chosen faculty', async () => {
    renderPage()

    expect(await screen.findByText('Esta facultad todavía no tiene programas.')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: /Facultad de Ingenierías y Tecnologías/ }))

    const systems = await screen.findByRole('row', { name: /Ingeniería de Sistemas/ })
    expect(within(systems).getByText('108')).toBeInTheDocument()
    expect(within(systems).getByText('214')).toBeInTheDocument()
    expect(service.programs).toHaveBeenLastCalledWith(1)
  })

  it('creates a program in the faculty and counts it', async () => {
    service.createProgram.mockResolvedValue(programFixture({ id: 9, code: '117', name: 'Ingeniería Agroindustrial' }))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo programa' }))
    const form = screen.getByRole('form', { name: /Nuevo programa/ })
    await userEvent.type(within(form).getByLabelText('Código'), '117')
    await userEvent.type(within(form).getByLabelText('Nombre'), 'Ingeniería Agroindustrial')
    await userEvent.click(within(form).getByRole('button', { name: 'Crear programa' }))

    expect(service.createProgram).toHaveBeenCalledWith(2, { code: '117', name: 'Ingeniería Agroindustrial' })
    expect(await screen.findByRole('row', { name: /Ingeniería Agroindustrial/ })).toBeInTheDocument()
    expect(screen.getByText('Código 02 · 1 programa')).toBeInTheDocument()
  })

  it('creates a faculty and selects it', async () => {
    service.createFaculty.mockResolvedValue({ id: 3, code: '01', name: 'Facultad de Derecho', programCount: 0 })
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Nueva facultad' }))
    const form = screen.getByRole('form', { name: 'Nueva facultad' })
    await userEvent.click(within(form).getByRole('button', { name: 'Crear facultad' }))
    expect(within(form).getByText('El código es obligatorio')).toBeInTheDocument()

    await userEvent.type(within(form).getByLabelText('Código'), '01')
    await userEvent.type(within(form).getByLabelText('Nombre'), 'Facultad de Derecho')
    await userEvent.click(within(form).getByRole('button', { name: 'Crear facultad' }))

    expect(service.createFaculty).toHaveBeenCalledWith({ code: '01', name: 'Facultad de Derecho' })
    expect(await screen.findByText('Facultad de Derecho', { selector: '[data-slot="card-title"]' }))
      .toBeInTheDocument()
  })

  it('explains why a program cannot be deleted', async () => {
    service.faculties.mockResolvedValue([engineering])
    service.deleteProgram.mockRejectedValue(apiError(409, 'Tiene momentos con grupos o calificaciones.'))
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Eliminar Ingeniería de Sistemas' }))

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'No se puede eliminar Ingeniería de Sistemas: Tiene momentos con grupos o calificaciones.')
    expect(screen.getByRole('row', { name: /Ingeniería de Sistemas/ })).toBeInTheDocument()
  })
})
