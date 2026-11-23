import { useEffect, useId, useState } from 'react'

import { authService } from '@/services/api/authService'

interface PrivacyConsentProps {
  checked: boolean
  onChange: (checked: boolean) => void
  /** Mensaje de validación (del cliente o de `fieldErrors`). */
  error?: string
}

/**
 * Casilla obligatoria de autorización del tratamiento de datos personales (Ley 1581 de
 * 2012). Enlaza la política de tratamiento de datos si el super_admin configuró su URL
 * (`GET /api/auth/privacy-policy`); si no hay URL o la consulta falla, el texto va sin enlace.
 */
export function PrivacyConsent({ checked, onChange, error }: PrivacyConsentProps) {
  const id = useId()
  const errorId = `${id}-error`
  const [url, setUrl] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    authService
      .getPrivacyPolicyUrl()
      .then((policyUrl) => {
        if (!cancelled) setUrl(policyUrl)
      })
      .catch(() => {
        // Sin la URL la casilla sigue funcionando, solo que sin enlace.
      })
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <div className="flex flex-col gap-1">
      <div className="flex items-start gap-2">
        <input
          id={id}
          type="checkbox"
          className="mt-1"
          checked={checked}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          onChange={(event) => onChange(event.target.checked)}
        />
        <label htmlFor={id} className="text-sm">
          Autorizo a la Universidad Popular del Cesar el tratamiento de mis datos personales según la{' '}
          {url ? (
            <a
              href={url}
              target="_blank"
              rel="noopener noreferrer"
              className="text-link hover:text-link-hover underline underline-offset-4"
            >
              Política de tratamiento de datos
            </a>
          ) : (
            'Política de tratamiento de datos'
          )}{' '}
          (Ley 1581 de 2012).
        </label>
      </div>
      {error && (
        <p id={errorId} className="text-destructive text-sm">
          {error}
        </p>
      )}
    </div>
  )
}
