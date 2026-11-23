import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { auditService } from '@/services/api/auditService'
import type { AuditLogEntry, AuditLogPage as AuditPage } from '@/types/audit'

import { AuditLogPage } from './AuditLogPage'

vi.mock('@/services/api/auditService', () => ({ auditService: { list: vi.fn() } }))

const list = vi.mocked(auditService.list)

function entry(overrides: Partial<AuditLogEntry> = {}): AuditLogEntry {
  return {
    id: 1,
    createdAt: '2026-09-28T14:30:00',
    actorId: 5,
    actorName: 'Carlos Ramos',
    action: 'ACCOUNT_UPDATED',
    entityType: 'User',
    entityId: 9,
    facultyId: 1,
    summary: 'Cambió el correo de Ana Pérez de a@unicesar.edu.co a b@unicesar.edu.co.',
    ip: '10.0.0.1',
    ...overrides,
  }
}

function page(items: AuditLogEntry[], overrides: Partial<AuditPage> = {}): AuditPage {
  return { items, total: items.length, page: 0, size: 50, ...overrides }
}

describe('AuditLogPage', () => {
  beforeEach(() => list.mockReset())

  it('shows the entries with the date, the actor, the action in Spanish and the summary', async () => {
    list.mockResolvedValue(page([entry(), entry({ id: 2, actorName: null, action: 'MOMENT_EXTENDED', summary: 'Prorrogó.' })]))
    render(<AuditLogPage />)

    expect(await screen.findByText('Cambió el correo de Ana Pérez de a@unicesar.edu.co a b@unicesar.edu.co.')).toBeInTheDocument()
    expect(screen.getAllByText('28/09/2026 14:30', { selector: 'td' })).toHaveLength(2)
    expect(screen.getByText('Carlos Ramos')).toBeInTheDocument()
    expect(screen.getByText('Cuenta editada', { selector: 'span' })).toBeInTheDocument()
    expect(screen.getByText('Prórroga de momento', { selector: 'span' })).toBeInTheDocument()
    expect(screen.getByText('Sistema')).toBeInTheDocument()
    expect(screen.getByText('Página 1 de 1 · 2 registros')).toBeInTheDocument()
    expect(list).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 50, action: undefined, q: '' }))
  })

  it('sends the filters and goes back to the first page', async () => {
    list.mockResolvedValue(page([entry()]))
    render(<AuditLogPage />)
    await screen.findByText('Carlos Ramos')

    await userEvent.selectOptions(screen.getByLabelText('Acción'), 'MOMENT_LAUNCHED')
    await userEvent.type(screen.getByLabelText('Desde'), '2026-09-01')
    await userEvent.type(screen.getByLabelText('Hasta'), '2026-09-30')
    await userEvent.type(screen.getByLabelText('Buscar'), ' Ana ')
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))

    expect(list).toHaveBeenLastCalledWith({
      page: 0,
      size: 50,
      action: 'MOMENT_LAUNCHED',
      from: '2026-09-01',
      to: '2026-09-30',
      q: 'Ana',
    })
  })

  it('does not search with an inverted date range', async () => {
    list.mockResolvedValue(page([entry()]))
    render(<AuditLogPage />)
    await screen.findByText('Carlos Ramos')
    const calls = list.mock.calls.length

    await userEvent.type(screen.getByLabelText('Desde'), '2026-09-10')
    await userEvent.type(screen.getByLabelText('Hasta'), '2026-09-01')

    expect(screen.getByText('La fecha final no puede ser anterior a la inicial')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Buscar' })).toBeDisabled()
    expect(list.mock.calls.length).toBe(calls)
  })

  it('moves between pages', async () => {
    list.mockResolvedValueOnce(page([entry()], { total: 120 }))
    list.mockResolvedValueOnce(page([entry({ id: 51, summary: 'Segunda página.' })], { total: 120, page: 1 }))
    render(<AuditLogPage />)

    expect(await screen.findByText('Página 1 de 3 · 120 registros')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()
    await userEvent.click(screen.getByRole('button', { name: 'Siguiente' }))

    expect(await screen.findByText('Segunda página.')).toBeInTheDocument()
    expect(list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1 }))
    expect(screen.getByText('Página 2 de 3 · 120 registros')).toBeInTheDocument()
  })

  it('says so when there are no entries', async () => {
    list.mockResolvedValue(page([]))
    render(<AuditLogPage />)

    expect(await screen.findByText('No hay registros con esos filtros.')).toBeInTheDocument()
  })

  it('offers to retry after an error', async () => {
    list.mockRejectedValueOnce(new Error('boom'))
    list.mockResolvedValueOnce(page([entry()]))
    render(<AuditLogPage />)

    await userEvent.click(await screen.findByRole('button', { name: 'Reintentar' }))

    expect(await screen.findByText('Carlos Ramos')).toBeInTheDocument()
  })
})
