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
