import type { ReactNode } from 'react'
import { ChevronRight } from 'lucide-react'
import { Link } from 'react-router'

import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ROLE_MENUS } from '@/components/layout/roleMenus'
import type { UserRole } from '@/types/auth'
import type { MomentSummary } from '@/types/moment'

import { momentPhase } from './momentPhase'

/** Etiqueta con la fase del momento y hasta cuándo dura. */
export function PhaseBadge({ moment }: { moment: MomentSummary }) {
  const phase = momentPhase(moment)
  return (
    <Badge variant={phase.variant}>
      {phase.label}
      {phase.until && ` · hasta el ${phase.until}`}
    </Badge>
  )
}

/** Tarjeta de una sección del inicio. */
export function HomeCard({
  title,
  description,
  children,
}: {
  title: string
  description?: string
  children: ReactNode
}) {
  return (
    <Card className="min-w-0">
      <CardHeader>
        <CardTitle>{title}</CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      <CardContent className="flex flex-col gap-3">{children}</CardContent>
    </Card>
  )
}

/** Fila de una lista del inicio que lleva a otra página. */
export function HomeLink({ to, title, detail, aside }: { to: string; title: string; detail?: string; aside?: ReactNode }) {
  return (
    <Link
      to={to}
      className="hover:bg-muted/60 focus-visible:ring-ring/50 flex flex-wrap items-center gap-x-3 gap-y-1 rounded-md border px-3 py-2 outline-none focus-visible:ring-[3px]"
    >
      {/* En el celular la etiqueta (`aside`) baja a otra línea en vez de cortar el título. */}
      <span className="flex min-w-48 flex-1 flex-col">
        <span className="truncate text-sm font-medium">{title}</span>
        {detail && <span className="text-muted-foreground truncate text-xs">{detail}</span>}
      </span>
      {aside}
      <ChevronRight className="text-muted-foreground size-4 shrink-0" aria-hidden />
    </Link>
  )
}

/** Texto para una lista vacía del inicio. */
export function Empty({ children }: { children: ReactNode }) {
  return <p className="text-muted-foreground text-sm">{children}</p>
}

/** Accesos directos a las páginas del menú del rol, con lo que se hace en cada una. */
export function QuickLinks({ role }: { role: UserRole }) {
  return (
    <section aria-labelledby="quick-links" className="flex flex-col gap-3">
      <h2 id="quick-links" className="text-lg font-semibold">
        Accesos rápidos
      </h2>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {ROLE_MENUS[role].map(({ path, label, icon: Icon, description }) => (
          <Link
            key={path}
            to={path}
            className="bg-card hover:border-primary focus-visible:ring-ring/50 flex items-start gap-3 rounded-lg border p-4 outline-none focus-visible:ring-[3px]"
          >
            <Icon className="text-primary mt-0.5 size-5 shrink-0" aria-hidden />
            <span className="flex flex-col gap-0.5">
              <span className="text-sm font-semibold">{label}</span>
              <span className="text-muted-foreground text-xs">{description}</span>
            </span>
          </Link>
        ))}
      </div>
    </section>
  )
}
