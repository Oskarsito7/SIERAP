/** Aviso de que la pantalla muestra datos de ejemplo mientras su ruta de la API no existe. */
export function SampleDataNotice({ reason }: { reason: string }) {
  return (
    <p role="note" className="rounded-md border border-dashed px-3 py-2 text-xs text-muted-foreground">
      Datos de ejemplo: {reason}
    </p>
  )
}
