/** Puntaje de un RAP: lo obtenido sobre su máximo (`docs/07-contrato-api.md`, "Results"). */
export interface OutcomeScore {
  id: number
  code: string
  score: number
  maxScore: number
}

/** Nota individual de un integrante, en total y por RAP. */
export interface MemberResult {
  studentId: number
  fullName: string
  /** `null` mientras no hay evaluaciones enviadas. */
  individualGrade: number | null
  learningOutcomes: OutcomeScore[]
}

/** Resultados de un grupo para su docente: vista previa mientras la calificación está abierta. */
export interface GroupResults {
  groupId: number
  /** `true` cuando ya cerró la ventana de calificación. */
  final: boolean
  maxScore: number
  /** `null` mientras no hay evaluaciones enviadas. */
  groupGrade: number | null
  groupLearningOutcomes: OutcomeScore[]
  members: MemberResult[]
  submittedEvaluations: number
  /** Evaluadores asignados (y el docente) que todavía no envían su evaluación. */
  pendingEvaluations: number
}

/** Desempeño del estudiante en un RAP. */
export interface StudentOutcomeScore {
  code: string
  description: string
  score: number
  maxScore: number
}

/** Comentario de un evaluador: se muestra su tipo, nunca su nombre. */
export interface EvaluatorComment {
  evaluatorKind: string
  comment: string
}

/** Lo que ve el estudiante de un momento; las notas llegan cuando cierra la calificación. */
export interface StudentResult {
  momentId: number
  momentName: string
  periodCode: string
  groupId: number
  groupName: string
  projectTitle: string | null
  available: boolean
  /** Su nota individual; `null` mientras no está disponible. */
  finalGrade: number | null
  groupGrade: number | null
  maxScore: number
  learningOutcomes: StudentOutcomeScore[]
  comments: EvaluatorComment[]
}
