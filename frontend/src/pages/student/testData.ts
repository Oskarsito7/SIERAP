import type { UserSummary } from '@/types/auth'
import type { Submission } from '@/types/submission'

/** Estudiante de prueba con sesión abierta: Juan Rojas, id 40. */
export const studentUser: UserSummary = {
  id: 40,
  firstNames: 'Juan',
  lastNames: 'Rojas',
  fullName: 'Juan Rojas',
  email: 'juan@unicesar.edu.co',
  role: 'STUDENT',
  active: true,
  mustChangePassword: false,
}

/** Versión de prueba del grupo 7. */
export function submissionFixture(overrides: Partial<Submission> = {}): Submission {
  return {
    id: 90,
    groupId: 7,
    versionNumber: 1,
    submittedAt: '2026-10-14T22:41:05',
    late: false,
    zipFileName: 'sierap-v1.zip',
    zipSize: 5 * 1024 * 1024,
    presentationFileName: null,
    presentationSize: null,
    toGrade: true,
    ...overrides,
  }
}
