import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { createMemoryRouter, Link, RouterProvider } from 'react-router'
import { AxiosError, type AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { evaluationService } from '@/services/api/evaluationService'
import { submissionService } from '@/services/api/submissionService'
import type { ApiError } from '@/types/api'
import type { EvaluationForm } from '@/types/evaluation'

import { EvaluationEditor } from './EvaluationEditor'

vi.mock('@/services/api/evaluationService', () => ({
  evaluationService: { form: vi.fn(), save: vi.fn(), submit: vi.fn(), list: vi.fn() },
}))
vi.mock('@/services/api/submissionService', () => ({
  submissionService: { history: vi.fn(), downloadZip: vi.fn(), downloadPresentation: vi.fn() },
}))

const loadForm = vi.mocked(evaluationService.form)
const save = vi.mocked(evaluationService.save)
const submit = vi.mocked(evaluationService.submit)
const history = vi.mocked(submissionService.history)

/** Dos RAP de un criterio cada uno: "Problema" (máx. 1) y "Sustentación" (máx. 2). */
function formFixture(overrides: Partial<EvaluationForm> = {}): EvaluationForm {
  return {
    id: null,
    groupId: 7,
    status: null,
    evaluatedAt: null,
    comment: null,
    canEdit: true,
    submissionToGrade: { id: 90, versionNumber: 3, late: false },
    learningOutcomes: [
      {
        id: 10, code: 'RAP1', description: 'Integra las ciencias básicas', maxScore: 1,
        criteria: [{ id: 20, description: 'Problema', levelId: null, levels: [
          { id: 30, name: 'Bajo', description: null, score: 0 },
          { id: 31, name: 'Alto', description: 'Causas y consecuencias', score: 1 },
        ] }],
      },
      {
        id: 11, code: 'RAP2', description: 'Sustenta la solución', maxScore: 2,
        criteria: [{ id: 21, description: 'Sustentación', levelId: null, levels: [
          { id: 40, name: 'Bajo', description: null, score: 0 },
          { id: 41, name: 'Alto', description: null, score: 2 },
        ] }],
      },
    ],
    members: [
      { id: 50, fullName: 'Ana Gómez', adjustments: [], totalScore: 0 },
      { id: 51, fullName: 'Luis Martínez', adjustments: [], totalScore: 0 },
    ],
    totalScore: 0,
    maxScore: 3,
    complete: false,
    ...overrides,
  }
}

/** La misma evaluación con los dos criterios en "Alto". */
function gradedForm(overrides: Partial<EvaluationForm> = {}): EvaluationForm {
  const form = formFixture()
  return {
    ...form,
    id: 15,
    status: 'DRAFT',
    evaluatedAt: '2026-11-24T10:15:00',
    learningOutcomes: form.learningOutcomes.map((outcome) => ({
      ...outcome,
      criteria: outcome.criteria.map((criterion) => ({ ...criterion, levelId: criterion.levels[1].id })),
    })),
    totalScore: 3,
    complete: true,
    ...overrides,
  }
}

/**
 * Muestra el editor dentro de un router de datos (lo exige el aviso de cambios sin
 * guardar), con un enlace para intentar salir a otra página.
 */
function renderEditor() {
  const router = createMemoryRouter(
    [
      {
        path: '/groups/7/evaluation',
        element: (
          <>
            <Link to="/groups">Volver a mis grupos</Link>
            <EvaluationEditor groupId={7} />
          </>
        ),
      },
      { path: '/groups', element: <p>Lista de grupos</p> },
    ],
    { initialEntries: ['/groups/7/evaluation'] },
  )
  render(<RouterProvider router={router} />)
}

function criterion(name: string): HTMLElement {
  return screen.getByRole('radiogroup', { name })
}

describe('EvaluationEditor', () => {
  beforeEach(() => {
    loadForm.mockReset()
    save.mockReset()
    submit.mockReset()
    history.mockReset()
    history.mockResolvedValue({
      versions: [{ id: 90, groupId: 7, versionNumber: 3, submittedAt: '2026-11-14T22:41:00', late: false,
        zipFileName: 'sierap-v3.zip', zipSize: 1, presentationFileName: null, presentationSize: null,
        toGrade: true }],
      currentPresentation: null,
      maxVersions: 20,
    })
  })

  it('grades the group criterion by criterion and saves a partial draft', async () => {
    loadForm.mockResolvedValue(formFixture())
    save.mockResolvedValue(formFixture({ id: 15, status: 'DRAFT', evaluatedAt: '2026-11-24T10:15:00' }))
    renderEditor()

    expect(await screen.findByText(/Se califica la v3/)).toHaveTextContent('Se califica la v3 (14/11/2026 22:41, a tiempo)')
    expect(screen.getByText('Sin empezar')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Enviar evaluación' })).toBeDisabled()

    await userEvent.click(within(criterion('Problema')).getByRole('radio', { name: 'Alto 1,00' }))
    expect(within(criterion('Problema')).getByRole('radio', { name: 'Alto 1,00' })).toHaveAttribute('aria-checked', 'true')
    expect(screen.getByLabelText('Nota del grupo')).toHaveTextContent('1,00')
    expect(screen.getByText(/1 de 2 criterios calificados/)).toBeInTheDocument()
    expect(screen.getByText('Para enviar, califica los 2 criterios.')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Comentario para el grupo'), 'Buen trabajo')
    await userEvent.click(screen.getByRole('button', { name: 'Guardar borrador' }))

    expect(save).toHaveBeenCalledWith(7, {
      levels: [{ criterionId: 20, levelId: 31 }],
      comment: 'Buen trabajo',
      adjustments: [],
    })
    expect(await screen.findByText('Borrador')).toBeInTheDocument()
  })

  it('submits once every criterion is graded, saving the pending changes first', async () => {
    loadForm.mockResolvedValue(gradedForm({ learningOutcomes: gradedForm().learningOutcomes.map((outcome, index) =>
      index === 1 ? { ...outcome, criteria: [{ ...outcome.criteria[0], levelId: null }] } : outcome) }))
    save.mockResolvedValue(gradedForm())
    submit.mockResolvedValue(gradedForm({ status: 'SUBMITTED' }))
    renderEditor()

    await userEvent.click(await within(await screen.findByRole('radiogroup', { name: 'Sustentación' }))
      .findByRole('radio', { name: 'Alto 2,00' }))
    await userEvent.click(screen.getByRole('button', { name: 'Enviar evaluación' }))

    expect(save).toHaveBeenCalledTimes(1)
    expect(submit).toHaveBeenCalledWith(7)
    expect(await screen.findByText('Enviada')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Enviar evaluación' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Guardar corrección' })).toBeDisabled()
  })

  it('adjusts one student and goes back to the group level by picking it again', async () => {
    loadForm.mockResolvedValue(gradedForm())
    save.mockResolvedValue(gradedForm())
    renderEditor()

    await userEvent.click(await screen.findByRole('button', { name: 'Ajustar a Ana Gómez' }))
    expect(screen.getByText('Ajuste individual · Ana Gómez')).toBeInTheDocument()
    const defense = screen.getByRole('radiogroup', { name: 'RAP2 · Sustentación' })
    expect(within(defense).getByRole('radio', { name: 'Alto 2,00 (nivel del grupo)' }))
      .toHaveAttribute('aria-checked', 'true')

    await userEvent.click(within(defense).getByRole('radio', { name: 'Bajo 0,00' }))
    expect(screen.getByText('Nota de Ana').nextSibling).toHaveTextContent('1,00')
    expect(screen.getByText('Diferencia').nextSibling).toHaveTextContent('-2,00')
    expect(screen.getByText(/Ajustado en 1 criterio/)).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Guardar borrador' }))
    expect(save).toHaveBeenCalledWith(7, expect.objectContaining({
      adjustments: [{ studentId: 50, criterionId: 21, levelId: 40 }],
    }))

    await userEvent.click(within(screen.getByRole('radiogroup', { name: 'RAP2 · Sustentación' }))
      .getByRole('radio', { name: 'Alto 2,00 (nivel del grupo)' }))
    expect(screen.queryByText(/Ajustado en/)).not.toBeInTheDocument()
  })

  it('locks the form outside the grading period', async () => {
    loadForm.mockResolvedValue(gradedForm({ canEdit: false, status: 'SUBMITTED' }))
    renderEditor()

    expect(await screen.findByText(/Hoy no puedes modificar esta evaluación/)).toBeInTheDocument()
    expect(within(criterion('Problema')).getByRole('radio', { name: 'Bajo 0,00' })).toBeDisabled()
    expect(screen.getByLabelText('Comentario para el grupo')).toBeDisabled()
  })

  it('shows the message of the API when submitting fails', async () => {
    loadForm.mockResolvedValue(gradedForm())
    submit.mockRejectedValue(apiError(422, 'La ventana de calificación cerró el 2026-11-28.'))
    renderEditor()

    await userEvent.click(await screen.findByRole('button', { name: 'Enviar evaluación' }))

    expect(save).not.toHaveBeenCalled()
    expect(await screen.findByRole('alert')).toHaveTextContent('La ventana de calificación cerró el 2026-11-28.')
  })

  it('says when the group has not submitted anything', async () => {
    loadForm.mockResolvedValue(formFixture({ submissionToGrade: null }))
    renderEditor()

    expect(await screen.findByText('El grupo todavía no ha subido entregas.')).toBeInTheDocument()
  })

  it('asks before leaving the page with unsaved changes', async () => {
    loadForm.mockResolvedValue(formFixture())
    renderEditor()

    await userEvent.click(within(await screen.findByRole('radiogroup', { name: 'Problema' }))
      .getByRole('radio', { name: 'Alto 1,00' }))
    await userEvent.click(screen.getByRole('link', { name: 'Volver a mis grupos' }))

    const dialog = await screen.findByRole('alertdialog', { name: '¿Salir sin guardar?' })
    await userEvent.click(within(dialog).getByRole('button', { name: 'Seguir calificando' }))
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(criterion('Problema')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('link', { name: 'Volver a mis grupos' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Salir sin guardar' }))
    expect(await screen.findByText('Lista de grupos')).toBeInTheDocument()
  })

  it('leaves without asking when there is nothing to save', async () => {
    loadForm.mockResolvedValue(formFixture())
    renderEditor()

    await screen.findByRole('radiogroup', { name: 'Problema' })
    await userEvent.click(screen.getByRole('link', { name: 'Volver a mis grupos' }))

    expect(await screen.findByText('Lista de grupos')).toBeInTheDocument()
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })
})

function apiError(status: number, message: string): AxiosError<ApiError> {
  const data: ApiError = { timestamp: '', status, error: '', message, path: '', fieldErrors: {} }
  return new AxiosError('error', String(status), undefined, undefined, { status, data } as AxiosResponse<ApiError>)
}
