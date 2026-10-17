import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { catalogService } from '@/services/api/catalogService'
import { momentService } from '@/services/api/momentService'

import { MomentDetailPage } from './MomentDetailPage'
import { momentDetailFixture, periodFixture } from './testData'

vi.mock('@/services/api/catalogService', () => ({ catalogService: { periods: vi.fn() } }))
vi.mock('@/services/api/momentService', () => ({
  momentService: { get: vi.fn(), update: vi.fn(), remove: vi.fn(), launchCheck: vi.fn(), launch: vi.fn() },
}))

const get = vi.mocked(momentService.get)
const update = vi.mocked(momentService.update)
const remove = vi.mocked(momentService.remove)

function renderPage(path = '/moments/1') {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/moments/:id" element={<MomentDetailPage />} />
        <Route path="/moments/:id/rubric" element={<MomentDetailPage tab="rubric" />} />
        <Route path="/moments" element={<p>Lista de momentos</p>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('MomentDetailPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 8, 25, 10, 0))
    vi.mocked(catalogService.periods).mockResolvedValue([periodFixture()])
    get.mockReset()
    update.mockReset()
    remove.mockReset()
  })
  afterEach(() => {
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('shows the moment with the step of today and saves its data and windows', async () => {
    get.mockResolvedValue(momentDetailFixture())
    update.mockResolvedValue(momentDetailFixture({ name: 'Momento 1 · Proyecto' }))
    renderPage()

    expect(await screen.findByRole('heading', { name: 'Momento 1 · Ingeniería de Sistemas' })).toBeInTheDocument()
    const step = screen.getByText('Conformación', { selector: 'span' }).closest('li')
    expect(step).toHaveAttribute('aria-current', 'step')
    expect(screen.getByText('Límite UPC: 28/11/2026')).toBeInTheDocument()

    const name = screen.getByLabelText('Nombre')
    await userEvent.clear(name)
    await userEvent.type(name, 'Momento 1 · Proyecto')
    await userEvent.clear(screen.getByLabelText('Fin · Calificación'))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))
    expect(screen.getByText('Pon las dos fechas de la ventana o deja ambas vacías')).toBeInTheDocument()
    expect(update).not.toHaveBeenCalled()

    await userEvent.clear(screen.getByLabelText('Inicio · Calificación'))
    await userEvent.click(screen.getByRole('button', { name: 'Guardar cambios' }))

    expect(update).toHaveBeenCalledWith(1, expect.objectContaining({
      name: 'Momento 1 · Proyecto', gradingStart: null, gradingEnd: null, submissionStart: '2026-10-01',
    }))
    expect(await screen.findByRole('status')).toHaveTextContent('Se guardaron los cambios del momento.')
  })

  it('is read only once the moment was launched', async () => {
    get.mockResolvedValue(momentDetailFixture({ status: 'LAUNCHED' }))
    renderPage()

    expect(await screen.findByRole('note')).toHaveTextContent('El momento ya fue lanzado')
    expect(screen.getByLabelText('Nombre')).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Guardar cambios' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Eliminar momento' })).not.toBeInTheDocument()
  })

  it('deletes a moment without groups after confirming', async () => {
    get.mockResolvedValue(momentDetailFixture())
    remove.mockResolvedValue()
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Eliminar momento' }))

    expect(remove).toHaveBeenCalledWith(1)
    expect(await screen.findByText('Lista de momentos')).toBeInTheDocument()
  })

  it('does not offer to delete a moment with groups', async () => {
    get.mockResolvedValue(momentDetailFixture({ groupCount: 2 }))
    renderPage()

    expect(await screen.findByRole('button', { name: 'Eliminar momento' })).toBeDisabled()
    expect(screen.getByText(/ya habilitaron 2 grupos/)).toBeInTheDocument()
  })
  it('shows the rubric in its tab', async () => {
    get.mockResolvedValue(momentDetailFixture())
    renderPage('/moments/1/rubric')

    const tab = await screen.findByRole('link', { name: 'RAP, criterios y niveles' })
    expect(tab).toHaveAttribute('aria-current', 'page')
    expect(screen.getByRole('link', { name: 'Datos y ventanas' })).toHaveAttribute('href', '/moments/1')
    expect(screen.getByText(/El momento todavía no tiene RAP/)).toBeInTheDocument()
    expect(screen.queryByLabelText('Nombre')).not.toBeInTheDocument()
  })
  it('launches the moment from its header and locks the configuration', async () => {
    get.mockResolvedValue(momentDetailFixture())
    vi.mocked(momentService.launchCheck).mockResolvedValue({ momentId: 1, canLaunch: true, problems: [],
      minMembers: 3, groupsBelowMinimum: [], evaluatorTypesWithoutWeight: [] })
    vi.mocked(momentService.launch).mockResolvedValue(momentDetailFixture({ status: 'LAUNCHED' }))
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Lanzar momento' }))
    const panel = screen.getByRole('region', { name: 'Lanzar Momento 1' })
    await userEvent.click(await within(panel).findByRole('button', { name: 'Lanzar momento' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Se lanzó el momento')
    expect(screen.getByRole('note')).toHaveTextContent('El momento ya fue lanzado')
    expect(screen.getByLabelText('Nombre')).toBeDisabled()
    expect(screen.queryByRole('button', { name: 'Lanzar momento' })).not.toBeInTheDocument()
  })
})
