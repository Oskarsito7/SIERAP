import { Badge } from '@/components/ui/badge'
import type { MomentStatus } from '@/types/moment'

/** Estado de un momento (ADR-012): "Configuración" o "Lanzado". */
export function MomentStatusBadge({ status }: { status: MomentStatus }) {
  return status === 'LAUNCHED' ? <Badge>Lanzado</Badge> : <Badge variant="outline">Configuración</Badge>
}
