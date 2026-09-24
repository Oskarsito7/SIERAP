import { useEffect, useState } from 'react'
import { CircleCheck, CircleX, LoaderCircle, RefreshCw } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { getErrorMessage } from '@/services/api/httpClient'
import { systemStatusService } from '@/services/api/systemStatusService'
import type { SystemStatus } from '@/types/systemStatus'

type LoadState =
  | { kind: 'loading' }
  | { kind: 'success'; status: SystemStatus }
  | { kind: 'error'; message: string }

/**
 * Temporary home page (Stage 0): introduces the system and checks that the
 * frontend reaches the backend and that the backend is connected to PostgreSQL.
 */
export function HomePage() {
  const [load, setLoad] = useState<LoadState>({ kind: 'loading' })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    systemStatusService
      .getStatus()
      .then((status) => {
        if (!cancelled) setLoad({ kind: 'success', status })
      })
      .catch((error: unknown) => {
        if (!cancelled) setLoad({ kind: 'error', message: getErrorMessage(error) })
      })
    return () => {
      cancelled = true
    }
  }, [attempt])

  const retry = () => {
    setLoad({ kind: 'loading' })
    setAttempt((previous) => previous + 1)
  }

  return (
    <div className="flex flex-col gap-8">
      <section className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight">Bienvenido a SIERAP</h1>
        <p className="text-muted-foreground max-w-2xl">
          Gestión y evaluación de los proyectos de aula y de los Resultados de Aprendizaje del
          Programa (RAP) de la Universidad Popular del Cesar.
        </p>
      </section>

      <Card className="max-w-xl">
        <CardHeader>
          <CardTitle>Estado del sistema</CardTitle>
          <CardDescription>Conexión con la API y con la base de datos.</CardDescription>
        </CardHeader>
        <CardContent>
          {load.kind === 'loading' && (
            <p className="text-muted-foreground flex items-center gap-2 text-sm">
              <LoaderCircle className="size-4 animate-spin" aria-hidden />
              Consultando el backend…
            </p>
          )}

          {load.kind === 'error' && (
            <div className="flex flex-col items-start gap-3">
              <Badge variant="destructive">
                <CircleX aria-hidden />
                Sin conexión con la API
              </Badge>
              <p className="text-sm">{load.message}</p>
              <p className="text-muted-foreground text-sm">
                Verifica que el backend esté corriendo en el puerto 8080.
              </p>
              <Button variant="outline" size="sm" onClick={retry}>
                <RefreshCw aria-hidden />
                Reintentar
              </Button>
            </div>
          )}

          {load.kind === 'success' && <StatusDetail status={load.status} />}
        </CardContent>
      </Card>
    </div>
  )
}

function StatusDetail({ status }: { status: SystemStatus }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        <Badge>
          <CircleCheck aria-hidden />
          API conectada
        </Badge>
        {status.databaseAvailable ? (
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
      <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
        <dt className="text-muted-foreground">Versión</dt>
        <dd>{status.version}</dd>
        <dt className="text-muted-foreground">Perfil</dt>
        <dd>{status.profiles.join(', ') || '—'}</dd>
        <dt className="text-muted-foreground">Hora del servidor</dt>
        <dd>{new Date(status.timestamp).toLocaleString('es-CO')}</dd>
      </dl>
    </div>
  )
}
