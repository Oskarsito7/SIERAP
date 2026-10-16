import type { GroupResults, StudentResult } from '@/types/results'

import { httpClient } from './httpClient'
import { sampleGroupResults, sampleStudentResults } from './resultsMock'

/**
 * `true` cuando D1 publique las rutas de resultados (docs/06, Etapa 6). Mientras sea
 * `false`, las pantallas muestran datos de ejemplo con la forma del contrato y lo avisan.
 */
export const RESULTS_API_READY = false

/** Notas finales de los grupos y de los estudiantes (`docs/07-contrato-api.md`, "Results"). */
export const resultsService = {
  /** Nota del grupo y de cada integrante, en total y por RAP (docente, admin y super_admin). */
  async group(groupId: number): Promise<GroupResults> {
    if (!RESULTS_API_READY) return sampleGroupResults(groupId)
    const { data } = await httpClient.get<GroupResults>(`/groups/${groupId}/results`)
    return data
  },

  /** Resultados del estudiante, uno por momento, el más reciente primero. */
  async mine(): Promise<StudentResult[]> {
    if (!RESULTS_API_READY) return sampleStudentResults()
    const { data } = await httpClient.get<StudentResult[]>('/students/me/results')
    return data
  },
}
