import type { NewTeacherRequest } from '@/types/account'
import type { UserSummary } from '@/types/auth'

import { httpClient } from './httpClient'

/** Docentes de una facultad (`docs/07-contrato-api.md`, "Stage 2 — Accounts"). */
export const teacherService = {
  /** Docentes de la facultad (el admin solo puede pedir la suya). */
  async list(facultyId: number): Promise<UserSummary[]> {
    const { data } = await httpClient.get<UserSummary[]>('/teachers', { params: { facultyId } })
    return data
  },

  /** Crea un docente en la facultad; le llega un correo para crear su contraseña. */
  async create(facultyId: number, request: NewTeacherRequest): Promise<UserSummary> {
    const { data } = await httpClient.post<UserSummary>('/teachers', { ...request, facultyId })
    return data
  },

  /** Activa o desactiva la cuenta; desactivarla también cierra sus sesiones. */
  async setActive(userId: number, active: boolean): Promise<UserSummary> {
    const { data } = await httpClient.patch<UserSummary>(`/users/${userId}`, { active })
    return data
  },
}
