import type { DocumentType } from './auth'

/** Tipo de evaluador del catálogo (espejo de `EvaluatorTypeResponse`). */
export interface EvaluatorType {
  id: number
  name: string
  description: string | null
  /** Solo los activos se asignan a evaluadores nuevos. */
  active: boolean
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
