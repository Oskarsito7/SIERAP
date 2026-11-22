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

/** Datos de un administrador nuevo; la facultad la agrega el servicio (`NewAdminRequest`). */
export interface NewAdminRequest {
  firstNames: string
  lastNames: string
  documentType: DocumentType
  documentNumber: string
  email: string
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

/**
 * Cuerpo de `PATCH /api/users/{id}` (`UpdateUserRequest`): todos los campos son
 * opcionales y solo se cambian los que se envían. `studentCode` y `currentSemester`
 * aplican únicamente a estudiantes.
 */
export interface UpdateUserRequest {
  active?: boolean
  firstNames?: string
  lastNames?: string
  documentType?: DocumentType
  documentNumber?: string
  email?: string
  studentCode?: string
  currentSemester?: number
}

/** Cuerpo de `PATCH /api/students/semester` (`SetSemesterRequest`). */
export interface SetSemesterRequest {
  /** De 1 a 500 estudiantes, sin repetidos. */
  studentIds: number[]
  /** Semestre que se les asigna, desde 1. */
  currentSemester: number
}

/** Fila del CSV de estudiantes con su validación (`StudentImportRowResponse`). */
export interface StudentImportRow {
  /** Línea del archivo (la cabecera es la 1). */
  row: number
  firstNames: string
  lastNames: string
  documentType: string
  documentNumber: string
  email: string
  studentCode: string
  currentSemester: string
  /** Errores de la fila en español; vacío si es válida. */
  errors: string[]
}

/** Respuesta de `POST /api/students/bulk/preview` (`StudentImportPreviewResponse`): no escribe nada. */
export interface StudentImportPreview {
  rows: StudentImportRow[]
  valid: number
  invalid: number
}

/** Respuesta de `POST /api/students/bulk` (`StudentImportResultResponse`). */
export interface StudentImportResult {
  created: number
  errors: { row: number; message: string }[]
}
