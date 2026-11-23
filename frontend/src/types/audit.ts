/** Tipo de hecho de la bitácora (`AuditAction` del backend). */
export type AuditAction =
  | 'ACCOUNT_CREATED'
  | 'ACCOUNTS_IMPORTED'
  | 'ACCOUNT_UPDATED'
  | 'ACCOUNT_ACTIVATED'
  | 'ACCOUNT_DEACTIVATED'
  | 'SEMESTER_BULK_UPDATED'
  | 'REGISTRATION_CODE_DEACTIVATED'
  | 'MOMENT_LAUNCHED'
  | 'MOMENT_EXTENDED'
  | 'GLOBAL_PARAMETER_CREATED'
  | 'GLOBAL_PARAMETER_CHANGED'
  | 'EVALUATION_CORRECTED'
  | 'GROUP_DELETED'
  | 'GROUP_MEMBER_REMOVED'
  | 'GROUP_EVALUATOR_ASSIGNED'
  | 'GROUP_EVALUATOR_REMOVED'

/** Un registro de la bitácora de auditoría (`AuditLogResponse`). */
export interface AuditLogEntry {
  id: number
  /** Fecha y hora del hecho (`YYYY-MM-DDTHH:mm:ss`). */
  createdAt: string
  actorId: number | null
  actorName: string | null
  action: AuditAction
  entityType: string
  entityId: number | null
  facultyId: number | null
  /** Descripción del hecho, en español. */
  summary: string
  ip: string | null
}

/** Página de la bitácora (`AuditLogPageResponse`). */
export interface AuditLogPage {
  items: AuditLogEntry[]
  total: number
  /** Página devuelta, desde 0. */
  page: number
  size: number
}

/** Filtros de la consulta de la bitácora; los vacíos no se envían. */
export interface AuditLogQuery {
  page?: number
  size?: number
  action?: AuditAction
  /** Fecha inicial inclusive (`YYYY-MM-DD`). */
  from?: string
  /** Fecha final inclusive (`YYYY-MM-DD`). */
  to?: string
  q?: string
}
