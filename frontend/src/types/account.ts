import type { DocumentType, UserSummary } from './auth'

/** Tipo de evaluador del catálogo (espejo de `EvaluatorTypeResponse`). */
export interface EvaluatorType {
  id: number
  name: string
  description: string | null
  /** Solo los activos se asignan a evaluadores nuevos. */
  active: boolean
  /** Evaluadores externos de este tipo. */
  evaluatorCount: number
}

/** Cuerpo de `POST /api/external-evaluators` (`NewExternalEvaluatorRequest`). */
export interface NewExternalEvaluatorRequest {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
  evaluatorTypeId: number
  organization?: string
  position?: string
  graduationYear?: number
}

/**
 * Docente en la lista de "Docentes" del admin (wireframe AD-06). Propuesta para
 * `GET /api/teachers`, que hoy devuelve `UserSummary` sin documento ni grupos.
 */
export interface TeacherSummary extends UserSummary {
  documentType: DocumentType
  documentNumber: string
  /** Grupos que habilitó en el periodo actual. */
  groupCount: number
}

/** Cuerpo de `POST /api/teachers`; la facultad es la del admin que lo crea. */
export interface NewTeacherRequest {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
}
