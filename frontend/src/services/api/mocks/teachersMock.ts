import type { NewTeacherRequest, TeacherSummary } from '@/types/account'
import type { UserSummary } from '@/types/auth'

import { mockApiError } from './mockApiError'

/*
 * Docentes de ejemplo para "Docentes" (AD-06) mientras se ajusta la API: hoy
 * `GET /api/teachers` pide el `facultyId`, que el frontend del admin no conoce, y no
 * devuelve el documento ni los grupos. Los cambios viven solo en memoria y se pierden al
 * recargar la página.
 */

const teachers: TeacherSummary[] = [
  sample(1, 'Marta', 'Díaz', 'marta.diaz@unicesar.edu.co', '49777123', 3),
  sample(2, 'Rafael', 'Ospino', 'rafael.ospino@unicesar.edu.co', '12345678', 2),
  sample(3, 'Diana', 'Quintero', 'diana.quintero@unicesar.edu.co', '36555010', 0),
]
let nextId = 100

function sample(
  id: number, firstNames: string, lastNames: string, email: string, documentNumber: string, groupCount: number,
): TeacherSummary {
  return {
    id, firstNames, lastNames, fullName: `${firstNames} ${lastNames}`, email, role: 'TEACHER', active: true,
    mustChangePassword: false, documentType: 'CC', documentNumber, groupCount,
  }
}

function find(userId: number): TeacherSummary {
  const teacher = teachers.find((item) => item.id === userId)
  if (!teacher) throw mockApiError(404, 'No se encontró el usuario.')
  return teacher
}

export const teachersMock = {
  async list(): Promise<TeacherSummary[]> {
    return [...teachers].sort((a, b) => a.lastNames.localeCompare(b.lastNames, 'es'))
  },

  async create(request: NewTeacherRequest): Promise<TeacherSummary> {
    const email = request.email.trim().toLowerCase()
    if (teachers.some((teacher) => teacher.email === email)) {
      throw mockApiError(422, 'Ya existe una cuenta con ese correo.')
    }
    const teacher = sample(nextId++, request.firstNames, request.lastNames, email, request.documentNumber, 0)
    teacher.documentType = request.documentType
    teachers.push(teacher)
    return teacher
  },

  async resendPasswordLink(userId: number): Promise<void> {
    find(userId)
  },

  async setActive(userId: number, active: boolean): Promise<UserSummary> {
    const teacher = find(userId)
    teacher.active = active
    return teacher
  },
}
