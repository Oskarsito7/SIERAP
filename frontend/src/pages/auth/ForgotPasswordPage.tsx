import { useState, type FormEvent } from 'react'
import { Link } from 'react-router'

import { FormError, FormField } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { authService } from '@/services/api/authService'
import { getErrorMessage, getFieldErrors } from '@/services/api/httpClient'

/** Solicitud del enlace para recuperar la contraseña. */
export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [sent, setSent] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await authService.forgotPassword({ email: email.trim() })
      setSent(true)
    } catch (forgotError) {
      setFieldErrors(getFieldErrors(forgotError))
      setError(getErrorMessage(forgotError))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <Card className="mx-auto w-full max-w-sm">
      <CardHeader>
        <CardTitle>
          <h1>Recuperar contraseña</h1>
        </CardTitle>
        <CardDescription>Te enviaremos un enlace para crear una contraseña nueva.</CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        {sent ? (
          // El backend responde igual exista o no la cuenta; el mensaje tampoco lo revela.
          <p role="status" className="text-sm">
            Si el correo está registrado, recibirás un enlace para crear tu contraseña nueva. El
            enlace vence en una hora.
          </p>
        ) : (
          <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
            <FormError message={error} />
            <FormField
              label="Correo"
              type="email"
              autoComplete="email"
              required
              value={email}
              error={fieldErrors.email}
              onChange={(event) => setEmail(event.target.value)}
            />
            <Button type="submit" disabled={submitting}>
              {submitting ? 'Enviando…' : 'Enviar enlace'}
            </Button>
          </form>
        )}
        <Link to="/login" className="text-primary text-sm underline-offset-4 hover:underline">
          Volver a iniciar sesión
        </Link>
      </CardContent>
    </Card>
  )
}
