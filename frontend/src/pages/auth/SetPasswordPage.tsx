import { useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router'

import { FormError, FormField } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { PASSWORD_RULES_MESSAGE, validateNewPassword } from '@/lib/passwordRules'
import { authService } from '@/services/api/authService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'

/**
 * Creación o recuperación de la contraseña con el enlace recibido por correo
 * (`/set-password?token=...`), tanto para cuentas nuevas como para "olvidé mi contraseña".
 */
export function SetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [done, setDone] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors = validateNewPassword(newPassword, confirmPassword)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSubmitting(true)
    try {
      await authService.setPassword({ token, newPassword })
      setDone(true)
    } catch (saveError) {
      setFieldErrors(getFieldErrors(saveError))
      setError(getErrorMessage(saveError))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card className="mx-auto w-full max-w-sm">
      <CardHeader>
        <CardTitle>
          <h1>Crear contraseña</h1>
        </CardTitle>
        <CardDescription>{PASSWORD_RULES_MESSAGE}</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {!token && (
          <FormError message="El enlace no es válido. Revisa que lo hayas copiado completo o pide uno nuevo." />
        )}
        {token && done && (
          <p role="status" className="text-sm">
            Tu contraseña quedó guardada. Ya puedes iniciar sesión.
          </p>
        )}
        {token && !done && (
          <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
            <FormError message={error} />
            <FormField
              label="Contraseña nueva"
              type="password"
              autoComplete="new-password"
              required
              value={newPassword}
              error={fieldErrors.newPassword}
              onChange={(event) => setNewPassword(event.target.value)}
            />
            <FormField
              label="Confirmar contraseña nueva"
              type="password"
              autoComplete="new-password"
              required
              value={confirmPassword}
              error={fieldErrors.confirmPassword}
              onChange={(event) => setConfirmPassword(event.target.value)}
            />
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Guardando…' : 'Guardar contraseña'}
            </Button>
          </form>
        )}
        <div className="flex flex-col gap-1 text-sm">
          <Link to="/login" className="text-primary underline-offset-4 hover:underline">
            Ir a iniciar sesión
          </Link>
          {!done && (
            <Link to="/forgot-password" className="text-primary underline-offset-4 hover:underline">
              Pedir un enlace nuevo
            </Link>
          )}
        </div>
      </CardContent>
    </Card>
  )
}
