import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { momentService } from '@/services/api/momentService'
import { answerConfirm } from '@/test/confirm'

import { ExtensionSection } from './ExtensionSection'
import { momentDetailFixture, periodFixture } from './testData'

vi.mock('@/services/api/momentService', () => ({ momentService: { extend: vi.fn() } }))

const extend = vi.mocked(momentService.extend)
const onExtended = vi.fn()
const REASON = 'Paro estudiantil de dos semanas'

function renderSection() {
  render(
    <ExtensionSection
      moment={momentDetailFixture({ status: 'LAUNCHED' })}
      periods={[periodFixture()]}
      onExtended={onExtended}
    />,
  )
}

describe('ExtensionSection', () => {
  beforeEach(() => {
    extend.mockReset()
    onExtended.mockReset()
  })

  it('shows the current dates and the UPC deadline', () => {
    renderSection()

    expect(screen.getByRole('region', { name: 'Prórroga excepcional' })).toBeInTheDocument()
    expect(screen.getByText('31/10/2026')).toBeInTheDocument()
    expect(screen.getByText('20/11/2026')).toBeInTheDocument()
    expect(screen.getByText(/límite de la UPC \(28\/11\/2026\)/)).toBeInTheDocument()
  })

  it('extends after the confirmation and reports the updated moment', async () => {
    const updated = momentDetailFixture({ status: 'LAUNCHED', gradingEnd: '2026-11-25' })
    extend.mockResolvedValue(updated)
    renderSection()

    await userEvent.type(screen.getByLabelText('Nuevo fin de calificación'), '2026-11-25')
    await userEvent.type(screen.getByLabelText('Motivo'), REASON)
    await userEvent.click(screen.getByRole('button', { name: 'Prorrogar fechas' }))

    expect(await screen.findByText('Las fechas solo se pueden ampliar y quedará registrado en la bitácora.')).toBeInTheDocument()
    expect(extend).not.toHaveBeenCalled()
    await answerConfirm('Prorrogar')

    expect(extend).toHaveBeenCalledWith(1, { submissionEnd: undefined, gradingEnd: '2026-11-25', reason: REASON })
    expect(onExtended).toHaveBeenCalledWith(updated)
    expect(await screen.findByRole('status')).toHaveTextContent('Se prorrogaron las fechas del momento')
  })

  it('does nothing when the confirmation is cancelled', async () => {
    renderSection()

    await userEvent.type(screen.getByLabelText('Nuevo fin de entregas'), '2026-11-05')
    await userEvent.type(screen.getByLabelText('Motivo'), REASON)
    await userEvent.click(screen.getByRole('button', { name: 'Prorrogar fechas' }))
    await answerConfirm('Cancelar')

    expect(extend).not.toHaveBeenCalled()
  })

  it('validates the dates and the reason before asking for confirmation', async () => {
    renderSection()

    await userEvent.click(screen.getByRole('button', { name: 'Prorrogar fechas' }))
    expect(screen.getByText('Indica al menos una fecha nueva')).toBeInTheDocument()
    expect(screen.getByText('El motivo debe tener entre 10 y 500 caracteres')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('Nuevo fin de calificación'), '2026-11-10')
    await userEvent.click(screen.getByRole('button', { name: 'Prorrogar fechas' }))
    expect(screen.getByText('Las fechas solo se pueden ampliar: deben ser posteriores a las actuales')).toBeInTheDocument()

    await userEvent.clear(screen.getByLabelText('Nuevo fin de calificación'))
    await userEvent.type(screen.getByLabelText('Nuevo fin de calificación'), '2026-11-29')
    await userEvent.click(screen.getByRole('button', { name: 'Prorrogar fechas' }))
    expect(screen.getByText(/después del límite de la UPC \(28\/11\/2026\)/)).toBeInTheDocument()

    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(extend).not.toHaveBeenCalled()
  })

  it('shows the message of the API when the extension is rejected', async () => {
    extend.mockRejectedValue({ isAxiosError: true, response: { data: { message: 'Con el nuevo fin de entregas la calificación empezaría el 2026-11-21.' } } })
    renderSection()

    await userEvent.type(screen.getByLabelText('Nuevo fin de entregas'), '2026-11-20')
    await userEvent.type(screen.getByLabelText('Motivo'), REASON)
    await userEvent.click(screen.getByRole('button', { name: 'Prorrogar fechas' }))
    await answerConfirm('Prorrogar')

    expect(await screen.findByRole('alert')).toBeInTheDocument()
    expect(onExtended).not.toHaveBeenCalled()
  })
})
