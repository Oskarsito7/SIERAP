import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { FormError, FormField } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { PASSWORD_RULES_MESSAGE, validateNewPassword } from '@/lib/passwordRules'
import { authService } from '@/services/api/authService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'

/**
 * Cambio de contraseña del usuario autenticado. Mientras la contraseña sea temporal
 * es la única pantalla disponible (el backend responde 403 a todo lo demás).
 */
export function ChangePasswordPage() {
  const navigate = useNavigate()
  const { user, logout } = useAuth()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const mustChange = user?.mustChangePassword ?? false

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const errors = validateNewPassword(newPassword, confirmPassword)
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) return

    setSubmitting(true)
    try {
      // La respuesta trae una sesión nueva; authService la guarda en lugar de la anterior.
      await authService.changePassword({ currentPassword, newPassword })
      navigate('/', { replace: true })
    } catch (changeError) {
      setFieldErrors(getFieldErrors(changeError))
      setError(getErrorMessage(changeError))
      setSubmitting(false)
    }
  }

  return (
    <Card className="mx-auto w-full max-w-sm">
      <CardHeader>
        <CardTitle>
          <h1>Cambiar contraseña</h1>
        </CardTitle>
        <CardDescription>
          {mustChange
            ? 'Tu contraseña es temporal. Debes cambiarla antes de continuar.'
            : PASSWORD_RULES_MESSAGE}
        </CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
          <FormError message={error} />
          <FormField
            label="Contraseña actual"
            type="password"
            autoComplete="current-password"
            required
            value={currentPassword}
            error={fieldErrors.currentPassword}
            onChange={(event) => setCurrentPassword(event.target.value)}
          />
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
            {submitting ? 'Guardando…' : 'Cambiar contraseña'}
          </Button>
          {mustChange && (
            <Button type="button" variant="ghost" onClick={() => void logout()}>
              Cerrar sesión
            </Button>
          )}
        </form>
      </CardContent>
    </Card>
  )
}
