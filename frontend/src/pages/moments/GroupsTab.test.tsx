import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { catalogService } from '@/services/api/catalogService'
import { groupService } from '@/services/api/groupService'
import { teacherService } from '@/services/api/teacherService'
import type { UserSummary } from '@/types/auth'
import type { Program } from '@/types/catalog'

import { apiError } from '../catalogs/testHelpers'
import { groupFixture } from '../groups/testData'
import { GroupsTab } from './GroupsTab'
import { momentDetailFixture } from './testData'

vi.mock('@/services/api/groupService', () => ({
  groupService: { list: vi.fn(), reassign: vi.fn() },
}))
vi.mock('@/services/api/catalogService', () => ({
  catalogService: { programs: vi.fn() },
}))
vi.mock('@/services/api/teacherService', () => ({
  teacherService: { list: vi.fn() },
}))

function teacher(id: number, fullName: string, active = true): UserSummary {
  const [firstNames, lastNames] = fullName.split(' ')
  return {
    id,
    firstNames,
    lastNames,
    fullName,
    email: `${id}@unicesar.edu.co`,
    role: 'TEACHER',
    active,
    mustChangePassword: false,
  }
}

describe('GroupsTab', () => {
  beforeEach(() => {
    vi.resetAllMocks()
  })

  it('lists the groups of the moment with their members against the minimum', async () => {
    vi.mocked(groupService.list).mockResolvedValue([
      groupFixture({
        id: 1,
        name: 'Grupo 1',
        projectTitle: 'SIERAP',
        memberCount: 5,
        full: true,
      }),
      groupFixture({ id: 3, name: 'Grupo 3', memberCount: 2 }),
    ])
    render(<GroupsTab moment={momentDetailFixture()} />)

    const full = await screen.findByRole('row', { name: /Grupo 1/ })
    expect(within(full).getByText('5 / 5')).toBeInTheDocument()
    expect(within(full).getByText('Completo')).toBeInTheDocument()
    const below = screen.getByRole('row', { name: /Grupo 3/ })
    expect(within(below).getByText('Bajo el mínimo (3)')).toBeInTheDocument()
    expect(within(below).getByText('Sin título')).toBeInTheDocument()
    expect(groupService.list).toHaveBeenCalledWith(1)
  })

  it('passes a group to another active teacher of the faculty', async () => {
    const user = userEvent.setup()
    const moment = momentDetailFixture()
    vi.mocked(groupService.list).mockResolvedValue([groupFixture({ id: 1, name: 'Grupo 1' })])
    vi.mocked(catalogService.programs).mockResolvedValue([{ id: moment.programId, facultyId: 4 } as Program])
    vi.mocked(teacherService.list).mockResolvedValue([
      teacher(12, 'Marta Díaz'),
      teacher(20, 'Luis Gómez'),
      teacher(21, 'Ana Ruiz', false),
    ])
    vi.mocked(groupService.reassign).mockResolvedValue(
      groupFixture({
        id: 1,
        name: 'Grupo 1',
        teacher: { id: 20, fullName: 'Luis Gómez' },
      }),
    )
    render(<GroupsTab moment={moment} />)

    await user.click(await screen.findByRole('button', { name: 'Reasignar Grupo 1' }))
    const panel = await screen.findByRole('region', {
      name: 'Reasignar Grupo 1',
    })
    const select = await within(panel).findByRole('combobox', {
      name: 'Nuevo docente',
    })
    // Ni el docente actual ni los inactivos aparecen como opción.
    expect(
      within(select)
        .getAllByRole('option')
        .map((option) => option.textContent),
    ).toEqual(['Elige un docente', 'Luis Gómez'])
    expect(teacherService.list).toHaveBeenCalledWith(4)

    await user.selectOptions(select, '20')
    await user.click(within(panel).getByRole('button', { name: 'Reasignar' }))

    expect(groupService.reassign).toHaveBeenCalledWith(1, 20)
    const row = await screen.findByRole('row', { name: /Grupo 1/ })
    expect(within(row).getByText('Luis Gómez')).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Reasignar Grupo 1' })).not.toBeInTheDocument()
  })

  it('shows the message of the API when the group cannot be reassigned', async () => {
    const user = userEvent.setup()
    const moment = momentDetailFixture()
    vi.mocked(groupService.list).mockResolvedValue([groupFixture({ id: 1, name: 'Grupo 1' })])
    vi.mocked(catalogService.programs).mockResolvedValue([{ id: moment.programId, facultyId: 4 } as Program])
    vi.mocked(teacherService.list).mockResolvedValue([teacher(20, 'Luis Gómez')])
    vi.mocked(groupService.reassign).mockRejectedValue(
      apiError(422, 'Marta Díaz ya envió su evaluación del grupo: no se puede reasignar.'),
    )
    render(<GroupsTab moment={moment} />)

    await user.click(await screen.findByRole('button', { name: 'Reasignar Grupo 1' }))
    const panel = await screen.findByRole('region', {
      name: 'Reasignar Grupo 1',
    })
    await user.selectOptions(await within(panel).findByRole('combobox', { name: 'Nuevo docente' }), '20')
    await user.click(within(panel).getByRole('button', { name: 'Reasignar' }))

    expect(
      await within(panel).findByText('Marta Díaz ya envió su evaluación del grupo: no se puede reasignar.'),
    ).toBeInTheDocument()
  })
})
