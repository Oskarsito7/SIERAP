import type { EvaluatorType, NewExternalEvaluatorRequest } from '@/types/account'
import type { UserSummary } from '@/types/auth'

import { httpClient } from './httpClient'

/** Cuentas que gestiona el usuario (`docs/07-contrato-api.md`, "Stage 2 — Accounts"). */
export const accountService = {
  /** Evaluadores externos que creó el usuario (todos para el super_admin). */
  async listExternalEvaluators(): Promise<UserSummary[]> {
    const { data } = await httpClient.get<UserSummary[]>('/external-evaluators')
    return data
  },

  /** Crea un evaluador externo; le llega un correo para crear su contraseña. */
  async createExternalEvaluator(request: NewExternalEvaluatorRequest): Promise<UserSummary> {
    const { data } = await httpClient.post<UserSummary>('/external-evaluators', request)
    return data
  },

  /** Reenvía el enlace para crear o recuperar la contraseña de una cuenta que gestiona el usuario. */
  async resendPasswordLink(userId: number): Promise<void> {
    await httpClient.post(`/users/${userId}/password-link`)
  },

  /** Catálogo de tipos de evaluador externo. */
  async listEvaluatorTypes(): Promise<EvaluatorType[]> {
    const { data } = await httpClient.get<EvaluatorType[]>('/evaluator-types')
    return data
  },
}
