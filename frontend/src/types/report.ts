/**
 * Tipos de datos del módulo de reportes académicos (Etapa 7, RF 12,
 * `docs/07-contrato-api.md` sección "Stage 7 — Reporting").
 */

/** Resumen de un periodo académico dentro de un reporte. */
export interface ReportPeriodSummary {
  id: number
  code: string
}

/** Resumen de un programa académico dentro de un reporte. */
export interface ReportProgramSummary {
  id: number
  code: string
  name: string
}

/** Resumen de un momento evaluativo dentro de un reporte. */
export interface ReportMomentSummary {
  id: number
  momentNumber: number
  name: string
  subject: string
  maxScore: number
}

/** Resumen de un estudiante dentro del reporte individual. */
export interface ReportStudentSummary {
  id: number
  studentCode: string
  fullName: string
  currentSemester: number
}

/** Resumen de un grupo dentro de un reporte individual o grupal. */
export interface ReportGroupSummary {
  id: number
  name: string
  projectTitle: string | null
  teacherName: string | null
}

/** Desempeño de un estudiante en un RAP (`IndividualReportData.learningOutcomes`). */
export interface IndividualOutcomeScore {
  id: number
  code: string
  description: string
  /** Puntaje individual del estudiante en este RAP; `null` si aún no hay evaluación enviada. */
  score: number | null
  /** Puntaje base del grupo en este RAP antes de ajustes individuales. */
  groupScore: number | null
  maxScore: number
  /** Porcentaje de logro (`score / maxScore * 100`), redondeado a 2 decimales. */
  percentage: number | null
}

/**
 * 1. Reporte individual (`GET /api/reports/individual?momentId=&studentId=`):
 * desempeño de un estudiante por RAP en un momento.
 */
export interface IndividualReportData {
  generatedAt: string
  period: ReportPeriodSummary
  program: ReportProgramSummary
  moment: ReportMomentSummary
  student: ReportStudentSummary
  group: ReportGroupSummary
  /** `true` cuando ya cerró la ventana de calificación del momento. */
  final: boolean
  individualGrade: number | null
  groupGrade: number | null
  percentage: number | null
  learningOutcomes: IndividualOutcomeScore[]
  submittedEvaluations: number
  pendingEvaluations: number
}

/** Desempeño del grupo en un RAP (`GroupReportData.learningOutcomes`). */
export interface GroupOutcomeScore {
  id: number
  code: string
  description: string
  score: number | null
  maxScore: number
  percentage: number | null
}

/** Puntaje individual de un integrante en un RAP dentro del reporte grupal. */
export interface GroupMemberOutcomeScore {
  id: number
  code: string
  score: number | null
  maxScore: number
  percentage: number | null
}

/** Resumen de un integrante del grupo (`GroupReportData.members`). */
export interface GroupMemberSummary {
  studentId: number
  studentCode: string
  fullName: string
  individualGrade: number | null
  /** `true` si la nota individual o algún RAP difiere de la nota del grupo. */
  adjusted: boolean
  learningOutcomes: GroupMemberOutcomeScore[]
}

/**
 * 2. Reporte por grupo (`GET /api/reports/groups/{groupId}`):
 * nota grupal, desempeño por RAP y notas individuales de sus integrantes.
 */
export interface GroupReportData {
  generatedAt: string
  period: ReportPeriodSummary
  program: ReportProgramSummary
  moment: ReportMomentSummary
  group: ReportGroupSummary
  final: boolean
  groupGrade: number | null
  percentage: number | null
  learningOutcomes: GroupOutcomeScore[]
  members: GroupMemberSummary[]
  submittedEvaluations: number
  pendingEvaluations: number
}

/** Promedio de un RAP en el consolidado del momento (`MomentReportData.learningOutcomes`). */
export interface MomentOutcomeSummary {
  id: number
  code: string
  description: string
  averageScore: number | null
  maxScore: number
  percentage: number | null
}

/**
 * Rango de la distribución de notas en la escala 0,00–5,00 de la rúbrica institucional
 * (`Bajo`, `Básico`, `Alto`, `Sobresaliente`).
 */
export interface DistributionBucket {
  range: string
  label: string
  min: number
  max: number
  count: number
  percentage: number | null
}

/**
 * 3. Reporte consolidado por momento y programa (`GET /api/reports/moments/{momentId}`):
 * promedios generales, aprobación, promedios por RAP y distribución de notas.
 */
export interface MomentReportData {
  generatedAt: string
  period: ReportPeriodSummary
  program: ReportProgramSummary
  moment: ReportMomentSummary
  final: boolean
  totalGroups: number
  gradedGroups: number
  totalStudents: number
  gradedStudents: number
  averageGroupGrade: number | null
  averageGrade: number | null
  minGrade: number | null
  maxGrade: number | null
  passingCount: number
  passingPercentage: number | null
  learningOutcomes: MomentOutcomeSummary[]
  distribution: DistributionBucket[]
}

/** Resumen agregado de un periodo dentro del reporte comparativo. */
export interface PeriodComparisonSummary {
  period: ReportPeriodSummary
  momentId: number
  momentName: string
  subject: string
  maxScore: number
  gradedGroups: number
  gradedStudents: number
  averageGrade: number | null
  passingPercentage: number | null
  distribution: DistributionBucket[]
}

/** Comparación de un RAP entre dos periodos (`ComparativeReportData.learningOutcomes`). */
export interface OutcomeComparison {
  code: string
  description: string
  averageScoreA: number | null
  maxScoreA: number | null
  percentageA: number | null
  averageScoreB: number | null
  maxScoreB: number | null
  percentageB: number | null
  /** Diferencia en puntos porcentuales (`percentageB - percentageA`). */
  deltaPercentage: number | null
}

/**
 * 4. Reporte comparativo semestre X vs. semestre Y
 * (`GET /api/reports/comparative?programId=&momentNumber=&periodAId=&periodBId=`):
 * exclusivamente agregado por cohorte, sin nombres ni filas individuales de estudiantes.
 */
export interface ComparativeReportData {
  generatedAt: string
  program: ReportProgramSummary
  momentNumber: number
  periodA: PeriodComparisonSummary
  periodB: PeriodComparisonSummary
  deltaAverageGrade: number | null
  deltaPassingPercentage: number | null
  learningOutcomes: OutcomeComparison[]
}

/** Parámetros de consulta del reporte comparativo. */
export interface ComparativeReportParams {
  programId: number
  momentNumber: number
  periodAId: number
  periodBId: number
}
