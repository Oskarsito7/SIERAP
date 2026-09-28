/** Estado del ciclo de vida de un momento (ADR-012). */
export type MomentStatus = 'CONFIGURATION' | 'LAUNCHED'

/**
 * Resumen de un momento (espejo de `MomentSummaryResponse`, `MomentSummary` en
 * `docs/07`). Las fechas llegan como `YYYY-MM-DD` y son `null` mientras el admin no
 * defina la ventana. Los indicadores `*Open` los calcula el backend con su reloj:
 * sirven para habilitar o deshabilitar botones; fuera de ventana la API responde 422.
 */
export interface MomentSummary {
  id: number
  number: number
  name: string
  subject: string
  programId: number
  programName: string
  periodId: number
  periodCode: string
  semesterFrom: number
  semesterTo: number
  minMembers: number
  maxMembers: number
  status: MomentStatus
  groupFormationStart: string | null
  groupFormationEnd: string | null
  submissionStart: string | null
  submissionEnd: string | null
  gradingStart: string | null
  gradingEnd: string | null
  groupFormationOpen: boolean
  submissionOpen: boolean
  gradingOpen: boolean
}

/** Nivel de desempeño de un criterio. */
export interface PerformanceLevel {
  id: number
  name: string
  description: string | null
  score: number
}

/** Criterio de evaluación de un RAP. */
export interface EvaluationCriterion {
  id: number
  description: string
  levels: PerformanceLevel[]
}

/** RAP (resultado de aprendizaje) de un momento. */
export interface LearningOutcome {
  id: number
  code: string
  description: string
  maxScore: number
  criteria: EvaluationCriterion[]
}

/** Detalle de un momento: el resumen más su estructura de evaluación (`MomentDetail`). */
export interface MomentDetail extends MomentSummary {
  maxScore: number
  learningOutcomes: LearningOutcome[]
}
