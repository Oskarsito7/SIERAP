import { useId, useRef, useState, type DragEvent } from 'react'
import { FileUp, X } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { formatSize } from '@/lib/files'
import { cn } from '@/lib/utils'

/**
 * Campo de archivo que también recibe el archivo arrastrado y soltado encima.
 *
 * El `<input type="file">` nativo sigue existiendo (oculto, porque su botón sale en el
 * idioma del navegador) y se abre con el botón "Elegir archivo", así que el campo se
 * usa igual con teclado o lector de pantalla. Arrastrar es solo un atajo para el ratón.
 */
export function FileDropzone({
  label,
  hint,
  accept,
  file,
  error,
  disabled = false,
  onChange,
}: {
  label: string
  hint: string
  /** Extensiones aceptadas, p. ej. `.zip` o `.pdf,.docx`; filtra el diálogo del sistema. */
  accept: string
  file: File | null
  error?: string
  disabled?: boolean
  /** Recibe el archivo elegido o soltado, o `null` al quitarlo. */
  onChange: (file: File | null) => void
}) {
  const id = useId()
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)

  const onDragOver = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    if (disabled) {
      event.dataTransfer.dropEffect = 'none'
      return
    }
    event.dataTransfer.dropEffect = 'copy'
    setDragging(true)
  }

  const onDragLeave = (event: DragEvent<HTMLDivElement>) => {
    // Pasar sobre un elemento hijo también dispara `dragleave`: solo cuenta salir del recuadro.
    if (!event.currentTarget.contains(event.relatedTarget as Node | null)) setDragging(false)
  }

  const onDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault()
    event.stopPropagation()
    setDragging(false)
    const dropped = event.dataTransfer.files[0]
    if (!disabled && dropped) onChange(dropped)
  }

  const clear = () => {
    // Vacía el campo nativo para que elegir otra vez el mismo archivo dispare `change`.
    if (inputRef.current) inputRef.current.value = ''
    onChange(null)
  }

  return (
    <div
      data-testid={`dropzone-${label}`}
      className={cn(
        'flex flex-col gap-2 rounded-md border-2 border-dashed p-4 transition-colors',
        dragging && 'border-primary bg-upc-green-50',
        error && !dragging && 'border-destructive/60',
      )}
      onDragOver={onDragOver}
      onDragEnter={onDragOver}
      onDragLeave={onDragLeave}
      onDrop={onDrop}
    >
      <Label htmlFor={id}>{label}</Label>
      <span className="text-muted-foreground text-xs">{hint}</span>
      <div className="flex flex-wrap items-center gap-3">
        <input
          ref={inputRef}
          id={id}
          type="file"
          accept={accept}
          disabled={disabled}
          className="peer sr-only"
          aria-invalid={error ? true : undefined}
          onChange={(event) => onChange(event.target.files?.[0] ?? null)}
        />
        <Button
          variant="outline"
          size="sm"
          className="peer-focus-visible:ring-ring/50 cursor-pointer peer-focus-visible:ring-[3px]"
          asChild
        >
          <label htmlFor={id}>Elegir archivo</label>
        </Button>
        {file ? (
          <span className="flex min-w-0 items-center gap-1 text-sm">
            <span className="break-all">
              {file.name} · {formatSize(file.size)}
            </span>
            {!disabled && (
              <Button
                type="button"
                variant="ghost"
                size="icon"
                className="size-7"
                aria-label={`Quitar ${file.name}`}
                onClick={clear}
              >
                <X aria-hidden />
              </Button>
            )}
          </span>
        ) : (
          <span className="text-muted-foreground flex items-center gap-1 text-sm">
            <FileUp aria-hidden className="size-4" />
            o arrástralo aquí
          </span>
        )}
      </div>
      {error && <p className="text-destructive text-sm">{error}</p>}
    </div>
  )
}
