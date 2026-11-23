import type { NewRegistrationCodeRequest, RegistrationCode } from '@/types/registrationCode'

import type { Page } from '@/types/api'

import { httpClient } from './httpClient'
import { toPage } from './pagination'

/** Códigos de registro de estudiantes (`/api/registration-codes`, `docs/07-contrato-api.md`). */
export const registrationCodeService = {
  /** Los códigos del docente, de la facultad (admin) o todos (super_admin), del más nuevo al más viejo. */
  async list(): Promise<RegistrationCode[]> {
    const { data } = await httpClient.get<RegistrationCode[]>('/registration-codes')
    return data
  },

  /** Una página de los códigos (`page` desde 0); el total viene en `X-Total-Count`. */
  async listPage(params: { page: number; size: number }): Promise<Page<RegistrationCode>> {
    return toPage(await httpClient.get<RegistrationCode[]>('/registration-codes', { params }))
  },

  /** Genera un código; el docente omite `teacherId` (el código queda a su nombre). */
  async create(request: NewRegistrationCodeRequest): Promise<RegistrationCode> {
    const { data } = await httpClient.post<RegistrationCode>('/registration-codes', request)
    return data
  },

  /** Desactiva un código: deja de servir de inmediato y no se puede reactivar. */
  async deactivate(id: number): Promise<RegistrationCode> {
    const { data } = await httpClient.patch<RegistrationCode>(`/registration-codes/${id}`, { active: false })
    return data
  },
}
