import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { catalogService } from '@/services/api/catalogService'
import { momentService } from '@/services/api/momentService'

import { NewMomentPage } from './NewMomentPage'
import { momentDetailFixture, periodFixture, programFixture } from './testData'

vi.mock('@/services/api/catalogService', () => ({ catalogService: { periods: vi.fn(), programs: vi.fn() } }))
vi.mock('@/services/api/momentService', () => ({ momentService: { create: vi.fn() } }))

const create = vi.mocked(momentService.create)

describe('NewMomentPage', () => {
  beforeEach(() => {
    vi.mocked(catalogService.periods).mockResolvedValue([periodFixture()])
    vi.mocked(catalogService.programs).mockResolvedValue([programFixture()])
    create.mockReset()
  })

  it('creates the moment in the chosen period and opens it', async () => {
    create.mockResolvedValue(momentDetailFixture({ id: 9 }))
    render(
      <MemoryRouter initialEntries={['/moments/new?periodId=1']}>
        <Routes>
          <Route path="/moments/new" element={<NewMomentPage />} />
          <Route path="/moments/:id" element={<p>Momento creado</p>} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByLabelText('Periodo académico')).toHaveValue('1')
    await userEvent.selectOptions(screen.getByLabelText('Programa'), '1')
    await userEvent.type(screen.getByLabelText('Nombre'), 'Momento 2')
    await userEvent.type(screen.getByLabelText('Número'), '2')
    await userEvent.type(screen.getByLabelText('Asignatura'), 'Estructuras de Datos')
    await userEvent.type(screen.getByLabelText('Desde el semestre'), '4')
    await userEvent.type(screen.getByLabelText('Hasta el semestre'), '6')
    await userEvent.type(screen.getByLabelText('Mínimo de integrantes'), '2')
    await userEvent.type(screen.getByLabelText('Máximo de integrantes'), '4')
    await userEvent.type(screen.getByLabelText('Nivel de exigencia'), 'Medio')
    await userEvent.click(screen.getByRole('button', { name: 'Crear momento' }))

    expect(create).toHaveBeenCalledWith({
      programId: 1, periodId: 1, number: 2, name: 'Momento 2', subject: 'Estructuras de Datos', semesterFrom: 4,
      semesterTo: 6, requirementLevel: 'Medio', minMembers: 2, maxMembers: 4, groupFormationStart: null,
      groupFormationEnd: null, submissionStart: null, submissionEnd: null, gradingStart: null, gradingEnd: null,
    })
    expect(await screen.findByText('Momento creado')).toBeInTheDocument()
  })
})
