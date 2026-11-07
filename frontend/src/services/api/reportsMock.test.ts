import { beforeEach, describe, expect, it, vi } from 'vitest'

import { groupFixture, momentFixture } from '@/pages/groups/testData'
import { momentDetailFixture, periodFixture } from '@/pages/moments/testData'

import { catalogService } from './catalogService'
import { groupService } from './groupService'
import { momentService } from './momentService'
import { sampleComparativeReport, sampleMomentReport } from './reportsMock'

vi.mock('./catalogService', () => ({ catalogService: { periods: vi.fn() } }))
vi.mock('./groupService', () => ({ groupService: { list: vi.fn(), get: vi.fn() } }))
vi.mock('./momentService', () => ({ momentService: { get: vi.fn(), list: vi.fn() } }))

const outcome = (id: number) => ({ id, code: `RAP${id}`, description: `RAP ${id}`, maxScore: 1, criteria: [] })

describe('reportsMock', () => {
  beforeEach(() => {
    vi.mocked(momentService.get).mockResolvedValue(
      momentDetailFixture({ maxScore: 5, learningOutcomes: [outcome(1), outcome(2)] }),
    )
  })

  it('keeps every graded student in the distribution and leaves one group without grade', async () => {
    vi.mocked(groupService.list).mockResolvedValue([
      groupFixture({ id: 7, memberCount: 4 }),
      groupFixture({ id: 8, memberCount: 3 }),
      groupFixture({ id: 9, memberCount: 2 }),
    ])

    const report = await sampleMomentReport(1)

    expect(report.totalGroups).toBe(3)
    expect(report.gradedGroups).toBe(2)
    expect(report.gradedStudents).toBe(7)
    expect(report.distribution.reduce((sum, bucket) => sum + bucket.count, 0)).toBe(7)
    expect(report.learningOutcomes.map((item) => item.percentage)).toEqual([78, 71])
  })

  it('sets the limits of the buckets relative to the maximum grade of the moment', async () => {
    vi.mocked(momentService.get).mockResolvedValue(
      momentDetailFixture({ maxScore: 10, learningOutcomes: [outcome(1)] }),
    )
    vi.mocked(groupService.list).mockResolvedValue([groupFixture({ memberCount: 4 })])

    const report = await sampleMomentReport(1)

    expect(report.distribution.map((bucket) => [bucket.min, bucket.max])).toEqual([
      [0, 5.99],
      [6, 7.59],
      [7.6, 8.99],
      [9, 10],
    ])
  })

  it('rejects comparing a period with itself, as the API does', async () => {
    await expect(
      sampleComparativeReport({ programId: 1, momentNumber: 1, periodAId: 1, periodBId: 1 }),
    ).rejects.toMatchObject({ response: { status: 422 } })
  })

  it('pairs the RAP of both periods and computes the change', async () => {
    vi.mocked(catalogService.periods).mockResolvedValue([
      periodFixture({ id: 1, code: '2026-2' }),
      periodFixture({ id: 2, code: '2026-1' }),
    ])
    vi.mocked(momentService.list).mockImplementation(async (periodId) => [
      momentFixture({ id: periodId === 1 ? 1 : 4, periodId: periodId ?? 1 }),
    ])

    const report = await sampleComparativeReport({ programId: 1, momentNumber: 1, periodAId: 2, periodBId: 1 })

    expect(report.periodA.period.code).toBe('2026-1')
    expect(report.learningOutcomes[0]).toMatchObject({
      code: 'RAP1',
      percentageA: 72,
      percentageB: 78,
      deltaPercentage: 6,
    })
  })
})
