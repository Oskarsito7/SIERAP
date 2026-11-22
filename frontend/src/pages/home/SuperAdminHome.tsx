import { CircleCheck, CircleX } from 'lucide-react'

import { StatTile } from '@/components/charts/StatTile'
import { LoadStatus } from '@/components/LoadStatus'
import { Badge } from '@/components/ui/badge'
import { formatDate } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { momentService } from '@/services/api/momentService'
import { systemStatusService } from '@/services/api/systemStatusService'

import { Empty, HomeCard, HomeLink, PhaseBadge } from './homeParts'
import { plural } from './momentPhase'

/**
 * Inicio del super_admin: resumen institucional (facultades, programas, periodo
 * actual con su límite UPC y momentos de todas las facultades) y el estado técnico
 * del sistema, que solo le interesa a él.
 */
export function SuperAdminHome() {
  const data = useLoad(async () => {
    const [faculties, periods, moments] = await Promise.all([
      catalogService.faculties(),
      catalogService.periods(),
      momentService.list(),
    ])
    return { faculties, period: periods.find((period) => period.status === 'CURRENT') ?? null, moments }
  }, [])
  const status = useLoad(() => systemStatusService.getStatus(), [])

  if (data.state.kind !== 'success') {
    return <LoadStatus state={data.state} onRetry={data.reload} label="Cargando el resumen institucional…" />
  }
  const { faculties, period, moments } = data.state.data
  const programs = faculties.reduce((sum, faculty) => sum + faculty.programCount, 0)

  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatTile label="Facultades" value={String(faculties.length)} />
        <StatTile label="Programas" value={String(programs)} />
        <StatTile
          label="Periodo actual"
          value={period?.code ?? 'Sin periodo'}
          detail={period ? `Límite UPC: ${formatDate(period.upcEvaluationDeadline)}` : 'Créalo en Periodos académicos'}
        />
        <StatTile label="Momentos del periodo" value={String(moments.length)} />
      </div>

      <div className="grid items-start gap-6 lg:grid-cols-[minmax(0,1fr)_18rem]">
        <HomeCard title="Momentos de todas las facultades">
          {moments.length === 0 && <Empty>No hay momentos en el periodo actual.</Empty>}
          {moments.map((moment) => (
            <HomeLink
              key={moment.id}
              to="/reports"
              title={`${moment.name} · ${moment.programName}`}
              detail={plural(moment.groupCount, 'grupo')}
              aside={<PhaseBadge moment={moment} />}
            />
          ))}
        </HomeCard>

        <HomeCard title="Estado del sistema">
          <LoadStatus state={status.state} onRetry={status.reload} label="Consultando el servidor…" />
          {status.state.kind === 'success' && (
            <>
              <div className="flex flex-wrap gap-2">
                <Badge>
                  <CircleCheck aria-hidden />
                  API conectada
                </Badge>
                {status.state.data.databaseAvailable ? (
                  <Badge variant="secondary">
                    <CircleCheck aria-hidden />
                    Base de datos disponible
                  </Badge>
                ) : (
                  <Badge variant="destructive">
                    <CircleX aria-hidden />
                    Base de datos no disponible
                  </Badge>
                )}
              </div>
              <p className="text-muted-foreground text-xs">Versión {status.state.data.version}</p>
            </>
          )}
        </HomeCard>
      </div>
    </div>
  )
}
