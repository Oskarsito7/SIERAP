import { todayIso } from '@/lib/dates'
import type { EvaluationForm } from '@/types/evaluation'
import type { GroupResults, OutcomeScore, StudentResult } from '@/types/results'

import { evaluationService } from './evaluationService'
import { groupService } from './groupService'
import { momentService } from './momentService'

/*
 * Datos de ejemplo para las pantallas de resultados mientras D1 publica
 * `GET /api/groups/{id}/results` y `GET /api/students/me/results` (docs/06, Etapa 6).
 * Tienen la misma forma que el contrato de `docs/07`; se borran cuando existan las rutas.
 */

/** Redondea a dos decimales, como el backend (`setScale(2, HALF_UP)`). */
function round(value: number): number {
  return Math.round(value * 100) / 100
}

/** Puntaje de cada RAP con los niveles del grupo, cambiando los criterios ajustados de un estudiante. */
function outcomeScores(form: EvaluationForm, studentId?: number): OutcomeScore[] {
  const adjustments = form.members.find((member) => member.id === studentId)?.adjustments ?? []
  return form.learningOutcomes.map((outcome) => {
    const score = outcome.criteria.reduce((sum, criterion) => {
      const levelId = adjustments.find((item) => item.criterionId === criterion.id)?.levelId ?? criterion.levelId
      return sum + (criterion.levels.find((level) => level.id === levelId)?.score ?? 0)
    }, 0)
    return { id: outcome.id, code: outcome.code, score: round(score), maxScore: outcome.maxScore }
  })
}

/**
 * Resultados del grupo calculados solo con la evaluación del docente, si ya la envió
 * (las de los evaluadores externos no se pueden leer desde el frontend).
 */
export async function sampleGroupResults(groupId: number): Promise<GroupResults> {
  const group = await groupService.get(groupId)
  const [moment, form, evaluations] = await Promise.all([
    momentService.get(group.momentId),
    evaluationService.form(groupId),
    evaluationService.list(groupId),
  ])
  const submitted = form.status === 'SUBMITTED'
  const total = (scores: OutcomeScore[]) => (submitted ? round(scores.reduce((sum, item) => sum + item.score, 0)) : null)
  const groupOutcomes = outcomeScores(form)
  const submittedEvaluations = evaluations.filter((evaluation) => evaluation.status === 'SUBMITTED').length
  return {
    groupId,
    final: moment.gradingEnd !== null && todayIso() > moment.gradingEnd,
    maxScore: form.maxScore,
    groupGrade: total(groupOutcomes),
    groupLearningOutcomes: groupOutcomes,
    members: form.members.map((member) => {
      const learningOutcomes = outcomeScores(form, member.id)
      return { studentId: member.id, fullName: member.fullName, individualGrade: total(learningOutcomes), learningOutcomes }
    }),
    submittedEvaluations,
    pendingEvaluations: Math.max(0, group.evaluators.length + 1 - submittedEvaluations),
  }
}

const SAMPLE_OUTCOMES = [
  { code: 'RAP1', description: 'Identifica y formula el problema', score: 0.57, maxScore: 0.6 },
  { code: 'RAP2', description: 'Diseña la solución', score: 1.05, maxScore: 1.2 },
  { code: 'RAP3', description: 'Aplica el paradigma orientado a objetos', score: 0.48, maxScore: 0.6 },
  { code: 'RAP4', description: 'Implementa y prueba la solución', score: 1.17, maxScore: 1.2 },
  { code: 'RAP5', description: 'Sustenta el proyecto', score: 1.36, maxScore: 1.4 },
]

/** Un resultado de ejemplo por cada grupo real del estudiante, disponible si ya cerró la calificación. */
export async function sampleStudentResults(): Promise<StudentResult[]> {
  const [groups, moments] = await Promise.all([groupService.myGroups(), momentService.list()])
  const today = todayIso()
  return groups.map((group) => {
    const moment = moments.find((item) => item.id === group.momentId)
    const available = moment?.gradingEnd != null && today > moment.gradingEnd
    return {
      momentId: group.momentId,
      momentName: group.momentName,
      periodCode: moment?.periodCode ?? '',
      groupId: group.id,
      groupName: group.name,
      projectTitle: group.projectTitle,
      available,
      finalGrade: available ? 4.63 : null,
      groupGrade: available ? 4.51 : null,
      maxScore: 5,
      learningOutcomes: available ? SAMPLE_OUTCOMES : [],
      comments: available
        ? [
            { evaluatorKind: 'Docente', comment: 'Buen trabajo en equipo; falta documentar las excepciones.' },
            { evaluatorKind: 'Empresario', comment: 'La solución resuelve un problema real. Mejoren la presentación.' },
          ]
        : [],
    }
  })
}
