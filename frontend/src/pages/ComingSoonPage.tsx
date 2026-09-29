import { Construction } from 'lucide-react'
import { Link } from 'react-router'

import { Button } from '@/components/ui/button'
import { Card, CardContent } from '@/components/ui/card'

/** Datos de la sección que todavía no tiene página. */
export interface ComingSoonPageProps {
  /** Qué hará la página, p. ej. "La gestión de tus grupos". */
  description: string
  /** Etapa del plan de trabajo (`docs/06`) en la que llega. */
  stage: number
}

/**
 * Pantalla "Próximamente" (wireframe AU-07): mientras una opción del menú no tenga su
 * página, lleva aquí y dice en qué etapa llega.
 */
export function ComingSoonPage({ description, stage }: ComingSoonPageProps) {
  return (
    <Card className="bg-muted/40">
      <CardContent className="flex min-h-80 flex-col items-center justify-center gap-4 text-center">
        <div className="bg-muted text-muted-foreground grid size-16 place-items-center rounded-full">
          <Construction className="size-8" aria-hidden />
        </div>
        <h1 className="text-2xl font-bold tracking-tight">Próximamente</h1>
        <p className="text-muted-foreground max-w-md">
          {description} llega en la Etapa {stage}.
        </p>
        <Button variant="outline" asChild>
          <Link to="/">Volver al inicio</Link>
        </Button>
      </CardContent>
    </Card>
  )
}
