import { useId, type ComponentProps } from 'react'

import { Label } from '@/components/ui/label'

interface SelectFieldProps extends ComponentProps<'select'> {
  label: string
  /** Mensaje de validación del campo (del cliente o de `fieldErrors` de la API). */
  error?: string
}

/** Lista desplegable con su etiqueta y su mensaje de error, accesibles (par de `FormField`). */
export function SelectField({ label, error, children, ...selectProps }: SelectFieldProps) {
  const id = useId()
  const errorId = `${id}-error`
  return (
    <div className="flex flex-col gap-2">
      <Label htmlFor={id}>{label}</Label>
      <select
        id={id}
        className="border-input bg-background h-9 rounded-md border px-3 text-sm shadow-xs"
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...selectProps}
      >
        {children}
      </select>
      {error && (
        <p id={errorId} className="text-destructive text-sm">
          {error}
        </p>
      )}
    </div>
  )
}
