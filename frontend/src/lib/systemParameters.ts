/**
 * Parámetros globales que el motor del sistema lee (migraciones V10 y V11). Las reglas
 * de formato son las mismas que aplica el backend al usarlos (`GradeScale.of` y
 * `SubmissionServiceImpl`): revisarlas antes de guardar evita que un valor mal escrito
 * rompa los reportes o la carga de entregas con un error 422.
 */
export interface SystemParameter {
  /** Para qué lo usa el sistema, en una frase. */
  usage: string
  /** Formato esperado, con un ejemplo. */
  format: string
  /** Mensaje de error del valor, o `null` si es válido. */
  validate: (value: string) => string | null
}

const PERCENTAGE = /^\d+(\.\d+)?$/

function percentageError(text: string): string | null {
  if (!PERCENTAGE.test(text)) return 'debe ser un número (con punto decimal si hace falta)'
  const number = Number(text)
  return number < 0 || number > 100 ? 'debe estar entre 0 y 100' : null
}

export const SYSTEM_PARAMETERS: Record<string, SystemParameter> = {
  'report.passing-percentage': {
    usage: 'Porcentaje del puntaje máximo del momento que se necesita para aprobar (reportes).',
    format: 'Un número entre 0 y 100, p. ej. 60.',
    validate: (value) => {
      const error = percentageError(value.trim())
      return error && `El porcentaje para aprobar ${error}.`
    },
  },
  'report.grade-bands': {
    usage: 'Niveles de desempeño de la distribución de notas en los reportes.',
    format: 'Nombre:porcentaje mínimo, separados por punto y coma, p. ej. Bajo:0;Básico:60;Alto:76;Sobresaliente:90.',
    validate: (value) => {
      const bands = value
        .split(';')
        .map((band) => band.trim())
        .filter((band) => band !== '')
      if (bands.length === 0) return 'Escribe al menos un nivel con la forma Nombre:porcentaje.'
      for (const band of bands) {
        const colon = band.indexOf(':')
        if (colon <= 0 || colon === band.length - 1) {
          return `"${band}" no tiene la forma Nombre:porcentaje.`
        }
        const error = percentageError(band.slice(colon + 1).trim())
        if (error) return `El porcentaje de "${band.slice(0, colon).trim()}" ${error}.`
      }
      return null
    },
  },
  'submission.max-versions': {
    usage: 'Máximo de versiones de entrega que puede subir cada grupo en un momento.',
    format: 'Un número entero mayor que cero, p. ej. 20.',
    validate: (value) =>
      /^\d+$/.test(value.trim()) && Number(value.trim()) > 0
        ? null
        : 'El máximo de versiones debe ser un número entero mayor que cero.',
  },
}
