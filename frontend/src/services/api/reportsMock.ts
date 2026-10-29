import { AxiosError, type AxiosResponse } from 'axios'

import { todayIso } from '@/lib/dates'
import type { ApiError } from '@/types/api'
import type { Group } from '@/types/group'
import type { MomentDetail } from '@/types/moment'
import type {
  ComparativeOutcome,
  ComparativePeriod,
  ComparativeReportData,
  ComparativeReportQuery,
  GradeBucket,
  GroupReportData,
  IndividualReportData,
  MomentReportData,
  OutcomeAchievement,
  ReportGroup,
  ReportMoment,
} from '@/types/report'

import { catalogService } from './catalogService'
import { groupService } from './groupService'
import { momentService } from './momentService'

/*
 * Datos de ejemplo para la pantalla de reportes mientras D2 publica las rutas de
 * `GET /api/reports/...` (docs/06, Etapa 7). Usan el momento, los grupos y los RAP reales
 * para que los selectores funcionen, pero las cifras son inventadas. Tienen la misma
 * forma que el contrato de `docs/07`; se borran cuando existan las rutas.
 */

/** Porcentaje de logro de cada RAP en el periodo actual y en el anterior (wireframe AD-08). */
const SAMPLE_PERCENTAGES = [78, 71, 66, 69, 74]
const PREVIOUS_PERCENTAGES = [72, 65, 58, 70, 61]

/** Franjas de la rúbrica de la UPC y qué parte de los estudiantes cae en cada una. */
const BUCKETS = [
  { range: '0.00 - 2.99', label: 'Bajo', min: 0, max: 2.99 },
  { range: '3.00 - 3.79', label: 'Básico', min: 3, max: 3.79 },
  { range: '3.80 - 4.49', label: 'Alto', min: 3.8, max: 4.49 },
  { range: '4.50 - 5.00', label: 'Sobresaliente', min: 4.5, max: 5 },
]
const SHARES = [0.1, 0.3, 0.4, 0.2]
const PREVIOUS_SHARES = [0.16, 0.37, 0.31, 0.16]

/** Redondea a dos decimales, como el backend (`setScale(2, HALF_UP)`). */
function round(value: number): number {
  return Math.round(value * 100) / 100
}

function sample(list: number[], index: number): number {
  return list[index % list.length]
}

function ratio(score: number, maxScore: number): number {
  return maxScore > 0 ? round((score / maxScore) * 100) : 0
}

/** Error con la forma de la API, para los casos en que la ruta real respondería 4xx. */
function apiError(status: number, message: string): AxiosError {
  const data: ApiError = { timestamp: '', status, error: '', message, path: '', fieldErrors: {} }
  return new AxiosError(message, 'ERR_BAD_REQUEST', undefined, null, { status, data } as AxiosResponse)
}

function reportMoment(moment: MomentDetail): ReportMoment {
  return {
    id: moment.id,
    momentNumber: moment.number,
    name: moment.name,
    subject: moment.subject,
    maxScore: moment.maxScore,
  }
}

function reportGroup(group: Group): ReportGroup {
  return { id: group.id, name: group.name, projectTitle: group.projectTitle, teacherName: group.teacher.fullName }
}

function header(moment: MomentDetail) {
  return {
    generatedAt: `${todayIso()}T10:30:00`,
    period: { id: moment.periodId, code: moment.periodCode },
    program: { id: moment.programId, code: String(moment.programId), name: moment.programName },
    moment: reportMoment(moment),
    final: moment.gradingEnd !== null && todayIso() > moment.gradingEnd,
  }
}

/** Puntaje de cada RAP; `penalty` baja el primero, como un ajuste individual. */
function outcomes(moment: MomentDetail, penalty = 0): OutcomeAchievement[] {
  return moment.learningOutcomes.map((outcome, index) => {
    const percent = sample(SAMPLE_PERCENTAGES, index) - (index === 0 ? penalty : 0)
    const score = round((outcome.maxScore * percent) / 100)
    return {
      id: outcome.id,
      code: outcome.code,
      description: outcome.description,
      score,
      maxScore: outcome.maxScore,
      percentage: ratio(score, outcome.maxScore),
    }
  })
}

