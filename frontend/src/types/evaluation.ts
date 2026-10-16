import type { PerformanceLevel } from './moment'

/** Estado de una evaluación (ADR-012, `EvaluationStatus` del backend). */
export type EvaluationStatus = 'DRAFT' | 'SUBMITTED'

/** Versión de la entrega que se califica. */
export interface SubmissionRef {
  id: number
  versionNumber: number
  late: boolean
}

/** Criterio con sus niveles y el nivel elegido para el grupo. */
export interface CriterionForm {
  id: number
  description: string
  levels: PerformanceLevel[]
  /** `null` si todavía no se calificó. */
  levelId: number | null
}

/** RAP con sus criterios en el formulario de calificación. */
export interface OutcomeForm {
  id: number
  code: string
  description: string
  maxScore: number
  criteria: CriterionForm[]
}

/** Ajuste individual: nivel de un criterio para un solo estudiante (ADR-018). */
export interface Adjustment {
  criterionId: number
  levelId: number
}

/** Integrante con sus ajustes y su nota en esta evaluación. */
export interface MemberForm {
  id: number
  fullName: string
  /** Solo los criterios en que su nivel difiere del grupo. */
  adjustments: Adjustment[]
  totalScore: number
}

/** Evaluación propia de un grupo, espejo de `EvaluationFormResponse` (`EvaluationForm` en `docs/07`). */
export interface EvaluationForm {
  /** `null` si todavía no se empezó. */
  id: number | null
  groupId: number
  status: EvaluationStatus | null
  evaluatedAt: string | null
  comment: string | null
  /** El periodo de calificación del grupo está abierto hoy. */
  canEdit: boolean
  submissionToGrade: SubmissionRef | null
  learningOutcomes: OutcomeForm[]
  members: MemberForm[]
  /** Nota del grupo, solo con los niveles del grupo. */
  totalScore: number
  maxScore: number
  complete: boolean
}

/** Cuerpo de `PUT /api/groups/{id}/evaluation`; lo que no se envía se conserva. */
export interface SaveEvaluationRequest {
  levels?: { criterionId: number; levelId: number }[]
  comment?: string
  /** Si se envía, reemplaza todos los ajustes anteriores. */
  adjustments?: { studentId: number; criterionId: number; levelId: number }[]
}

/** Evaluación en la lista de evaluaciones de un grupo (`GET /api/groups/{id}/evaluations`). */
export interface EvaluationSummary {
  id: number
  evaluator: { id: number; fullName: string; kind: string }
  status: EvaluationStatus
  evaluatedAt: string
  totalScore: number
  comment: string | null
}
