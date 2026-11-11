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

/** Datos de un docente nuevo; la facultad la agrega el servicio (la del admin). */
export interface NewTeacherRequest {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
}

/**
 * Estudiante de la lista "Mis estudiantes" (espejo de `StudentResponse`, `Student` en
 * `docs/07`): el resumen de la cuenta, sus datos académicos y cómo llegó al sistema.
 */
export interface Student extends UserSummary {
  studentCode: string
  currentSemester: number
  /** Código de registro con el que se registró; `null` si alguien creó su cuenta. */
  registrationCode: string | null
  /** Quien creó la cuenta o el docente dueño del código; `null` si no se sabe. */
  createdBy: { id: number; fullName: string } | null
  /** `false` si quien consulta no puede reenviarle el enlace ni activar o desactivar su cuenta. */
  manageable: boolean
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
