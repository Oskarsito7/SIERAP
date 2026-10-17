import type { NewTeacherRequest, TeacherSummary } from '@/types/account'
import type { UserSummary } from '@/types/auth'

import { accountService } from './accountService'
import { httpClient } from './httpClient'
import { teachersMock } from './mocks/teachersMock'

/**
 * `true` cuando el backend acepte las rutas de docentes sin `facultyId` para el admin
 * (usa su facultad) y devuelva `TeacherSummary`. Mientras sea `false`, "Docentes" usa
 * datos de ejemplo y lo avisa.
 */
export const TEACHERS_API_READY = false

/** Docentes de la facultad del admin (`docs/07-contrato-api.md`, "Stage 2 — Accounts"). */
export const teacherService = {
  /** Docentes de la facultad, por apellido. */
  async list(): Promise<TeacherSummary[]> {
    if (!TEACHERS_API_READY) return teachersMock.list()
    const { data } = await httpClient.get<TeacherSummary[]>('/teachers')
    return data
  },

  /** Crea un docente en la facultad del admin; le llega un correo para crear su contraseña. */
  async create(request: NewTeacherRequest): Promise<TeacherSummary> {
    if (!TEACHERS_API_READY) return teachersMock.create(request)
    const { data } = await httpClient.post<TeacherSummary>('/teachers', request)
    return data
  },

  /** Reenvía el enlace para crear o recuperar la contraseña. */
  async resendPasswordLink(userId: number): Promise<void> {
    if (!TEACHERS_API_READY) return teachersMock.resendPasswordLink(userId)
    return accountService.resendPasswordLink(userId)
  },

  /** Activa o desactiva la cuenta; desactivarla también cierra sus sesiones. */
  async setActive(userId: number, active: boolean): Promise<UserSummary> {
    if (!TEACHERS_API_READY) return teachersMock.setActive(userId, active)
    const { data } = await httpClient.patch<UserSummary>(`/users/${userId}`, { active })
    return data
  },
}
