import type { AcademicPeriod, Program } from '@/types/catalog'
import type { MomentDetail } from '@/types/moment'

import { momentFixture } from '../groups/testData'

/** Periodo de prueba: 2026-2, en curso, con límite de la UPC el 28/11/2026. */
export function periodFixture(overrides: Partial<AcademicPeriod> = {}): AcademicPeriod {
  return {
    id: 1, code: '2026-2', startDate: '2026-08-03', endDate: '2026-12-05', upcEvaluationDeadline: '2026-11-28',
    status: 'CURRENT', ...overrides,
  }
}

/** Programa de prueba de la facultad 1. */
export function programFixture(overrides: Partial<Program> = {}): Program {
  return {
    id: 1, code: '108', name: 'Ingeniería de Sistemas', facultyId: 1, facultyName: 'Facultad de Ingenierías',
    currentPeriodMomentCount: 1, studentCount: 120, ...overrides,
  }
}

/** Detalle de prueba del Momento 1, sin RAP. */
export function momentDetailFixture(overrides: Partial<MomentDetail> = {}): MomentDetail {
  return {
    ...momentFixture(), teacherWeight: 100, evaluatorWeights: [], totalWeight: 100, maxScore: 0, learningOutcomes: [],
    ...overrides,
  }
}
