import type { AuditLogPage, AuditLogQuery } from '@/types/audit'

import { httpClient } from './httpClient'

/** Bitácora de auditoría (`/api/audit-log`, ADR-023): solo lectura para super_admin y admin. */
export const auditService = {
  /** Página de la bitácora, del hecho más reciente al más antiguo; el admin solo ve su facultad. */
  async list(query: AuditLogQuery = {}): Promise<AuditLogPage> {
    const params = Object.fromEntries(
      Object.entries(query).filter(([, value]) => value !== undefined && value !== ''),
    )
    const { data } = await httpClient.get<AuditLogPage>('/audit-log', { params })
    return data
  },
}
