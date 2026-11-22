import { ArrowLeft } from 'lucide-react'
import { Link, useNavigate, useSearchParams } from 'react-router'

import { LoadStatus } from '@/components/LoadStatus'
import { Button } from '@/components/ui/button'
import { useLoad } from '@/lib/useLoad'
import { catalogService } from '@/services/api/catalogService'
import { momentService } from '@/services/api/momentService'

import { MomentForm } from './MomentForm'
import { emptyMomentForm, toMomentData } from './momentFormValues'

/**
 * Crear un momento (wireframe AD-02, en blanco): queda en configuración, con el docente
 * al 100 % de la nota y sin RAP. Al crearlo se abre su configuración.
 */
export function NewMomentPage() {
  const navigate = useNavigate()
  const periodId = Number(useSearchParams()[0].get('periodId')) || undefined
  const catalogs = useLoad(async () => {
    const [periods, programs] = await Promise.all([catalogService.periods(), catalogService.programs()])
    return { periods, programs }
  }, [])
  const back = periodId ? `/moments?periodId=${periodId}` : '/moments'

  return (
    <div className="flex flex-col gap-6">
      <Link to={back} className="text-muted-foreground flex w-fit items-center gap-1 text-sm hover:underline">
        <ArrowLeft className="size-4" aria-hidden />
        Momentos
      </Link>
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-bold tracking-tight">Nuevo momento</h1>
        <p className="text-muted-foreground max-w-2xl text-sm">
          Después de crearlo podrás agregar sus RAP, criterios y niveles y fijar los pesos de la nota.
        </p>
      </div>
      <LoadStatus state={catalogs.state} onRetry={catalogs.reload} label="Cargando los programas…" />
      {catalogs.state.kind === 'success' && (
        <MomentForm
          initial={emptyMomentForm(periodId)}
          periods={catalogs.state.data.periods}
          programs={catalogs.state.data.programs}
          submitLabel="Crear momento"
          onSubmit={async (values) => {
            const created = await momentService.create({ ...toMomentData(values), programId: Number(values.programId) })
            navigate(`/moments/${created.id}`)
          }}
          actions={
            <Button type="button" variant="outline" asChild>
              <Link to={back}>Cancelar</Link>
            </Button>
          }
        />
      )}
    </div>
  )
}
