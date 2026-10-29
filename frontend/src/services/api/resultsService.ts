import type { GroupResults, StudentResult } from '@/types/results'

import { httpClient } from './httpClient'

/** Notas finales de los grupos y de los estudiantes (`docs/07-contrato-api.md`, "Results"). */
export const resultsService = {
  /** Nota del grupo y de cada integrante, en total y por RAP (docente, admin y super_admin). */
  async group(groupId: number): Promise<GroupResults> {
    const { data } = await httpClient.get<GroupResults>(`/groups/${groupId}/results`)
    return data
  },

  /** Resultados del estudiante, uno por momento, el más reciente primero. */
  async mine(): Promise<StudentResult[]> {
    const { data } = await httpClient.get<StudentResult[]>('/students/me/results')
    return data
  },
}
