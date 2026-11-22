import type {
  ComparativeReportData,
  GradeBucket,
  GroupReportData,
  IndividualReportData,
  MomentReportData,
} from '@/types/report'

const header = {
  generatedAt: '2026-11-29T10:30:00',
  period: { id: 1, code: '2026-2' },
  program: { id: 1, code: '108', name: 'Ingeniería de Sistemas' },
  moment: { id: 1, momentNumber: 1, name: 'Momento 1', subject: 'Programación II', maxScore: 5 },
  final: true,
}

const group = { id: 7, name: 'Grupo 1', projectTitle: 'SIERAP', teacherName: 'Marta Díaz' }

function buckets(counts: number[], total: number): GradeBucket[] {
  const labels = ['Bajo', 'Básico', 'Alto', 'Sobresaliente']
  const limits = [
    [0, 2.99],
    [3, 3.79],
    [3.8, 4.49],
    [4.5, 5],
  ]
  return counts.map((count, index) => ({
    range: `${limits[index][0].toFixed(2)} - ${limits[index][1].toFixed(2)}`,
    label: labels[index],
    min: limits[index][0],
    max: limits[index][1],
    count,
    percentage: Math.round((count / total) * 10000) / 100,
  }))
}

/** Reporte de prueba del Momento 1 con 20 de 24 estudiantes calificados. */
export function momentReportFixture(overrides: Partial<MomentReportData> = {}): MomentReportData {
  return {
    ...header,
    totalGroups: 6,
    gradedGroups: 5,
    totalStudents: 24,
    gradedStudents: 20,
    averageGroupGrade: 4.18,
    averageGrade: 4.12,
    minGrade: 2.65,
    maxGrade: 4.85,
    passingCount: 18,
    passingPercentage: 90,
    learningOutcomes: [
      {
        id: 10,
        code: 'RAP1',
        description: 'Analiza el problema',
        averageScore: 0.47,
        maxScore: 0.6,
        percentage: 78.33,
      },
      { id: 11, code: 'RAP2', description: 'Diseña la solución', averageScore: 0.6, maxScore: 1, percentage: 60 },
    ],
    distribution: buckets([2, 6, 8, 4], 20),
    ...overrides,
  }
}

/** Reporte de prueba del Grupo 1: Juan Rojas tiene un ajuste en RAP1. */
export function groupReportFixture(overrides: Partial<GroupReportData> = {}): GroupReportData {
  return {
    ...header,
    group,
    groupGrade: 4.52,
    percentage: 90.4,
    learningOutcomes: [
      { id: 10, code: 'RAP1', description: 'Analiza el problema', score: 0.6, maxScore: 0.6, percentage: 100 },
      { id: 11, code: 'RAP2', description: 'Diseña la solución', score: 0.8, maxScore: 1, percentage: 80 },
    ],
    members: [
      {
        studentId: 40,
        studentCode: '20241001',
        fullName: 'Juan Rojas',
        individualGrade: 4.4,
        adjusted: true,
        learningOutcomes: [
          { id: 10, code: 'RAP1', score: 0.48, maxScore: 0.6, percentage: 80 },
          { id: 11, code: 'RAP2', score: 0.8, maxScore: 1, percentage: 80 },
        ],
      },
      {
        studentId: 41,
        studentCode: '20241002',
        fullName: 'Ana Pérez',
        individualGrade: 4.52,
        adjusted: false,
        learningOutcomes: [
          { id: 10, code: 'RAP1', score: 0.6, maxScore: 0.6, percentage: 100 },
          { id: 11, code: 'RAP2', score: 0.8, maxScore: 1, percentage: 80 },
        ],
      },
    ],
    submittedEvaluations: 2,
    pendingEvaluations: 1,
    ...overrides,
  }
}

/** Reporte individual de prueba de Juan Rojas. */
export function individualReportFixture(overrides: Partial<IndividualReportData> = {}): IndividualReportData {
  return {
    ...header,
    student: { id: 40, studentCode: '20241001', fullName: 'Juan Rojas', currentSemester: 4 },
    group,
    individualGrade: 4.4,
    groupGrade: 4.52,
    percentage: 88,
    learningOutcomes: [
      {
        id: 10,
        code: 'RAP1',
        description: 'Analiza el problema',
        score: 0.48,
        groupScore: 0.6,
        maxScore: 0.6,
        percentage: 80,
      },
    ],
    submittedEvaluations: 2,
    pendingEvaluations: 0,
    ...overrides,
  }
}

/** Comparativo de prueba: Momento 1 de 2026-1 frente a 2026-2. */
export function comparativeReportFixture(overrides: Partial<ComparativeReportData> = {}): ComparativeReportData {
  const side = (id: number, code: string, averageGrade: number, passingPercentage: number, counts: number[]) => ({
    period: { id, code },
    momentId: id + 10,
    momentName: 'Momento 1',
    subject: 'Programación II',
    maxScore: 5,
    gradedGroups: 5,
    gradedStudents: 20,
    averageGrade,
    passingPercentage,
    distribution: buckets(counts, 20),
  })
  return {
    generatedAt: '2026-11-29T10:30:00',
    program: header.program,
    momentNumber: 1,
    periodA: side(2, '2026-1', 3.78, 85, [3, 7, 6, 4]),
    periodB: side(1, '2026-2', 4.12, 90, [2, 6, 8, 4]),
    deltaAverageGrade: 0.34,
    deltaPassingPercentage: 5,
    learningOutcomes: [
      {
        code: 'RAP1',
        description: 'Analiza el problema',
        averageScoreA: 0.43,
        maxScoreA: 0.6,
        percentageA: 72,
        averageScoreB: 0.47,
        maxScoreB: 0.6,
        percentageB: 78,
        deltaPercentage: 6,
      },
      {
        code: 'RAP4',
        description: 'Documenta el código',
        averageScoreA: 0.7,
        maxScoreA: 1,
        percentageA: 70,
        averageScoreB: 0.69,
        maxScoreB: 1,
        percentageB: 69,
        deltaPercentage: -1,
      },
      {
        code: 'RAP6',
        description: 'Solo existe en 2026-2',
        averageScoreA: null,
        maxScoreA: null,
        percentageA: null,
        averageScoreB: 0.5,
        maxScoreB: 1,
        percentageB: 50,
        deltaPercentage: null,
      },
    ],
    ...overrides,
  }
}
