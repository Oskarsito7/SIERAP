/** Facultad (`Faculty` en `docs/07`, "Stage 3 — Catalogs"). */
export interface Faculty {
  id: number
  code: string
  name: string
  programCount: number
}

/** Programa académico de una facultad. */
export interface Program {
  id: number
  code: string
  name: string
  facultyId: number
  facultyName: string
  /** Momentos del programa en el periodo actual. */
  currentPeriodMomentCount: number
  studentCount: number
}

/** Estado de un periodo, calculado por el backend con su reloj. */
export type PeriodStatus = 'UPCOMING' | 'CURRENT' | 'CLOSED'

/** Periodo académico, p. ej. "2026-2". Las fechas llegan como `YYYY-MM-DD`. */
export interface AcademicPeriod {
  id: number
  code: string
  startDate: string
  endDate: string
  /** Límite de la UPC: la calificación de los momentos del periodo termina a más tardar ese día. */
  upcEvaluationDeadline: string
  status: PeriodStatus
}

/** Parámetro global del sistema (`GlobalParameter` en `docs/07`). */
export interface GlobalParameter {
  key: string
  value: string
  description: string | null
}

/** Cuerpo para crear o editar una facultad o un programa (el programa lleva su facultad al crearlo). */
export interface CatalogEntryRequest {
  code: string
  name: string
}

/** Cuerpo para crear o editar un periodo: siempre van los cuatro campos. */
export interface PeriodRequest {
  code: string
  startDate: string
  endDate: string
  upcEvaluationDeadline: string
}

/** Cuerpo para crear o editar un tipo de evaluador. */
export interface EvaluatorTypeRequest {
  name: string
  description: string | null
}
