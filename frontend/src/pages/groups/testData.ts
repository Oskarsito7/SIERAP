import type { Group } from '@/types/group'
import type { MomentSummary } from '@/types/moment'

/** Momento de prueba: Momento 1 en configuración, con todas sus ventanas definidas. */
export function momentFixture(overrides: Partial<MomentSummary> = {}): MomentSummary {
  return {
    id: 1,
    number: 1,
    name: 'Momento 1',
    subject: 'Programación II',
    programId: 1,
    programName: 'Ingeniería de Sistemas',
    periodId: 1,
    periodCode: '2026-2',
    semesterFrom: 1,
    semesterTo: 3,
    requirementLevel: 'Bajo',
    minMembers: 3,
    maxMembers: 5,
    groupCount: 0,
    status: 'CONFIGURATION',
    groupFormationStart: '2026-09-20',
    groupFormationEnd: '2026-09-30',
    submissionStart: '2026-10-01',
    submissionEnd: '2026-10-31',
    gradingStart: '2026-11-02',
    gradingEnd: '2026-11-20',
    groupFormationOpen: true,
    submissionOpen: false,
    gradingOpen: false,
    ...overrides,
  }
}

/** Grupo de prueba del docente 12 en el momento 1. */
export function groupFixture(overrides: Partial<Group> = {}): Group {
  return {
    id: 7,
    momentId: 1,
    momentName: 'Momento 1',
    name: 'Grupo 1',
    projectTitle: null,
    teacher: { id: 12, fullName: 'Marta Díaz' },
    memberCount: 0,
    minMembers: 3,
    maxMembers: 5,
    full: false,
    members: [],
    submissionDeadline: null,
    evaluationDate: null,
    evaluators: [],
    myGroup: false,
    ...overrides,
  }
}
