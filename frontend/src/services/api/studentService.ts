import type { NewStudentRequest } from '@/types/account'
import type { UserSummary } from '@/types/auth'

import { httpClient } from './httpClient'

/** Estudiantes de los programas de la facultad (`docs/07-contrato-api.md`, "Stage 2 — Accounts"). */
export const studentService = {
  /** Estudiantes de un programa (solo de la facultad del usuario). */
  async list(programId: number): Promise<UserSummary[]> {
    const { data } = await httpClient.get<UserSummary[]>('/students', { params: { programId } })
    return data
  },

  /** Crea un estudiante en un programa; le llega un correo para crear su contraseña. */
  async create(request: NewStudentRequest): Promise<UserSummary> {
    const { data } = await httpClient.post<UserSummary>('/students', request)
    return data
  },

  /** Activa o desactiva la cuenta; desactivarla también cierra sus sesiones. */
  async setActive(userId: number, active: boolean): Promise<UserSummary> {
    const { data } = await httpClient.patch<UserSummary>(`/users/${userId}`, { active })
    return data
  },
}
