import type { UserSummary } from '@/types/auth'
import type { Group, StudentWithoutGroup, UpdateGroupRequest } from '@/types/group'

import { httpClient } from './httpClient'

/** Grupos de un momento y sus evaluadores externos (`/api/groups`, `docs/07-contrato-api.md`). */
export const groupService = {
  /** Grupos del alcance del usuario; el docente ve solo los suyos. */
  async list(momentId?: number): Promise<Group[]> {
    const { data } = await httpClient.get<Group[]>('/groups', {
      params: momentId === undefined ? undefined : { momentId },
    })
    return data
  },

  async get(id: number): Promise<Group> {
    const { data } = await httpClient.get<Group>(`/groups/${id}`)
    return data
  },

  /** Habilita un grupo del docente en un momento que sigue en configuración. */
  async create(momentId: number, name: string): Promise<Group> {
    const { data } = await httpClient.post<Group>('/groups', { momentId, name })
    return data
  },

  async update(id: number, request: UpdateGroupRequest): Promise<Group> {
    const { data } = await httpClient.patch<Group>(`/groups/${id}`, request)
    return data
  },

  async remove(id: number): Promise<void> {
    await httpClient.delete(`/groups/${id}`)
  },

  async updateProjectTitle(id: number, projectTitle: string): Promise<Group> {
    const { data } = await httpClient.put<Group>(`/groups/${id}/project-title`, { projectTitle })
    return data
  },

  async studentsWithoutGroup(momentId: number): Promise<StudentWithoutGroup[]> {
    const { data } = await httpClient.get<StudentWithoutGroup[]>(`/moments/${momentId}/students-without-group`)
    return data
  },

  async addMember(id: number, studentId: number): Promise<Group> {
    const { data } = await httpClient.post<Group>(`/groups/${id}/members`, { studentId })
    return data
  },

  async removeMember(id: number, studentId: number): Promise<void> {
    await httpClient.delete(`/groups/${id}/members/${studentId}`)
  },

  /** Evaluador externo activo con ese correo o documento exactos; la API responde 404 si no hay. */
  async lookupEvaluator(query: string): Promise<UserSummary> {
    const { data } = await httpClient.get<UserSummary>('/external-evaluators/lookup', { params: { q: query } })
    return data
  },

  async assignEvaluator(id: number, evaluatorId: number): Promise<Group> {
    const { data } = await httpClient.post<Group>(`/groups/${id}/evaluators`, { evaluatorId })
    return data
  },

  async removeEvaluator(id: number, evaluatorId: number): Promise<void> {
    await httpClient.delete(`/groups/${id}/evaluators/${evaluatorId}`)
  },
}
