import { useEffect, useState } from 'react'
import { CircleCheck, CircleX, LoaderCircle, RefreshCw } from 'lucide-react'

import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { estadoService } from '@/services/api/estadoService'
import { obtenerMensajeError } from '@/services/api/httpClient'
import type { EstadoSistema } from '@/types/estado'

type EstadoCarga =
  | { tipo: 'cargando' }
  | { tipo: 'exito'; estado: EstadoSistema }
  | { tipo: 'error'; mensaje: string }

/**
 * Página de inicio provisional (Etapa 0): presenta el sistema y verifica que el
 * frontend se comunica con el backend y que este tiene conexión con PostgreSQL.
 */
export function InicioPage() {
  const [carga, setCarga] = useState<EstadoCarga>({ tipo: 'cargando' })
  const [intento, setIntento] = useState(0)

  useEffect(() => {
    let cancelado = false
    estadoService
      .consultar()
      .then((estado) => {
        if (!cancelado) setCarga({ tipo: 'exito', estado })
      })
      .catch((error: unknown) => {
        if (!cancelado) setCarga({ tipo: 'error', mensaje: obtenerMensajeError(error) })
      })
    return () => {
      cancelado = true
    }
  }, [intento])

  const reintentar = () => {
    setCarga({ tipo: 'cargando' })
    setIntento((anterior) => anterior + 1)
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
          {carga.tipo === 'cargando' && (
            <p className="text-muted-foreground flex items-center gap-2 text-sm">
              <LoaderCircle className="size-4 animate-spin" aria-hidden />
              Consultando el backend…
            </p>
          )}

          {carga.tipo === 'error' && (
            <div className="flex flex-col items-start gap-3">
              <Badge variant="destructive">
                <CircleX aria-hidden />
                Sin conexión con la API
              </Badge>
              <p className="text-sm">{carga.mensaje}</p>
              <p className="text-muted-foreground text-sm">
                Verifica que el backend esté corriendo en el puerto 8080.
              </p>
              <Button variant="outline" size="sm" onClick={reintentar}>
                <RefreshCw aria-hidden />
                Reintentar
              </Button>
            </div>
          )}

          {carga.tipo === 'exito' && <DetalleEstado estado={carga.estado} />}
        </CardContent>
      </Card>
    </div>
  )
}

function DetalleEstado({ estado }: { estado: EstadoSistema }) {
  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap gap-2">
        <Badge>
          <CircleCheck aria-hidden />
          API conectada
        </Badge>
        {estado.baseDatosDisponible ? (
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
        <dd>{estado.version}</dd>
        <dt className="text-muted-foreground">Perfil</dt>
        <dd>{estado.perfiles.join(', ') || '—'}</dd>
        <dt className="text-muted-foreground">Hora del servidor</dt>
        <dd>{new Date(estado.fechaHora).toLocaleString('es-CO')}</dd>
      </dl>
    </div>
  )
}
