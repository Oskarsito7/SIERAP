import type { AuditAction } from '@/types/audit'

/** Cada tipo de hecho de la bitácora tal como lo ve el usuario. */
export const AUDIT_ACTION_LABELS: Record<AuditAction, string> = {
  ACCOUNT_CREATED: 'Cuenta creada',
  ACCOUNTS_IMPORTED: 'Estudiantes importados',
  ACCOUNT_UPDATED: 'Cuenta editada',
  ACCOUNT_ACTIVATED: 'Cuenta activada',
  ACCOUNT_DEACTIVATED: 'Cuenta desactivada',
  SEMESTER_BULK_UPDATED: 'Semestre en lote',
  REGISTRATION_CODE_DEACTIVATED: 'Código de registro desactivado',
  MOMENT_LAUNCHED: 'Momento lanzado',
  MOMENT_EXTENDED: 'Prórroga de momento',
  GLOBAL_PARAMETER_CREATED: 'Parámetro creado',
  GLOBAL_PARAMETER_CHANGED: 'Parámetro cambiado',
  EVALUATION_CORRECTED: 'Evaluación corregida',
  GROUP_DELETED: 'Grupo eliminado',
  GROUP_MEMBER_REMOVED: 'Integrante retirado',
  GROUP_EVALUATOR_ASSIGNED: 'Evaluador asignado',
  GROUP_EVALUATOR_REMOVED: 'Evaluador retirado',
}
