import type { EvaluationForm, EvaluationSummary, SaveEvaluationRequest } from '@/types/evaluation'

import { httpClient } from './httpClient'

/** Calificación de los grupos (`docs/07-contrato-api.md`, "Evaluation"). */
export const evaluationService = {
  /** Evaluación propia del grupo, o una vacía si todavía no se empezó. */
  async form(groupId: number): Promise<EvaluationForm> {
    const { data } = await httpClient.get<EvaluationForm>(`/groups/${groupId}/evaluation`)
    return data
  },

  /** Guarda la evaluación propia como borrador (o la corrige si ya fue enviada). */
  async save(groupId: number, request: SaveEvaluationRequest): Promise<EvaluationForm> {
    const { data } = await httpClient.put<EvaluationForm>(`/groups/${groupId}/evaluation`, request)
    return data
  },

  /** Envía la evaluación propia; la API responde 422 si falta calificar algún criterio. */
  async submit(groupId: number): Promise<EvaluationForm> {
    const { data } = await httpClient.post<EvaluationForm>(`/groups/${groupId}/evaluation/submit`)
    return data
  },

  /** Evaluaciones del grupo: todas para su docente, solo la propia para un evaluador externo. */
  async list(groupId: number): Promise<EvaluationSummary[]> {
    const { data } = await httpClient.get<EvaluationSummary[]>(`/groups/${groupId}/evaluations`)
    return data
  },
}