function total(list: OutcomeAchievement[]): number {
  return round(list.reduce((sum, item) => sum + item.score, 0))
}

/** Reparte `count` estudiantes en las 4 franjas sin perder ninguno por el redondeo. */
function distribution(count: number, shares: number[]): GradeBucket[] {
  const counts = shares.map((share) => Math.floor(count * share))
  counts[2] += count - counts.reduce((sum, value) => sum + value, 0)
  return BUCKETS.map((bucket, index) => ({
    ...bucket,
    count: counts[index],
    percentage: count > 0 ? round((counts[index] / count) * 100) : 0,
  }))
}

export async function sampleMomentReport(momentId: number): Promise<MomentReportData> {
  const [moment, groups] = await Promise.all([momentService.get(momentId), groupService.list(momentId)])
  // Un grupo queda sin nota para mostrar la diferencia entre grupos y grupos calificados.
  const graded = groups.length > 1 ? groups.slice(0, -1) : groups
  const totalStudents = groups.reduce((sum, group) => sum + group.memberCount, 0)
  const gradedStudents = graded.reduce((sum, group) => sum + group.memberCount, 0)
  const buckets = distribution(gradedStudents, SHARES)
  const averages = outcomes(moment)
  const average = gradedStudents > 0 ? total(averages) : null
  return {
    ...header(moment),
    totalGroups: groups.length,
    gradedGroups: graded.length,
    totalStudents,
    gradedStudents,
    averageGroupGrade: average === null ? null : round(average + 0.06),
    averageGrade: average,
    // Sin estudiantes en la franja baja, la nota mínima queda por encima de ella.
    minGrade: gradedStudents > 0 ? round(moment.maxScore * (buckets[0].count > 0 ? 0.53 : 0.62)) : null,
    maxGrade: gradedStudents > 0 ? round(moment.maxScore * 0.97) : null,
    passingCount: gradedStudents - buckets[0].count,
    passingPercentage: gradedStudents > 0 ? round(((gradedStudents - buckets[0].count) / gradedStudents) * 100) : null,
    learningOutcomes: averages.map((item) => ({
      id: item.id,
      code: item.code,
      description: item.description ?? '',
      averageScore: item.score,
      maxScore: item.maxScore,
      percentage: item.percentage,
    })),
    distribution: buckets,
  }
}

export async function sampleGroupReport(groupId: number): Promise<GroupReportData> {
  const group = await groupService.get(groupId)
  const moment = await momentService.get(group.momentId)
  const groupOutcomes = outcomes(moment)
  return {
    ...header(moment),
    group: reportGroup(group),
    groupGrade: total(groupOutcomes),
    percentage: ratio(total(groupOutcomes), moment.maxScore),
    learningOutcomes: groupOutcomes,
    // El primer integrante tiene un ajuste individual en el primer RAP.
    members: [...group.members]
      .sort((a, b) => a.fullName.localeCompare(b.fullName, 'es'))
      .map((member, index) => {
        const memberOutcomes = outcomes(moment, index === 0 ? 20 : 0)
        return {
          studentId: member.id,
          studentCode: member.studentCode ?? '',
          fullName: member.fullName,
          individualGrade: total(memberOutcomes),
          adjusted: index === 0,
          learningOutcomes: memberOutcomes,
        }
      }),
    submittedEvaluations: group.evaluators.length + 1,
    pendingEvaluations: 0,
  }
}

