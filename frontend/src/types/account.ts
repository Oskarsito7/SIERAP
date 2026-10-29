import type { DocumentType } from './auth'

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

/** Datos de un docente nuevo; la facultad la agrega el servicio (la del admin). */
export interface NewTeacherRequest {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
}

/** Cuerpo de `POST /api/students` (`NewStudentRequest`). */
export interface NewStudentRequest {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
  programId: number
  studentCode: string
  /** Semestre que cursa, desde 1. */
  currentSemester: number
}
