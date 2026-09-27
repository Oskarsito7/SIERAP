import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'

import { FormError, FormField } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import type { RedirectState } from '@/routes/guards'
import { authService } from '@/services/api/authService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'

/** Inicio de sesión con el correo y la contraseña. */
export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setFieldErrors({})
    setSubmitting(true)
    try {
      await authService.login({ email: email.trim(), password })
      // Si la contraseña es temporal, la guarda de rutas lleva a "Cambiar contraseña".
      const from = (location.state as RedirectState | null)?.from
      navigate(from ?? '/', { replace: true })
    } catch (loginError) {
      setFieldErrors(getFieldErrors(loginError))
      setError(getErrorMessage(loginError))
      setSubmitting(false)
    }
  }

  return (
    <Card className="mx-auto w-full max-w-sm">
      <CardHeader>
        <CardTitle>
          <h1>Iniciar sesión</h1>
        </CardTitle>
        <CardDescription>Ingresa con tu correo y tu contraseña.</CardDescription>
      </CardHeader>
      <CardContent>
        <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
          <FormError message={error} />
          <FormField
            label="Correo"
            type="email"
            autoComplete="username"
            required
            value={email}
            error={fieldErrors.email}
            onChange={(event) => setEmail(event.target.value)}
          />
          <FormField
            label="Contraseña"
            type="password"
            autoComplete="current-password"
            required
            value={password}
            error={fieldErrors.password}
            onChange={(event) => setPassword(event.target.value)}
          />
          {/* Sin deshabilitar por campos vacíos: el autocompletado del navegador no avisa a
              React hasta que el usuario interactúa, y el botón quedaría bloqueado. */}
          <Button type="submit" disabled={submitting}>
            {submitting ? 'Ingresando…' : 'Ingresar'}
          </Button>
          <div className="flex flex-col gap-1 text-sm">
            <Link to="/forgot-password" className="text-primary underline-offset-4 hover:underline">
              ¿Olvidaste tu contraseña?
            </Link>
            <Link to="/register" className="text-primary underline-offset-4 hover:underline">
              Soy estudiante y tengo un código de registro
            </Link>
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
