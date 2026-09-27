/**
 * Reglas de contraseña del backend (`PasswordRules`): al menos 8 caracteres, con al
 * menos una letra y un número. Se validan también aquí para avisar antes de enviar.
 */
const PASSWORD_PATTERN = /^(?=.*[A-Za-zÁÉÍÓÚáéíóúÑñ])(?=.*\d).{8,}$/

export const PASSWORD_RULES_MESSAGE =
  'La contraseña debe tener al menos 8 caracteres, con al menos una letra y un número.'

/**
 * Valida la contraseña nueva y su confirmación. Devuelve los errores por campo
 * (`newPassword`, `confirmPassword`); vacío si todo está bien.
 */
export function validateNewPassword(newPassword: string, confirmPassword: string): Record<string, string> {
  const errors: Record<string, string> = {}
  if (!PASSWORD_PATTERN.test(newPassword)) {
    errors.newPassword = PASSWORD_RULES_MESSAGE
  }
  if (newPassword !== confirmPassword) {
    errors.confirmPassword = 'Las contraseñas no coinciden.'
  }
  return errors
}
