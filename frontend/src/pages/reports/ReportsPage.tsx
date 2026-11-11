import { useState } from 'react'
import { useSearchParams } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { LoadStatus } from '@/components/LoadStatus'
import { SampleDataNotice } from '@/components/SampleDataNotice'
import { SelectField } from '@/components/forms/SelectField'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'
import { REPORTS_API_READY } from '@/services/api/reportService'
import type { AcademicPeriod } from '@/types/catalog'

import { ComparativeReport } from './ComparativeReport'
import { GroupReportView } from './GroupReportView'
import { IndividualReportView } from './IndividualReportView'
import { MomentReportView } from './MomentReportView'

type ReportTab = 'moment' | 'group' | 'individual' | 'comparative'

const TABS: { tab: ReportTab; label: string }[] = [
  { tab: 'moment', label: 'Por momento' },
  { tab: 'group', label: 'Grupal' },
  { tab: 'individual', label: 'Individual' },
  { tab: 'comparative', label: 'Comparativo' },
]

/**
 * Reportes del admin y del docente (wireframe AD-08, RF 12): consolidado de un momento,
 * grupal, individual y comparativo entre dos periodos. Se ven en pantalla con gráficas y
 * tablas; exportarlos a PDF o Excel queda para el Momento 2. El alcance lo pone la API:
 * el admin ve su facultad y el docente, en el grupal y el individual, solo sus grupos.
 * La pestaña elegida queda en la dirección (`?tab=`).
 */
