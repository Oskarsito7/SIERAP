import type { NewStudentRequest, Student } from '@/types/account'
import type { UserSummary } from '@/types/auth'

import { httpClient } from './httpClient'

/** Estudiantes de los programas de la facultad (`docs/07-contrato-api.md`, "Stage 2 — Accounts"). */
export const studentService = {
  /**
   * Estudiantes de un programa (solo de la facultad del usuario), por apellidos. Con
   * `withoutGroupInMoment`, solo los activos que pueden estar en ese momento y aún no
   * tienen grupo en él.
   */
  async list(programId: number, withoutGroupInMoment?: number): Promise<Student[]> {
    const { data } = await httpClient.get<Student[]>('/students', { params: { programId, withoutGroupInMoment } })
    return data
  },

  /** Crea un estudiante en un programa; le llega un correo para crear su contraseña. */
  async create(request: NewStudentRequest): Promise<Student> {
    const { data } = await httpClient.post<Student>('/students', request)
    return data
  },

  /** Activa o desactiva la cuenta; desactivarla también cierra sus sesiones. */
  async setActive(userId: number, active: boolean): Promise<UserSummary> {
    const { data } = await httpClient.patch<UserSummary>(`/users/${userId}`, { active })
    return data
  },
}
