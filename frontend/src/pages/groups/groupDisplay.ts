import { formatDate } from '@/lib/dates'
import type { Group } from '@/types/group'

/** Estado del cupo de un grupo, como lo muestran los wireframes. */
export function groupStatus(group: Pick<Group, 'full' | 'memberCount' | 'minMembers'>): {
  label: string
  variant: 'default' | 'secondary' | 'destructive'
} {
  if (group.full) return { label: 'Completo', variant: 'default' }
  if (group.memberCount < group.minMembers) return { label: 'Bajo el mínimo', variant: 'destructive' }
  return { label: 'Con cupo', variant: 'secondary' }
}

/** Ventana de fechas del momento para mostrar, o "sin definir" si el admin no la fijó. */
export function formatWindow(start: string | null, end: string | null): string {
  return start && end ? `del ${formatDate(start)} al ${formatDate(end)}` : 'sin definir'
}

/** Fecha opcional de la API para mostrar. */
export function formatOptionalDate(date: string | null): string {
  return date ? formatDate(date) : 'Sin fijar'
}
