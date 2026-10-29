import { render, screen, within } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'

import { groupService } from '@/services/api/groupService'

import { groupFixture } from '../groups/testData'
import { GroupsTab } from './GroupsTab'
import { momentDetailFixture } from './testData'

vi.mock('@/services/api/groupService', () => ({ groupService: { list: vi.fn() } }))

describe('GroupsTab', () => {
  it('lists the groups of the moment with their members against the minimum', async () => {
    vi.mocked(groupService.list).mockResolvedValue([
      groupFixture({ id: 1, name: 'Grupo 1', projectTitle: 'SIERAP', memberCount: 5, full: true }),
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
})
