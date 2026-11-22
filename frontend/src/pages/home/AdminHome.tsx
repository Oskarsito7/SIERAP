import { Plus } from 'lucide-react'
import { Link } from 'react-router'

import { StatTile } from '@/components/charts/StatTile'
import { LoadStatus } from '@/components/LoadStatus'
import { Button } from '@/components/ui/button'
import { formatDate } from '@/lib/dates'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { momentService } from '@/services/api/momentService'

import { Empty, HomeCard, HomeLink, PhaseBadge } from './homeParts'
import { momentPhase, plural } from './momentPhase'

/**
 * Inicio del admin de facultad: los momentos de sus programas en el periodo actual,
 * agrupados por fase (configuración, conformación, entregas, calificación, cerrado),
 * y el límite de evaluación de la UPC del periodo.
 */
export function AdminHome() {
  const data = useLoad(async () => {
    const [moments, periods] = await Promise.all([momentService.list(), catalogService.periods()])
    return { moments, period: periods.find((period) => period.status === 'CURRENT') ?? null }
  }, [])

  if (data.state.kind !== 'success') {
    return <LoadStatus state={data.state} onRetry={data.reload} label="Cargando el resumen de la facultad…" />
  }
  const { moments, period } = data.state.data
  const count = (label: string) => moments.filter((moment) => momentPhase(moment).label === label).length
  const inProgress = moments.length - count('En configuración') - count('Cerrado')

  return (
    <div className="flex flex-col gap-6">
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <StatTile
          label="Periodo actual"
          value={period?.code ?? 'Sin periodo'}
          detail={period ? `Límite UPC: ${formatDate(period.upcEvaluationDeadline)}` : undefined}
        />
        <StatTile label="Momentos del periodo" value={String(moments.length)} />
        <StatTile label="En configuración" value={String(count('En configuración'))} detail="Aún no se lanzan" />
        <StatTile label="En curso" value={String(inProgress)} detail={plural(count('Cerrado'), 'cerrado')} />
      </div>
      <HomeCard title="Momentos de la facultad">
        <div className="flex justify-end">
          <Button size="sm" asChild>
            <Link to="/moments/new">
              <Plus aria-hidden />
              Nuevo momento
            </Link>
          </Button>
        </div>
        {moments.length === 0 && <Empty>Todavía no hay momentos en el periodo actual.</Empty>}
        {moments.map((moment) => (
          <HomeLink
            key={moment.id}
            to={`/moments/${moment.id}`}
            title={`${moment.name} · ${moment.programName}`}
            detail={`${moment.subject} · ${plural(moment.groupCount, 'grupo')}`}
            aside={<PhaseBadge moment={moment} />}
          />
        ))}
      </HomeCard>
    </div>
  )
}
