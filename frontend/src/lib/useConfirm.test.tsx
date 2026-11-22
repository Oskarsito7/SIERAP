import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useState } from 'react'
import { describe, expect, it } from 'vitest'

import { answerConfirm } from '@/test/confirm'

import { useConfirm } from './useConfirm'

/** Botón que pregunta antes de "borrar" y muestra qué se respondió. */
function Example() {
  const [confirm, confirmDialog] = useConfirm()
  const [answer, setAnswer] = useState('sin respuesta')
  const ask = async () => {
    const confirmed = await confirm({
      title: '¿Eliminar el periodo 2026-2?',
      description: 'Esta acción no se puede deshacer.',
      confirmLabel: 'Eliminar',
    })
    setAnswer(confirmed ? 'confirmó' : 'canceló')
  }
  return (
    <>
      {confirmDialog}
      <button type="button" onClick={ask}>
        Eliminar 2026-2
      </button>
      <p>{answer}</p>
    </>
  )
}

describe('useConfirm', () => {
  it('asks with the text of the action and resolves true when confirmed', async () => {
    render(<Example />)
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar 2026-2' }))

    const dialog = screen.getByRole('alertdialog', { name: '¿Eliminar el periodo 2026-2?' })
    expect(dialog).toHaveTextContent('Esta acción no se puede deshacer.')
    await answerConfirm('Eliminar')

    expect(await screen.findByText('confirmó')).toBeInTheDocument()
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
  })

  it('resolves false when cancelled with the button or with Escape', async () => {
    render(<Example />)
    await userEvent.click(screen.getByRole('button', { name: 'Eliminar 2026-2' }))
    await answerConfirm('Cancelar')
    expect(await screen.findByText('canceló')).toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: 'Eliminar 2026-2' }))
    await screen.findByRole('alertdialog')
    await userEvent.keyboard('{Escape}')
    expect(screen.queryByRole('alertdialog')).not.toBeInTheDocument()
    expect(screen.getByText('canceló')).toBeInTheDocument()
  })
})
