import type { NewAdminRequest } from '@/types/account'
import type { UserSummary } from '@/types/auth'

import { httpClient } from './httpClient'

/** Administradores de facultad, solo para el super_admin (`docs/07-contrato-api.md`, "Stage 2 — Accounts"). */
export const adminService = {
  /** Administradores de una facultad. */
  async list(facultyId: number): Promise<UserSummary[]> {
    const { data } = await httpClient.get<UserSummary[]>('/admins', { params: { facultyId } })
    return data
  },

  /** Crea un administrador de la facultad; le llega un correo para crear su contraseña. */
  async create(facultyId: number, request: NewAdminRequest): Promise<UserSummary> {
    const { data } = await httpClient.post<UserSummary>('/admins', { ...request, facultyId })
    return data
  },

  /** Activa o desactiva la cuenta; desactivarla también cierra sus sesiones. */
  async setActive(userId: number, active: boolean): Promise<UserSummary> {
    const { data } = await httpClient.patch<UserSummary>(`/users/${userId}`, { active })
    return data
  },
}