export function ReportsPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = TABS.find((item) => item.tab === searchParams.get('tab'))?.tab ?? 'moment'
  const periods = useLoad(() => catalogService.periods(), [])

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">Reportes</h1>
        <p className="text-muted-foreground max-w-2xl text-sm">
          {user?.role === 'TEACHER'
            ? 'El desempeño por RAP de tus grupos y de los momentos de tu facultad.'
            : 'El desempeño por RAP de los momentos, grupos y estudiantes de tu facultad.'}{' '}
          Mientras la calificación esté abierta, las cifras son una vista previa.
        </p>
      </div>
      {!REPORTS_API_READY && <SampleDataNotice reason="las rutas de reportes todavía no están publicadas." />}

      <nav aria-label="Tipos de reporte" className="flex flex-wrap gap-1 border-b">
        {TABS.map((item) => (
          <button
            key={item.tab}
            type="button"
            aria-current={item.tab === tab ? 'page' : undefined}
            onClick={() => setSearchParams({ tab: item.tab })}
            className={`-mb-px border-b-2 px-3 py-2 text-sm font-medium ${
              item.tab === tab ? 'border-primary text-foreground' : 'text-muted-foreground border-transparent'
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>

      <LoadStatus state={periods.state} onRetry={periods.reload} label="Cargando los periodos…" />
      {periods.state.kind === 'success' && periods.state.data.length === 0 && (
        <p className="text-muted-foreground text-sm">Todavía no hay periodos académicos.</p>
      )}
      {periods.state.kind === 'success' &&
        periods.state.data.length > 0 &&
        (tab === 'comparative' ? (
          <ComparativeReport periods={periods.state.data} defaultPeriodId={defaultPeriod(periods.state.data).id} />
        ) : (
          <MomentReports tab={tab} periods={periods.state.data} />
        ))}
    </div>
  )
}

/**
 * Filtros de los reportes de un momento (periodo, momento y, según la pestaña, grupo y
 * estudiante) y el reporte elegido. La selección se conserva al cambiar de pestaña.
 */
function MomentReports({ tab, periods }: { tab: Exclude<ReportTab, 'comparative'>; periods: AcademicPeriod[] }) {
  const { user } = useAuth()
  const [periodId, setPeriodId] = useState(() => defaultPeriod(periods).id)
  const [momentId, setMomentId] = useState<number | null>(null)
  const [groupId, setGroupId] = useState<number | null>(null)
  const [studentId, setStudentId] = useState<number | null>(null)

  const moments = useLoad(() => momentService.list(periodId), [periodId])
  const momentList = moments.state.kind === 'success' ? moments.state.data : []
  const moment = momentList.find((item) => item.id === momentId) ?? momentList[0]

  const needsGroup = tab !== 'moment'
  const groups = useLoad(
    () => (needsGroup && moment ? groupService.list(moment.id) : Promise.resolve([])),
    [needsGroup, moment?.id],
  )
  const groupList = groups.state.kind === 'success' ? groups.state.data : []
  const group = groupList.find((item) => item.id === groupId) ?? groupList[0]
  const members = group ? [...group.members].sort((a, b) => a.fullName.localeCompare(b.fullName, 'es')) : []
  const student = members.find((item) => item.id === studentId) ?? members[0]

  const changePeriod = (id: number) => {
    setPeriodId(id)
    setMomentId(null)
    setGroupId(null)
    setStudentId(null)
  }
  const changeMoment = (id: number) => {
    setMomentId(id)
    setGroupId(null)
    setStudentId(null)
  }
  const changeGroup = (id: number) => {
    setGroupId(id)
    setStudentId(null)
  }

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-wrap items-end gap-4">
        <div className="w-36">
          <SelectField
            label="Periodo"
            value={String(periodId)}
            onChange={(event) => changePeriod(Number(event.target.value))}
          >
            {periods.map((period) => (
              <option key={period.id} value={String(period.id)}>
                {period.code}
              </option>
            ))}
          </SelectField>
        </div>
        {momentList.length > 0 && moment && (
          <div className="w-72">
            <SelectField
              label="Momento"
              value={String(moment.id)}
              onChange={(event) => changeMoment(Number(event.target.value))}
            >
              {momentList.map((item) => (
                <option key={item.id} value={String(item.id)}>
                  {item.name} · {item.programName}
                </option>
              ))}
            </SelectField>
          </div>
        )}
        {needsGroup && group && (
          <div className="w-56">
            <SelectField
              label="Grupo"
              value={String(group.id)}
              onChange={(event) => changeGroup(Number(event.target.value))}
            >
              {groupList.map((item) => (
                <option key={item.id} value={String(item.id)}>
                  {item.name}
                </option>
              ))}
            </SelectField>
          </div>
        )}
        {tab === 'individual' && student && (
          <div className="w-64">
            <SelectField
              label="Estudiante"
              value={String(student.id)}
              onChange={(event) => setStudentId(Number(event.target.value))}
            >
              {members.map((item) => (
                <option key={item.id} value={String(item.id)}>
                  {item.fullName}
                </option>
              ))}
            </SelectField>
          </div>
        )}
      </div>

      <LoadStatus state={moments.state} onRetry={moments.reload} label="Cargando los momentos…" />
      {moments.state.kind === 'success' && !moment && (
        <p className="text-muted-foreground text-sm">No hay momentos en este periodo.</p>
      )}
      {moment && tab === 'moment' && <MomentReportView momentId={moment.id} />}

      {moment && needsGroup && (
        <LoadStatus state={groups.state} onRetry={groups.reload} label="Cargando los grupos…" />
      )}
      {moment && needsGroup && groups.state.kind === 'success' && !group && (
        <p className="text-muted-foreground text-sm">
          {user?.role === 'TEACHER'
            ? 'No tienes grupos en este momento.'
            : 'Este momento todavía no tiene grupos.'}
        </p>
      )}
      {group && tab === 'group' && <GroupReportView groupId={group.id} />}
      {group && tab === 'individual' && !student && (
        <p className="text-muted-foreground text-sm">El grupo todavía no tiene integrantes.</p>
      )}
      {moment && student && tab === 'individual' && (
        <IndividualReportView momentId={moment.id} studentId={student.id} />
      )}
    </div>
  )
}

/** El periodo en curso o, si no hay, el primero de la lista. */
function defaultPeriod(periods: AcademicPeriod[]): AcademicPeriod {
  return periods.find((period) => period.status === 'CURRENT') ?? periods[0]
}
