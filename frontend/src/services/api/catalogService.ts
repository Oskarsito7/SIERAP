import type { AcademicPeriod, Faculty, Program } from '@/types/catalog'

import { httpClient } from './httpClient'

/** Lectura de los catálogos académicos (`docs/07-contrato-api.md`, "Stage 3 — Catalogs"). */
export const catalogService = {
  /** Facultades por nombre: todas para el super_admin, solo la propia para el admin y el docente. */
  async faculties(): Promise<Faculty[]> {
    const { data } = await httpClient.get<Faculty[]>('/faculties')
    return data
  },

  /** Programas por nombre; el admin y el docente solo ven los de su facultad. */
  async programs(facultyId?: number): Promise<Program[]> {
    const { data } = await httpClient.get<Program[]>('/programs', {
      params: facultyId === undefined ? undefined : { facultyId },
    })
    return data
  },

  /** Periodos académicos, el más reciente primero. */
  async periods(): Promise<AcademicPeriod[]> {
    const { data } = await httpClient.get<AcademicPeriod[]>('/periods')
    return data
  },
}
