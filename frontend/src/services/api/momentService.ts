import type {
  CopyMomentsRequest,
  LaunchCheck,
  MomentData,
  MomentDetail,
  MomentSummary,
  MomentWeightsRequest,
  NewMomentRequest,
} from '@/types/moment'

import { httpClient } from './httpClient'

/** Momentos (`/api/moments`, `docs/07-contrato-api.md`): consulta y configuración. */
export const momentService = {
  /**
   * Momentos de un periodo dentro del alcance del usuario. Sin `periodId`, los del
   * periodo en curso (lista vacía si no hay uno).
   */
  async list(periodId?: number): Promise<MomentSummary[]> {
    const { data } = await httpClient.get<MomentSummary[]>('/moments', {
      params: periodId === undefined ? undefined : { periodId },
    })
    return data
  },

  /** Momento con sus pesos, RAP, criterios y niveles. */
  async get(id: number): Promise<MomentDetail> {
    const { data } = await httpClient.get<MomentDetail>(`/moments/${id}`)
    return data
  },

  /** Crea un momento en configuración (admin de la facultad del programa o super_admin). */
  async create(request: NewMomentRequest): Promise<MomentDetail> {
    const { data } = await httpClient.post<MomentDetail>('/moments', request)
    return data
  },

  /**
   * Cambia los datos del momento mientras sigue en configuración. Cada ventana se
   * envía con sus dos fechas, o con las dos en `null` para vaciarla.
   */
  async update(id: number, changes: Partial<MomentData>): Promise<MomentDetail> {
    const { data } = await httpClient.patch<MomentDetail>(`/moments/${id}`, changes)
    return data
  },

  /** Elimina un momento sin lanzar y sin grupos, con toda su configuración. */
  async remove(id: number): Promise<void> {
    await httpClient.delete(`/moments/${id}`)
  },

  /** Copia los momentos de un periodo a otro, sin las ventanas de fechas (ADR-010). */
  async copy(request: CopyMomentsRequest): Promise<MomentSummary[]> {
    const { data } = await httpClient.post<MomentSummary[]>('/moments/copy', request)
    return data
  },

  /**
   * Reemplaza los pesos del momento. No tienen que sumar 100 mientras se configura: eso
   * se revisa al lanzar.
   */
  async updateWeights(id: number, request: MomentWeightsRequest): Promise<void> {
    await httpClient.put(`/moments/${id}/weights`, request)
  },

  /** Revisa si el momento se puede lanzar, sin cambiar nada. */
  async launchCheck(id: number): Promise<LaunchCheck> {
    const { data } = await httpClient.get<LaunchCheck>(`/moments/${id}/launch-check`)
    return data
  },

  /**
   * Lanza el momento: bloquea su configuración y cierra la conformación de grupos.
   * Con grupos por debajo del mínimo, la API pide `confirmUnderMinimum`.
   */
  async launch(id: number, confirmUnderMinimum: boolean): Promise<MomentDetail> {
    const { data } = await httpClient.post<MomentDetail>(`/moments/${id}/launch`, { confirmUnderMinimum })
    return data
  },
}
