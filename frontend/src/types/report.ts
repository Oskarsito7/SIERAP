/*
 * Reportes de la Etapa 7 (`docs/07-contrato-api.md`, "Stage 7 — Reporting"). Todas las
 * notas y porcentajes llegan con 2 decimales; los porcentajes van de 0 a 100.
 */

/** Periodo académico dentro de un reporte. */
export interface ReportPeriod {
  id: number
  code: string
}

/** Programa dentro de un reporte. */
export interface ReportProgram {
  id: number
  code: string
  name: string
}

/** Momento dentro de un reporte. */
export interface ReportMoment {
  id: number
  momentNumber: number
  name: string
  subject: string
  maxScore: number
}

/** Grupo dentro de un reporte. */
export interface ReportGroup {
  id: number
  name: string
  projectTitle: string | null
  teacherName: string
}

/** Logro en un RAP: puntaje sobre su máximo y su porcentaje. */
export interface OutcomeAchievement {
  id: number
  code: string
  description?: string
  score: number
  maxScore: number
  percentage: number
}

/** Logro de un estudiante en un RAP, con el puntaje del grupo antes de sus ajustes. */
export interface IndividualOutcomeAchievement extends OutcomeAchievement {
  groupScore: number
}

/** Reporte individual: desempeño de un estudiante en un momento (`IndividualReportData`). */
export interface IndividualReportData {
  generatedAt: string
  period: ReportPeriod
  program: ReportProgram
  moment: ReportMoment
  student: { id: number; studentCode: string; fullName: string; currentSemester: number }
  group: ReportGroup
  /** `true` cuando ya cerró la ventana de calificación. */
  final: boolean
  /** `null` mientras el grupo no tiene evaluaciones enviadas que cuenten. */
  individualGrade: number | null
  groupGrade: number | null
  percentage: number | null
  learningOutcomes: IndividualOutcomeAchievement[]
  submittedEvaluations: number
  pendingEvaluations: number
}

/** Integrante en el reporte grupal. */
export interface GroupReportMember {
  studentId: number
  studentCode: string
  fullName: string
  individualGrade: number | null
  /** `true` si su nota o algún RAP difiere de los del grupo (ajuste individual). */
  adjusted: boolean
  learningOutcomes: OutcomeAchievement[]
}

/** Reporte grupal: desempeño del grupo por RAP y nota de cada integrante (`GroupReportData`). */
export interface GroupReportData {
  generatedAt: string
  period: ReportPeriod
  program: ReportProgram
  moment: ReportMoment
  group: ReportGroup
  final: boolean
  /** `null` mientras no hay evaluaciones enviadas que cuenten. */
  groupGrade: number | null
  percentage: number | null
  learningOutcomes: OutcomeAchievement[]
  members: GroupReportMember[]
  submittedEvaluations: number
  pendingEvaluations: number
}

/**
 * Franja de la distribución de notas. Sus límites y su nombre salen de los parámetros
 * globales, relativos a la nota máxima del momento: la pantalla pinta los que lleguen.
 */
export interface GradeBucket {
  range: string
  label: string
  min: number
  max: number
  count: number
  percentage: number
}

/** Promedio de un RAP entre los estudiantes calificados del momento. */
export interface MomentOutcomeAverage {
  id: number
  code: string
  description: string
  averageScore: number
  maxScore: number
  percentage: number
}

/** Reporte consolidado de un momento y su programa (`MomentReportData`). */
export interface MomentReportData {
  generatedAt: string
  period: ReportPeriod
  program: ReportProgram
  moment: ReportMoment
  final: boolean
  totalGroups: number
  gradedGroups: number
  totalStudents: number
  gradedStudents: number
  /** Los promedios y la nota mínima y máxima son `null` si nadie tiene nota. */
  averageGroupGrade: number | null
  averageGrade: number | null
  minGrade: number | null
  maxGrade: number | null
  /** Estudiantes calificados con nota individual de aprobado (un parámetro global). */
  passingCount: number
  passingPercentage: number | null
  learningOutcomes: MomentOutcomeAverage[]
  distribution: GradeBucket[]
}

/** Cifras agregadas de un periodo en el reporte comparativo. */
export interface ComparativePeriod {
  period: ReportPeriod
  momentId: number
  momentName: string
  subject: string
  maxScore: number
  gradedGroups: number
  gradedStudents: number
  averageGrade: number | null
  passingPercentage: number | null
  distribution: GradeBucket[]
}

/**
 * Un RAP en los dos periodos, emparejado por código. Si solo existe en uno, los campos
 * del otro y `deltaPercentage` son `null`.
 */
export interface ComparativeOutcome {
  code: string
  description: string
  averageScoreA: number | null
  maxScoreA: number | null
  percentageA: number | null
  averageScoreB: number | null
  maxScoreB: number | null
  percentageB: number | null
  /** `percentageB - percentageA`, en puntos porcentuales. */
  deltaPercentage: number | null
}

/**
 * Reporte comparativo: semestre X frente a semestre Y del mismo programa y número de
 * momento (`ComparativeReportData`). Solo cifras agregadas, sin estudiantes.
 */
export interface ComparativeReportData {
  generatedAt: string
  program: ReportProgram
  momentNumber: number
  periodA: ComparativePeriod
  periodB: ComparativePeriod
  deltaAverageGrade: number | null
  deltaPassingPercentage: number | null
  learningOutcomes: ComparativeOutcome[]
}

/** Parámetros de `GET /api/reports/comparative`. */
export interface ComparativeReportQuery {
  programId: number
  momentNumber: number
  periodAId: number
  periodBId: number
}
