import type {
  NewStudentRequest,
  SetSemesterRequest,
  Student,
  StudentImportPreview,
  StudentImportResult,
} from '@/types/account'
import type { Page } from '@/types/api'
import type { UserSummary } from '@/types/auth'

import { httpClient } from './httpClient'
import { toPage } from './pagination'

/** Estudiantes de los programas de la facultad (`docs/07-contrato-api.md`, "Stage 2 — Accounts"). */
export const studentService = {
  /**
   * Estudiantes de un programa (solo de la facultad del usuario), por apellidos. Con
   * `withoutGroupInMoment`, solo los activos que pueden estar en ese momento y aún no
   * tienen grupo en él.
   */
  async list(programId: number, withoutGroupInMoment?: number): Promise<Student[]> {
    const { data } = await httpClient.get<Student[]>('/students', { params: { programId, withoutGroupInMoment } })
    return data
  },

  /**
   * Una página de los estudiantes de un programa, por apellidos, con búsqueda opcional `q`
   * (nombres, apellidos, código y correo; sin distinguir mayúsculas ni tildes). `page`
   * empieza en 0; el total sin paginar viene en `X-Total-Count`.
   */
  async listPage(programId: number, params: { q?: string; page: number; size: number }): Promise<Page<Student>> {
    const response = await httpClient.get<Student[]>('/students', { params: { programId, ...params } })
    return toPage(response)
  },

  /** Crea un estudiante en un programa; le llega un correo para crear su contraseña. */
  async create(request: NewStudentRequest): Promise<Student> {
    const { data } = await httpClient.post<Student>('/students', request)
    return data
  },

  /** Activa o desactiva la cuenta; desactivarla también cierra sus sesiones. */
  async setActive(userId: number, active: boolean): Promise<UserSummary> {
    const { data } = await httpClient.patch<UserSummary>(`/users/${userId}`, { active })
    return data
  },

  /** Fija el mismo semestre a varios estudiantes (todo o nada); devuelve cuántos se actualizaron. */
  async setSemester(request: SetSemesterRequest): Promise<number> {
    const { data } = await httpClient.patch<{ updated: number }>('/students/semester', request)
    return data.updated
  },

  /** Valida el CSV de estudiantes fila por fila sin crear nada. */
  async previewImport(programId: number, file: File): Promise<StudentImportPreview> {
    const { data } = await httpClient.post<StudentImportPreview>('/students/bulk/preview', csvForm(programId, file), {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  },

  /** Crea los estudiantes de las filas válidas del CSV; cada uno recibe un correo para crear su contraseña. */
  async importStudents(programId: number, file: File): Promise<StudentImportResult> {
    const { data } = await httpClient.post<StudentImportResult>('/students/bulk', csvForm(programId, file), {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  },
}

function csvForm(programId: number, file: File): FormData {
  const form = new FormData()
  form.append('programId', String(programId))
  form.append('file', file)
  return form
}
