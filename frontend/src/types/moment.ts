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
  /** Texto libre que define cada programa (ADR-010), p. ej. "Bajo". */
  requirementLevel: string
  minMembers: number
  maxMembers: number
  /** Grupos que los docentes habilitaron en el momento. */
  groupCount: number
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

/** Nivel de un criterio en la configuración del momento, con su orden de presentación. */
export interface MomentLevel extends PerformanceLevel {
  displayOrder: number
}

/** Criterio de evaluación de un RAP. */
export interface EvaluationCriterion {
  id: number
  description: string
  displayOrder: number
  /** Puntaje de su nivel más alto. */
  maxScore: number
  levels: MomentLevel[]
}

/** RAP (resultado de aprendizaje) de un momento. */
export interface LearningOutcome {
  id: number
  code: string
  description: string
  maxScore: number
  criteria: EvaluationCriterion[]
}

/** Peso de un tipo de evaluador externo en la nota del momento (ADR-018). */
export interface EvaluatorWeight {
  evaluatorTypeId: number
  evaluatorTypeName: string
  /** Porcentaje mayor que 0 y hasta 100. */
  weight: number
}

/** Detalle de un momento: el resumen más sus pesos y su estructura de evaluación (`MomentDetail`). */
export interface MomentDetail extends MomentSummary {
  /** Porcentaje de la nota que aporta el docente; 100 en un momento nuevo. */
  teacherWeight: number
  evaluatorWeights: EvaluatorWeight[]
  /** Docente más tipos de evaluador; debe ser 100 para lanzar. */
  totalWeight: number
  maxScore: number
  learningOutcomes: LearningOutcome[]
}

/** Ventanas de fechas de un momento; cada una va completa o vacía (`null`). */
export interface MomentWindows {
  groupFormationStart: string | null
  groupFormationEnd: string | null
  submissionStart: string | null
  submissionEnd: string | null
  gradingStart: string | null
  gradingEnd: string | null
}

/** Datos generales de un momento que el admin define al crearlo o editarlo. */
export interface MomentData extends MomentWindows {
  periodId: number
  number: number
  name: string
  subject: string
  semesterFrom: number
  semesterTo: number
  requirementLevel: string
  minMembers: number
  maxMembers: number
}

/** Cuerpo de `POST /api/moments` (`NewMoment` en `docs/07`). */
export interface NewMomentRequest extends MomentData {
  programId: number
}

/** Cuerpo de `POST /api/moments/copy`: sin `programId`, copia todos los programas del alcance. */
export interface CopyMomentsRequest {
  fromPeriodId: number
  toPeriodId: number
  programId?: number
}

/** Cuerpo de `POST /api/moments/{id}/learning-outcomes` (y de su `PATCH`, con lo que cambie). */
export interface LearningOutcomeRequest {
  code: string
  description: string
}

/** Cuerpo de `POST /api/learning-outcomes/{id}/criteria`; sin `displayOrder`, va de último. */
export interface CriterionRequest {
  description: string
  displayOrder?: number
}

/** Cuerpo de `POST /api/criteria/{id}/levels`; el puntaje es al menos 0, con hasta 2 decimales. */
export interface LevelRequest {
  name: string
  description?: string | null
  score: number
  displayOrder?: number
}

/** Cuerpo de `PUT /api/moments/{id}/weights`: reemplaza todos los pesos del momento. */
export interface MomentWeightsRequest {
  teacherWeight: number
  evaluatorWeights: { evaluatorTypeId: number; weight: number }[]
}

/** Lo que muestra la pantalla antes de lanzar un momento (`LaunchCheck` en `docs/07`). */
export interface LaunchCheck {
  momentId: number
  /** Sin problemas y todavía en configuración. */
  canLaunch: boolean
  /** Todo lo que bloquea el lanzamiento, de una vez. */
  problems: string[]
  minMembers: number
  /** No bloquean: piden confirmación y quedan como están. */
  groupsBelowMinimum: { id: number; name: string; teacher: { id: number; fullName: string }; memberCount: number }[]
  /** Tipos asignados en sus grupos pero sin peso: su evaluación no contará. */
  evaluatorTypesWithoutWeight: { evaluatorTypeId: number; evaluatorTypeName: string; evaluatorCount: number }[]
}
