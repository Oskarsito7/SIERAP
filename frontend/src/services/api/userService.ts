import type { UpdateUserRequest } from '@/types/account'
import type { UserSummary } from '@/types/auth'

import { httpClient } from './httpClient'

/** Edición de cuentas (`PATCH /api/users/{id}`, `docs/07-contrato-api.md`, "Stage 2 — Accounts"). */
export const userService = {
  /**
   * Cambia los datos de una cuenta que gestiona el usuario; solo se modifican los campos
   * enviados. Cambiar el correo cierra las sesiones de la cuenta.
   */
  async update(userId: number, changes: UpdateUserRequest): Promise<UserSummary> {
    const { data } = await httpClient.patch<UserSummary>(`/users/${userId}`, changes)
    return data
  },
}
