import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { momentService } from '@/services/api/momentService'
import type { LaunchCheck } from '@/types/moment'

import { LaunchPanel } from './LaunchPanel'
import { momentDetailFixture } from './testData'

vi.mock('@/services/api/momentService', () => ({ momentService: { launchCheck: vi.fn(), launch: vi.fn() } }))

const launchCheck = vi.mocked(momentService.launchCheck)
const launch = vi.mocked(momentService.launch)
const onLaunched = vi.fn()

function checkFixture(overrides: Partial<LaunchCheck> = {}): LaunchCheck {
  return { momentId: 1, canLaunch: true, problems: [], minMembers: 3, groupsBelowMinimum: [],
    evaluatorTypesWithoutWeight: [], ...overrides }
}

function renderPanel() {
  render(<LaunchPanel moment={momentDetailFixture()} onCancel={vi.fn()} onLaunched={onLaunched} />)
}

describe('LaunchPanel', () => {
  beforeEach(() => {
    launchCheck.mockReset()
    launch.mockReset()
    onLaunched.mockReset()
  })

  it('lists every blocker and does not let the admin launch', async () => {
    launchCheck.mockResolvedValue(checkFixture({ canLaunch: false, problems: [
      'Para lanzar el momento se deben definir las tres ventanas de fechas.',
      'Los pesos deben sumar 100 % (hoy suman 70 %).',
    ] }))
    renderPanel()

    expect(await screen.findByRole('alert')).toHaveTextContent('las tres ventanas de fechas')
    expect(screen.getByText('Los pesos deben sumar 100 % (hoy suman 70 %).')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Lanzar momento' })).toBeDisabled()
  })

  it('asks to confirm the groups below the minimum and warns about types without weight', async () => {
    launchCheck.mockResolvedValue(checkFixture({
      groupsBelowMinimum: [
        { id: 7, name: 'Grupo 3', teacher: { id: 12, fullName: 'Marta Díaz' }, memberCount: 2 },
        { id: 9, name: 'Grupo 7', teacher: { id: 12, fullName: 'Marta Díaz' }, memberCount: 1 },
      ],
      evaluatorTypesWithoutWeight: [{ evaluatorTypeId: 2, evaluatorTypeName: 'Egresado', evaluatorCount: 3 }],
    }))
    launch.mockResolvedValue(momentDetailFixture({ status: 'LAUNCHED' }))
    renderPanel()

    expect(await screen.findByText('2 grupos están por debajo del mínimo de 3 integrantes')).toBeInTheDocument()
    expect(screen.getByText(/Grupo 3 \(2 integrantes\), Grupo 7 \(1 integrante\)/)).toBeInTheDocument()
    expect(screen.getByText(/Egresado \(3\): su tipo no tiene peso/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Lanzar momento' })).toBeDisabled()

    await userEvent.click(screen.getByLabelText('Entiendo y quiero lanzar el momento.'))
    await userEvent.click(screen.getByRole('button', { name: 'Lanzar momento' }))

    expect(launch).toHaveBeenCalledWith(1, true)
    expect(onLaunched).toHaveBeenCalledWith(expect.objectContaining({ status: 'LAUNCHED' }))
  })

  it('launches without confirmation when every group reaches the minimum', async () => {
    launchCheck.mockResolvedValue(checkFixture())
    launch.mockResolvedValue(momentDetailFixture({ status: 'LAUNCHED' }))
    renderPanel()

    await userEvent.click(await screen.findByRole('button', { name: 'Lanzar momento' }))

    expect(launch).toHaveBeenCalledWith(1, false)
  })
})
