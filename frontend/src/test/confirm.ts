import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

/**
 * Responde el `ConfirmDialog` abierto (lo que antes se simulaba con
 * `vi.spyOn(window, 'confirm')`): pulsa el botón con ese nombre, p. ej. "Eliminar" o
 * "Cancelar".
 */
export async function answerConfirm(button: string): Promise<void> {
  const dialog = await screen.findByRole('alertdialog')
  await userEvent.click(within(dialog).getByRole('button', { name: button }))
}