export async function sampleIndividualReport(momentId: number, studentId: number): Promise<IndividualReportData> {
  const [moment, groups] = await Promise.all([momentService.get(momentId), groupService.list(momentId)])
  const group = groups.find((item) => item.members.some((member) => member.id === studentId))
  const member = group?.members.find((item) => item.id === studentId)
  if (!group || !member) throw apiError(422, 'El estudiante no pertenece a ningún grupo de este momento.')
  const adjusted = [...group.members].sort((a, b) => a.fullName.localeCompare(b.fullName, 'es'))[0].id === studentId
  const groupOutcomes = outcomes(moment)
  const studentOutcomes = outcomes(moment, adjusted ? 20 : 0)
  const individualGrade = total(studentOutcomes)
  return {
    ...header(moment),
    student: {
      id: member.id,
      studentCode: member.studentCode ?? '',
      fullName: member.fullName,
      currentSemester: moment.semesterFrom,
    },
    group: reportGroup(group),
    individualGrade,
    groupGrade: total(groupOutcomes),
    percentage: ratio(individualGrade, moment.maxScore),
    learningOutcomes: studentOutcomes.map((item, index) => ({
      ...item,
      groupScore: groupOutcomes[index].score,
    })),
    submittedEvaluations: group.evaluators.length + 1,
    pendingEvaluations: 0,
  }
}

export async function sampleComparativeReport(query: ComparativeReportQuery): Promise<ComparativeReportData> {
  if (query.periodAId === query.periodBId) {
    throw apiError(422, 'Debe elegir dos periodos académicos distintos para comparar.')
  }
  const [periods, momentsA, momentsB] = await Promise.all([
    catalogService.periods(),
    momentService.list(query.periodAId),
    momentService.list(query.periodBId),
  ])
  const find = (list: typeof momentsA) =>
    list.find((moment) => moment.programId === query.programId && moment.number === query.momentNumber)
  const summaryA = find(momentsA)
  const summaryB = find(momentsB)
  if (!summaryA || !summaryB) {
    throw apiError(404, `El programa no tiene el momento ${query.momentNumber} en uno de los dos periodos.`)
  }
  const moment = await momentService.get(summaryB.id)
  const periodOf = (id: number) => ({ id, code: periods.find((period) => period.id === id)?.code ?? String(id) })

  const rows: ComparativeOutcome[] = moment.learningOutcomes.map((outcome, index) => {
    const percentageA = sample(PREVIOUS_PERCENTAGES, index)
    const percentageB = sample(SAMPLE_PERCENTAGES, index)
    return {
      code: outcome.code,
      description: outcome.description,
      averageScoreA: round((outcome.maxScore * percentageA) / 100),
      maxScoreA: outcome.maxScore,
      percentageA,
      averageScoreB: round((outcome.maxScore * percentageB) / 100),
      maxScoreB: outcome.maxScore,
      percentageB,
      deltaPercentage: round(percentageB - percentageA),
    }
  })
  const side = (
    period: { id: number; code: string },
    momentId: number,
    students: number,
    shares: number[],
    averageGrade: number,
  ): ComparativePeriod => {
    const buckets = distribution(students, shares)
    return {
      period,
      momentId,
      momentName: moment.name,
      subject: moment.subject,
      maxScore: moment.maxScore,
      gradedGroups: Math.ceil(students / moment.maxMembers),
      gradedStudents: students,
      averageGrade,
      passingPercentage: round(((students - buckets[0].count) / students) * 100),
      distribution: buckets,
    }
  }
  const periodA = side(periodOf(query.periodAId), summaryA.id, 19, PREVIOUS_SHARES, 3.78)
  const periodB = side(periodOf(query.periodBId), summaryB.id, 20, SHARES, 4.12)
  return {
    generatedAt: `${todayIso()}T10:30:00`,
    program: { id: query.programId, code: String(query.programId), name: summaryB.programName },
    momentNumber: query.momentNumber,
    periodA,
    periodB,
    deltaAverageGrade: round(4.12 - 3.78),
    deltaPassingPercentage: round((periodB.passingPercentage ?? 0) - (periodA.passingPercentage ?? 0)),
    learningOutcomes: rows,
  }
}
