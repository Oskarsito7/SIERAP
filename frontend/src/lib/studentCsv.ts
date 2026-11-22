/**
 * Plantilla del CSV de la carga masiva de estudiantes (`docs/07-contrato-api.md`).
 * La cabecera es la que exige el backend; el ejemplo solo ilustra el formato.
 */

/** Columnas de la cabecera, en el orden de la plantilla. */
export const STUDENT_CSV_HEADER = [
  'nombres',
  'apellidos',
  'tipo_documento',
  'numero_documento',
  'correo',
  'codigo_estudiantil',
  'semestre',
]

/** Fila de ejemplo de la plantilla. */
const EXAMPLE_ROW = ['Ana María', 'Pérez Ruiz', 'CC', '1065000123', 'ana.perez@unicesar.edu.co', '20261001', '3']

/** Tamaño máximo del archivo: 1 MB (lo valida también el backend). */
export const MAX_STUDENT_CSV_SIZE = 1024 * 1024

/** Contenido de la plantilla: cabecera y una fila de ejemplo, con BOM para que Excel lea las tildes. */
export function studentCsvTemplate(): string {
  const bom = String.fromCharCode(0xfeff)
  return `${bom}${STUDENT_CSV_HEADER.join(',')}\r\n${EXAMPLE_ROW.join(',')}\r\n`
}

/** Error del archivo elegido, o `null` si se puede enviar. */
export function studentCsvError(file: File | null): string | null {
  if (!file) return 'Elige el archivo CSV con los estudiantes.'
  if (!file.name.toLowerCase().endsWith('.csv')) return 'El archivo debe ser un .csv.'
  if (file.size === 0) return 'El archivo está vacío.'
  if (file.size > MAX_STUDENT_CSV_SIZE) return 'El archivo no puede pesar más de 1 MB.'
  return null
}
