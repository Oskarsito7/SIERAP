import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { MemoryRouter } from 'react-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { accountService } from '@/services/api/accountService'
import type { ApiError } from '@/types/api'
import type { UserSummary } from '@/types/auth'

import { ExternalEvaluatorsPage } from './ExternalEvaluatorsPage'

vi.mock('@/services/api/accountService', () => ({
  accountService: {
    listExternalEvaluators: vi.fn(),
    createExternalEvaluator: vi.fn(),
    resendPasswordLink: vi.fn(),
    listEvaluatorTypes: vi.fn(),
  },
}))

const service = vi.mocked(accountService)

function evaluator(id: number, firstNames: string, lastNames: string, overrides: Partial<UserSummary> = {}): UserSummary {
  return {
    id,
    firstNames,
    lastNames,
    fullName: `${firstNames} ${lastNames}`,
    email: `${firstNames.toLowerCase()}@correo.com`,
    role: 'EXTERNAL_EVALUATOR',
    active: true,
    mustChangePassword: false,
    ...overrides,
  }
}

function apiError(status: number, message: string, fieldErrors: Record<string, string> = {}): AxiosError {
  const data: ApiError = { timestamp: '', status, error: '', message, path: '', fieldErrors }
  return new AxiosError(message, 'ERR_BAD_REQUEST', undefined, null, { status, data } as AxiosResponse)
}

function renderPage() {
  render(
    <MemoryRouter>
      <ExternalEvaluatorsPage />
    </MemoryRouter>,
  )
}

async function fillForm() {
  await userEvent.click(screen.getByRole('button', { name: 'Nuevo evaluador' }))
  await userEvent.type(screen.getByLabelText('Nombres'), 'Carlos')
  await userEvent.type(screen.getByLabelText('Apellidos'), 'Pérez')
  await userEvent.type(screen.getByLabelText('Número de documento'), '1065800221')
  await userEvent.type(screen.getByLabelText('Correo'), 'carlos@gmail.com')
  await userEvent.selectOptions(await screen.findByLabelText('Tipo de evaluador'), 'Egresado')
}

describe('ExternalEvaluatorsPage', () => {
  beforeEach(() => {
    Object.values(service).forEach((mock) => mock.mockReset())
    service.listEvaluatorTypes.mockResolvedValue([
      { id: 1, name: 'Empresario', description: null, active: true },
      { id: 2, name: 'Egresado', description: null, active: true },
      { id: 3, name: 'Antiguo', description: null, active: false },
    ])
  })

  it('lists the evaluators and filters them by name or email', async () => {
    service.listExternalEvaluators.mockResolvedValue([
      evaluator(1, 'Sofía', 'Vega'),
      evaluator(2, 'Carlos', 'Pérez', { active: false }),
    ])
    renderPage()

    expect(await screen.findByText('Sofía Vega')).toBeInTheDocument()
    expect(screen.getByText('Desactivado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Reenviar enlace a Carlos Pérez' })).toBeDisabled()

    await userEvent.type(screen.getByLabelText('Buscar por nombre o correo'), 'sofía@')

    expect(screen.getByText('Sofía Vega')).toBeInTheDocument()
    expect(screen.queryByText('Carlos Pérez')).not.toBeInTheDocument()
  })

  it('resends the password link', async () => {
    service.listExternalEvaluators.mockResolvedValue([evaluator(1, 'Sofía', 'Vega')])
    service.resendPasswordLink.mockResolvedValue()
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Reenviar enlace a Sofía Vega' }))

    expect(service.resendPasswordLink).toHaveBeenCalledWith(1)
    expect(await screen.findByRole('status')).toHaveTextContent('Se envió de nuevo el enlace a sofía@correo.com.')
  })

  it('only offers the active evaluator types', async () => {
    service.listExternalEvaluators.mockResolvedValue([])
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo evaluador' }))
    const types = await screen.findByLabelText('Tipo de evaluador')

    expect(await within(types).findByRole('option', { name: 'Egresado' })).toBeInTheDocument()
    expect(within(types).queryByRole('option', { name: 'Antiguo' })).not.toBeInTheDocument()
  })

  it('checks the required fields before sending', async () => {
    service.listExternalEvaluators.mockResolvedValue([])
    renderPage()

    await userEvent.click(await screen.findByRole('button', { name: 'Nuevo evaluador' }))
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))

    expect(screen.getByText('Los nombres son obligatorios')).toBeInTheDocument()
    expect(screen.getByText('El tipo de evaluador es obligatorio')).toBeInTheDocument()
    expect(service.createExternalEvaluator).not.toHaveBeenCalled()
  })

  it('creates an evaluator, adds it to the list and says an email was sent', async () => {
    service.listExternalEvaluators.mockResolvedValue([evaluator(1, 'Sofía', 'Vega')])
    service.createExternalEvaluator.mockResolvedValue(evaluator(9, 'Carlos', 'Pérez'))
    renderPage()
    await screen.findByText('Sofía Vega')

    await fillForm()
    await userEvent.type(screen.getByLabelText('Año de grado (opcional)'), '2019')
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))

    expect(service.createExternalEvaluator).toHaveBeenCalledWith({
      firstNames: 'Carlos',
      lastNames: 'Pérez',
      documentType: 'CC',
      documentNumber: '1065800221',
      email: 'carlos@gmail.com',
      evaluatorTypeId: 2,
      organization: undefined,
      position: undefined,
      graduationYear: 2019,
    })
    expect(await screen.findByText('Carlos Pérez')).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Recibirá un correo para crear su contraseña.')
    expect(screen.queryByRole('button', { name: 'Crear y enviar enlace' })).not.toBeInTheDocument()
  })

  it('shows the errors the API returns for the form', async () => {
    service.listExternalEvaluators.mockResolvedValue([])
    service.createExternalEvaluator.mockRejectedValue(
      apiError(409, 'Ya existe una cuenta con ese correo o documento.'),
    )
    renderPage()
    await screen.findByText('Todavía no has creado evaluadores externos.')

    await fillForm()
    await userEvent.click(screen.getByRole('button', { name: 'Crear y enviar enlace' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Ya existe una cuenta con ese correo o documento.')
    expect(screen.getByRole('button', { name: 'Crear y enviar enlace' })).toBeEnabled()
  })
})
