import { beforeEach, describe, expect, it, vi } from 'vitest'

import { groupFixture, momentFixture } from '@/pages/groups/testData'
import type { EvaluationForm } from '@/types/evaluation'

import { evaluationService } from './evaluationService'
import { groupService } from './groupService'
import { momentService } from './momentService'
import { sampleGroupResults } from './resultsMock'

vi.mock('./groupService', () => ({ groupService: { get: vi.fn() } }))
vi.mock('./momentService', () => ({ momentService: { get: vi.fn() } }))
vi.mock('./evaluationService', () => ({ evaluationService: { form: vi.fn(), list: vi.fn() } }))

const level = (id: number, score: number) => ({ id, name: `N${id}`, description: null, score })

const form: EvaluationForm = {
  id: 15, groupId: 7, status: 'SUBMITTED', evaluatedAt: '2026-11-24T10:00:00', comment: null, canEdit: true,
  submissionToGrade: null,
  learningOutcomes: [
    { id: 10, code: 'RAP1', description: 'Problema', maxScore: 1, criteria: [
      { id: 20, description: 'A', levelId: 30, levels: [level(30, 0.5), level(31, 1)] }] },
    { id: 11, code: 'RAP2', description: 'Sustentación', maxScore: 2, criteria: [
      { id: 21, description: 'B', levelId: 41, levels: [level(40, 1), level(41, 2)] }] },
  ],
  members: [
    { id: 50, fullName: 'Ana Gómez', adjustments: [{ criterionId: 20, levelId: 31 }], totalScore: 3 },
    { id: 51, fullName: 'Luis Martínez', adjustments: [], totalScore: 2.5 },
  ],
  totalScore: 2.5, maxScore: 3, complete: true,
}

describe('sampleGroupResults', () => {
  beforeEach(() => {
    vi.mocked(groupService.get).mockResolvedValue(groupFixture({ evaluators: [{ id: 70, fullName: 'Carlos Pérez', evaluatorType: 'Egresado', countsInGrade: true }] }))
    vi.mocked(momentService.get).mockResolvedValue({
      ...momentFixture({ gradingEnd: '2999-12-31' }), maxScore: 3, learningOutcomes: [],
    })
    vi.mocked(evaluationService.list).mockResolvedValue([
      { id: 15, evaluator: { id: 12, fullName: 'Marta Díaz', kind: 'Docente' }, status: 'SUBMITTED',
        evaluatedAt: '2026-11-24T10:00:00', totalScore: 2.5, comment: null },
    ])
  })

  it('builds the results from the teacher evaluation, with the adjustments per student', async () => {
    vi.mocked(evaluationService.form).mockResolvedValue(form)

    const results = await sampleGroupResults(7)

    expect(results.final).toBe(false)
    expect(results.groupGrade).toBe(2.5)
    expect(results.groupLearningOutcomes.map((outcome) => outcome.score)).toEqual([0.5, 2])
    expect(results.members[0]).toMatchObject({ individualGrade: 3 })
    expect(results.members[0].learningOutcomes.map((outcome) => outcome.score)).toEqual([1, 2])
    expect(results.submittedEvaluations).toBe(1)
    expect(results.pendingEvaluations).toBe(1)
  })

  it('has no grades while the teacher evaluation is a draft', async () => {
    vi.mocked(evaluationService.form).mockResolvedValue({ ...form, status: 'DRAFT' })

    const results = await sampleGroupResults(7)

    expect(results.groupGrade).toBeNull()
    expect(results.members.every((member) => member.individualGrade === null)).toBe(true)
  })
})
