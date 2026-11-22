import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import { catalogService } from '@/services/api/catalogService'
import { momentService } from '@/services/api/momentService'
import type { ApiError } from '@/types/api'

import { momentFixture } from '../groups/testData'
import { MomentsPage } from './MomentsPage'
import { periodFixture, programFixture } from './testData'

vi.mock('@/services/api/catalogService', () => ({
  catalogService: { periods: vi.fn(), programs: vi.fn(), faculties: vi.fn() },
}))
vi.mock('@/services/api/momentService', () => ({ momentService: { list: vi.fn(), copy: vi.fn() } }))

const list = vi.mocked(momentService.list)
const copy = vi.mocked(momentService.copy)

const systems = programFixture()
const electronics = programFixture({ id: 2, name: 'Ingeniería Electrónica' })

function Location() {
  const location = useLocation()
  return <p data-testid="location">{location.pathname + location.search}</p>
}

function renderPage(path = '/moments') {
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/moments" element={<><MomentsPage /><Location /></>} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('MomentsPage', () => {
  beforeEach(() => {
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 8, 25, 10, 0))
    vi.mocked(catalogService.periods).mockResolvedValue([
      periodFixture({ id: 2, code: '2027-1', status: 'UPCOMING' }),
      periodFixture(),
    ])
    vi.mocked(catalogService.programs).mockResolvedValue([systems, electronics])
    vi.mocked(catalogService.faculties).mockResolvedValue([
      { id: 1, code: '04', name: 'Facultad de Ingenierías y Tecnologías', programCount: 2 },
    ])
    list.mockReset()
    copy.mockReset()
    list.mockResolvedValue([
      momentFixture({ groupCount: 9 }),
      momentFixture({ id: 3, name: 'Momento 1', subject: 'Circuitos I', programId: 2,
        programName: 'Ingeniería Electrónica', status: 'LAUNCHED', groupCount: 7 }),
    ])
  })
  afterEach(() => vi.useRealTimers())

  it('lists the moments of the current period with their phase of today', async () => {
    renderPage()

    expect(await screen.findByText('Facultad de Ingenierías y Tecnologías')).toBeInTheDocument()
    const systemsRow = await screen.findByRole('row', { name: /Ingeniería de Sistemas/ })
    expect(list).toHaveBeenCalledWith(1)
    expect(within(systemsRow).getByText('Configuración')).toBeInTheDocument()
    expect(within(systemsRow).getByText('Conformación de grupos abierta')).toBeInTheDocument()
    expect(within(systemsRow).getByText('hasta el 30/09/2026')).toBeInTheDocument()
    expect(within(systemsRow).getByRole('link', { name: 'Configurar Momento 1 de Ingeniería de Sistemas' }))
      .toHaveAttribute('href', '/moments/1')

    const electronicsRow = screen.getByRole('row', { name: /Ingeniería Electrónica/ })
    expect(within(electronicsRow).getByText('Lanzado')).toBeInTheDocument()
    expect(within(electronicsRow).getByText('Entregas')).toBeInTheDocument()
    expect(within(electronicsRow).getByRole('link', { name: /^Ver/ })).toHaveAttribute('href', '/moments/3')
    expect(screen.getByRole('link', { name: 'Nuevo momento' })).toHaveAttribute('href', '/moments/new?periodId=1')
  })

  it('filters by program and changes the period through the address', async () => {
    renderPage()

    await userEvent.selectOptions(await screen.findByLabelText('Programa'), '2')
    expect(screen.queryByRole('row', { name: /Ingeniería de Sistemas/ })).not.toBeInTheDocument()
    expect(screen.getByTestId('location')).toHaveTextContent('/moments?periodId=1&programId=2')

    await userEvent.selectOptions(screen.getByLabelText('Periodo'), '2')
    await waitFor(() => expect(list).toHaveBeenLastCalledWith(2))
  })

  it('copies the configuration of another period and reloads the list', async () => {
    copy.mockResolvedValue([momentFixture(), momentFixture({ id: 4 })])
    renderPage('/moments?periodId=2')

    await userEvent.click(await screen.findByRole('button', { name: 'Copiar de otro periodo' }))
    expect(screen.getByText('Copiar la configuración al periodo 2027-1')).toBeInTheDocument()
    await userEvent.selectOptions(screen.getByLabelText('Programas'), '1')
    await userEvent.click(screen.getByRole('button', { name: 'Copiar' }))

    expect(copy).toHaveBeenCalledWith({ fromPeriodId: 1, toPeriodId: 2, programId: 1 })
    expect(await screen.findByRole('status')).toHaveTextContent('Se copiaron 2 momentos al periodo 2027-1.')
    await waitFor(() => expect(list).toHaveBeenCalledTimes(2))
  })

  it('shows the message of the API when nothing can be copied', async () => {
    const data: ApiError = { timestamp: '', status: 422, error: '', path: '', fieldErrors: {},
      message: 'El periodo destino ya tiene el Momento 1 de Ingeniería de Sistemas.' }
    const response = { status: 422, data } as AxiosResponse
    copy.mockRejectedValue(new AxiosError('', 'ERR_BAD_REQUEST', undefined, null, response))
    renderPage('/moments?periodId=2')

    await userEvent.click(await screen.findByRole('button', { name: 'Copiar de otro periodo' }))
    await userEvent.click(screen.getByRole('button', { name: 'Copiar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('ya tiene el Momento 1')
  })
})
