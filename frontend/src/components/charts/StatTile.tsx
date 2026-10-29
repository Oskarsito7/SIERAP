/**
 * Cifra destacada de un reporte: su nombre, el valor y, si hace falta, un detalle
 * (p. ej. "de 5,00" o "18 de 20 estudiantes").
 */
export function StatTile({ label, value, detail }: { label: string; value: string; detail?: string }) {
  return (
    <div className="flex flex-col gap-1 rounded-lg border px-4 py-3">
      <span className="text-muted-foreground text-sm">{label}</span>
      <span className="text-2xl font-semibold tracking-tight">{value}</span>
      {detail && <span className="text-muted-foreground text-xs">{detail}</span>}
    </div>
  )
}
