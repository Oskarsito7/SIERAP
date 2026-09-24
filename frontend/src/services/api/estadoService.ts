import type { EstadoSistema } from '@/types/estado'

import { httpClient } from './httpClient'

/** Consultas del estado general del backend (`GET /api/estado`). */
export const estadoService = {
  async consultar(): Promise<EstadoSistema> {
    const { data } = await httpClient.get<EstadoSistema>('/estado')
    return data
  },
}
