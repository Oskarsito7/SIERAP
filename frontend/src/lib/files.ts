/**
 * Reglas de los archivos de una entrega (ADR-015), revisadas en el navegador antes de
 * subir. El backend las vuelve a validar; revisarlas aquí evita que un archivo muy
 * grande se envíe para nada: si supera por mucho el límite, el servidor corta la
 * conexión y el navegador solo vería un error de red en vez del mensaje de la API.
 */

const MB = 1024 * 1024

/** Tamaño máximo del `.zip` del proyecto: 100 MB. */
export const MAX_ZIP_SIZE = 100 * MB

/** Tamaño máximo de la presentación: 30 MB. */
export const MAX_PRESENTATION_SIZE = 30 * MB

/** Extensiones aceptadas para la presentación. */
export const PRESENTATION_EXTENSIONS = ['.pdf', '.docx', '.pptx']

function extensionOf(name: string): string {
  const dot = name.lastIndexOf('.')
  return dot < 0 ? '' : name.slice(dot).toLowerCase()
}

/** Error del `.zip` del proyecto, o `null` si se puede subir. */
export function zipError(file: File | null): string | null {
  if (!file) return 'Adjunta el archivo .zip con el proyecto.'
  if (extensionOf(file.name) !== '.zip') return 'El proyecto debe ser un archivo .zip.'
  if (file.size === 0) return 'El archivo .zip está vacío.'
  if (file.size > MAX_ZIP_SIZE) return `El .zip pesa ${formatSize(file.size)}: el máximo es 100 MB.`
  return null
}

/** Error de la presentación (opcional), o `null` si se puede subir. */
export function presentationError(file: File | null): string | null {
  if (!file) return null
  if (!PRESENTATION_EXTENSIONS.includes(extensionOf(file.name))) {
    return 'La presentación debe ser un archivo .pdf, .docx o .pptx.'
  }
  if (file.size === 0) return 'La presentación está vacía.'
  if (file.size > MAX_PRESENTATION_SIZE) {
    return `La presentación pesa ${formatSize(file.size)}: el máximo es 30 MB.`
  }
  return null
}

/** Tamaño legible, p. ej. "41 MB" o "820 KB". */
export function formatSize(bytes: number): string {
  if (bytes >= MB) return `${(bytes / MB).toFixed(bytes >= 10 * MB ? 0 : 1).replace('.', ',')} MB`
  return `${Math.max(1, Math.round(bytes / 1024))} KB`
}

/** Guarda en el equipo un archivo descargado de la API con el nombre indicado. */
export function saveFile(blob: Blob, fileName: string): void {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.append(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
