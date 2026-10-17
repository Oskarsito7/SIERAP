import { SampleDataNotice as Notice } from '@/components/SampleDataNotice'
import { RESULTS_API_READY } from '@/services/api/resultsService'

/** Aviso de que las notas son de ejemplo mientras no existan las rutas de resultados. */
export function SampleDataNotice() {
  if (RESULTS_API_READY) return null
  return <Notice reason="el cálculo de las notas finales todavía no está publicado." />
}
