import { useState } from 'react'
import { Printer } from 'lucide-react'
import { useSearchParams } from 'react-router'

import { useAuth } from '@/auth/authContext'
import { LoadStatus } from '@/components/LoadStatus'
import { SelectField } from '@/components/forms/SelectField'
import { Button } from '@/components/ui/button'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { groupService } from '@/services/api/groupService'
import { momentService } from '@/services/api/momentService'
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
 * Reportes del super_admin, del admin y del docente (wireframes AD-08 a AD-10, RF 12): consolidado
 * de un momento, grupal, individual y comparativo entre dos periodos. Se ven en pantalla
 * con gráficas y tablas, y se imprimen (o se guardan como PDF) desde el navegador; la
 * exportación oficial a PDF o Excel queda para el Momento 2. El alcance lo
 * pone la API: el super_admin ve todas las facultades (y puede filtrar por una), el admin
 * ve su facultad y el docente, en el grupal y el individual, solo sus grupos. La pestaña
 * elegida queda en la dirección (`?tab=`).
 */
export function ReportsPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const tab = TABS.find((item) => item.tab === searchParams.get('tab'))?.tab ?? 'moment'
  const periods = useLoad(() => catalogService.periods(), [])
  const superAdmin = user?.role === 'SUPER_ADMIN'
  // Solo el super_admin filtra por facultad; las facultades salen de los programas.
  const programs = useLoad(() => (superAdmin ? catalogService.programs() : Promise.resolve([])), [superAdmin])
  const programList = programs.state.kind === 'success' ? programs.state.data : []
  const faculties = [...new Map(programList.map((program) => [program.facultyId, program.facultyName]))]
    .map(([id, name]) => ({ id, name }))
    .sort((a, b) => a.name.localeCompare(b.name, 'es'))
  const [facultyId, setFacultyId] = useState<number | null>(null)
  const programIds =
    facultyId === null
      ? null
      : new Set(programList.filter((program) => program.facultyId === facultyId).map((program) => program.id))

  return (
    <div className="flex flex-col gap-6">
      <div className="flex flex-col gap-1">
        <p className="hidden text-sm print:block">
          Universidad Popular del Cesar · SIERAP · Reporte {TABS.find((item) => item.tab === tab)?.label.toLowerCase()}
        </p>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h1 className="text-2xl font-bold tracking-tight">Reportes</h1>
          <Button variant="outline" size="sm" className="print:hidden" onClick={() => window.print()}>
            <Printer aria-hidden />
            Imprimir o guardar como PDF
          </Button>
        </div>
        <p className="text-muted-foreground max-w-2xl text-sm print:hidden">
          {user?.role === 'TEACHER'
            ? 'El desempeño por RAP de tus grupos y de los momentos de tu facultad.'
            : superAdmin
              ? 'El desempeño por RAP de los momentos, grupos y estudiantes de todas las facultades.'
              : 'El desempeño por RAP de los momentos, grupos y estudiantes de tu facultad.'}{' '}
          Mientras la calificación esté abierta, las cifras son una vista previa.
        </p>
      </div>

      <nav aria-label="Tipos de reporte" className="flex flex-wrap gap-1 border-b print:hidden">
        {TABS.map((item) => (
          <button
            key={item.tab}
            type="button"
            aria-current={item.tab === tab ? 'page' : undefined}
            onClick={() => setSearchParams({ tab: item.tab })}
            className={`-mb-px border-b-2 px-3 py-2 text-sm font-medium ${
              item.tab === tab ? 'border-primary text-upc-green-800 font-semibold' : 'text-muted-foreground border-transparent'
            }`}
          >
            {item.label}
          </button>
        ))}
      </nav>

      {superAdmin && faculties.length > 1 && (
        <div className="w-72 print:hidden">
          <SelectField
            label="Facultad"
            value={facultyId === null ? '' : String(facultyId)}
            onChange={(event) => setFacultyId(event.target.value ? Number(event.target.value) : null)}
          >
            <option value="">Todas las facultades</option>
            {faculties.map((faculty) => (
              <option key={faculty.id} value={String(faculty.id)}>
                {faculty.name}
              </option>
            ))}
          </SelectField>
        </div>
      )}

      <LoadStatus state={periods.state} onRetry={periods.reload} label="Cargando los periodos…" />
      {periods.state.kind === 'success' && periods.state.data.length === 0 && (
        <p className="text-muted-foreground text-sm">Todavía no hay periodos académicos.</p>
      )}
      {periods.state.kind === 'success' &&
        periods.state.data.length > 0 &&
        (tab === 'comparative' ? (
          <ComparativeReport
            key={facultyId ?? 'all'}
            periods={periods.state.data}
            defaultPeriodId={defaultPeriod(periods.state.data).id}
            facultyId={facultyId}
          />
        ) : (
          <MomentReports key={facultyId ?? 'all'} tab={tab} periods={periods.state.data} programIds={programIds} />
        ))}
    </div>
  )
}

/**
 * Filtros de los reportes de un momento (periodo, momento y, según la pestaña, grupo y
 * estudiante) y el reporte elegido. La selección se conserva al cambiar de pestaña.
 */
function MomentReports({
  tab,
  periods,
  programIds,
}: {
  tab: Exclude<ReportTab, 'comparative'>
  periods: AcademicPeriod[]
  /** Programas de la facultad elegida por el super_admin; `null` muestra todos. */
  programIds: Set<number> | null
}) {
  const { user } = useAuth()
  const [periodId, setPeriodId] = useState(() => defaultPeriod(periods).id)
  const [momentId, setMomentId] = useState<number | null>(null)
  const [groupId, setGroupId] = useState<number | null>(null)
  const [studentId, setStudentId] = useState<number | null>(null)

  const moments = useLoad(() => momentService.list(periodId), [periodId])
  const momentList =
    moments.state.kind === 'success'
      ? moments.state.data.filter((item) => programIds === null || programIds.has(item.programId))
      : []
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
      <div className="flex flex-wrap items-end gap-4 print:hidden">
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
