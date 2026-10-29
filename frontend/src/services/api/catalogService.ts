import type { EvaluatorType } from '@/types/account'
import type {
  AcademicPeriod,
  CatalogEntryRequest,
  EvaluatorTypeRequest,
  Faculty,
  GlobalParameter,
  PeriodRequest,
  Program,
} from '@/types/catalog'

import { httpClient } from './httpClient'

/**
 * Catálogos académicos (`docs/07-contrato-api.md`, "Stage 3 — Catalogs"). Leen el admin
 * y el docente (solo lo de su facultad); crear, editar y eliminar es del super_admin.
 * Eliminar responde 409 con el motivo cuando algo cuelga del recurso.
 */
export const catalogService = {
  /** Facultades por nombre: todas para el super_admin, solo la propia para el admin y el docente. */
  async faculties(): Promise<Faculty[]> {
    const { data } = await httpClient.get<Faculty[]>('/faculties')
    return data
  },

  async createFaculty(request: CatalogEntryRequest): Promise<Faculty> {
    const { data } = await httpClient.post<Faculty>('/faculties', request)
    return data
  },

  async updateFaculty(id: number, changes: Partial<CatalogEntryRequest>): Promise<Faculty> {
    const { data } = await httpClient.patch<Faculty>(`/faculties/${id}`, changes)
    return data
  },

  async deleteFaculty(id: number): Promise<void> {
    await httpClient.delete(`/faculties/${id}`)
  },

  /** Programas por nombre; el admin y el docente solo ven los de su facultad. */
  async programs(facultyId?: number): Promise<Program[]> {
    const { data } = await httpClient.get<Program[]>('/programs', {
      params: facultyId === undefined ? undefined : { facultyId },
    })
    return data
  },

  async createProgram(facultyId: number, request: CatalogEntryRequest): Promise<Program> {
    const { data } = await httpClient.post<Program>('/programs', { facultyId, ...request })
    return data
  },

  /** La facultad de un programa no cambia. */
  async updateProgram(id: number, changes: Partial<CatalogEntryRequest>): Promise<Program> {
    const { data } = await httpClient.patch<Program>(`/programs/${id}`, changes)
    return data
  },

  async deleteProgram(id: number): Promise<void> {
    await httpClient.delete(`/programs/${id}`)
  },

  /** Periodos académicos, el más reciente primero. */
  async periods(): Promise<AcademicPeriod[]> {
    const { data } = await httpClient.get<AcademicPeriod[]>('/periods')
    return data
  },

  async createPeriod(request: PeriodRequest): Promise<AcademicPeriod> {
    const { data } = await httpClient.post<AcademicPeriod>('/periods', request)
    return data
  },

  /** El backend pide los cuatro campos aunque no cambien. */
  async updatePeriod(id: number, request: PeriodRequest): Promise<AcademicPeriod> {
    const { data } = await httpClient.patch<AcademicPeriod>(`/periods/${id}`, request)
    return data
  },

  async deletePeriod(id: number): Promise<void> {
    await httpClient.delete(`/periods/${id}`)
  },

  async createEvaluatorType(request: EvaluatorTypeRequest): Promise<EvaluatorType> {
    const { data } = await httpClient.post<EvaluatorType>('/evaluator-types', request)
    return data
  },

  /**
   * Cambia un tipo de evaluador. El backend pide el nombre, la descripción y el estado
   * en cada cambio (sin `active`, lo desactivaría), así que siempre se envían los tres.
   */
  async updateEvaluatorType(id: number, request: EvaluatorTypeRequest & { active: boolean }): Promise<EvaluatorType> {
    const { data } = await httpClient.patch<EvaluatorType>(`/evaluator-types/${id}`, request)
    return data
  },

  async deleteEvaluatorType(id: number): Promise<void> {
    await httpClient.delete(`/evaluator-types/${id}`)
  },

  /** Parámetros globales por clave (solo super_admin). */
  async parameters(): Promise<GlobalParameter[]> {
    const { data } = await httpClient.get<GlobalParameter[]>('/global-parameters')
    return data
  },

  /** Crea el parámetro si la clave no existe; si existe, cambia su valor y su descripción. */
  async saveParameter(key: string, value: string, description: string | null): Promise<GlobalParameter> {
    const { data } = await httpClient.put<GlobalParameter>(`/global-parameters/${encodeURIComponent(key)}`, {
      value,
      description,
    })
    return data
  },
}
