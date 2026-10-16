import { RESULTS_API_READY } from '@/services/api/resultsService'

/** Aviso de que las notas son de ejemplo mientras no existan las rutas de resultados. */
export function SampleDataNotice() {
  if (RESULTS_API_READY) return null
  return (
    <p role="note" className="rounded-md border border-dashed px-3 py-2 text-xs text-muted-foreground">
      Datos de ejemplo: el cálculo de las notas finales todavía no está publicado.
    </p>
  )
}
