import { Link } from 'react-router'

import { Button } from '@/components/ui/button'

/** Página que se muestra cuando la ruta no existe. */
export function NotFoundPage() {
  return (
    <div className="flex flex-col items-start gap-4">
      <h1 className="text-2xl font-bold tracking-tight">Página no encontrada</h1>
      <p className="text-muted-foreground">La dirección que buscas no existe en SIERAP.</p>
      <Button asChild>
        <Link to="/">Volver al inicio</Link>
      </Button>
    </div>
  )
}
