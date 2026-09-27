import { useId, type ComponentProps } from 'react'

import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

interface FormFieldProps extends ComponentProps<typeof Input> {
  label: string
  /** Mensaje de validación del campo (del cliente o de `fieldErrors` de la API). */
  error?: string
}

/** Campo de formulario con su etiqueta y su mensaje de error, accesibles. */
export function FormField({ label, error, ...inputProps }: FormFieldProps) {
  const id = useId()
  const errorId = `${id}-error`
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id}>{label}</Label>
      <Input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...inputProps}
      />
      {error && (
        <p id={errorId} className="text-destructive text-sm">
          {error}
        </p>
      )}
    </div>
  )
}

/** Mensaje de error general de un formulario (el `message` de la API). */
export function FormError({ message }: { message: string | null }) {
  if (!message) return null
  return (
    <p role="alert" className="border-destructive/50 text-destructive rounded-md border px-3 py-2 text-sm">
      {message}
    </p>
  )
}
