import type { CriterionRequest, LearningOutcomeRequest, LevelRequest } from '@/types/moment'

import { httpClient } from './httpClient'

/**
 * RAP, criterios y niveles de un momento (`docs/07-contrato-api.md`, "RAP, criteria and
 * levels"). Solo funcionan mientras el momento está en configuración (si no, la API
 * responde 422). Después de cada cambio la pantalla vuelve a leer el momento, que es la
 * única fuente de su configuración; por eso estas llamadas no devuelven nada.
 */
export const rubricService = {
  async createOutcome(momentId: number, request: LearningOutcomeRequest): Promise<void> {
    await httpClient.post(`/moments/${momentId}/learning-outcomes`, request)
  },

  async updateOutcome(id: number, changes: Partial<LearningOutcomeRequest>): Promise<void> {
    await httpClient.patch(`/learning-outcomes/${id}`, changes)
  },

  /** Elimina el RAP con sus criterios y niveles. */
  async deleteOutcome(id: number): Promise<void> {
    await httpClient.delete(`/learning-outcomes/${id}`)
  },

  async createCriterion(outcomeId: number, request: CriterionRequest): Promise<void> {
    await httpClient.post(`/learning-outcomes/${outcomeId}/criteria`, request)
  },

  async updateCriterion(id: number, changes: Partial<CriterionRequest>): Promise<void> {
    await httpClient.patch(`/criteria/${id}`, changes)
  },

  /** Elimina el criterio con sus niveles. */
  async deleteCriterion(id: number): Promise<void> {
    await httpClient.delete(`/criteria/${id}`)
  },

  async createLevel(criterionId: number, request: LevelRequest): Promise<void> {
    await httpClient.post(`/criteria/${criterionId}/levels`, request)
  },

  async updateLevel(id: number, changes: Partial<LevelRequest>): Promise<void> {
    await httpClient.patch(`/levels/${id}`, changes)
  },

  async deleteLevel(id: number): Promise<void> {
    await httpClient.delete(`/levels/${id}`)
  },
}
