/** Una barra: su etiqueta, su porcentaje (0 a 100) y el texto que va en la punta. */
export interface PercentBar {
  key: string | number
  label: string
  /** Texto pequeño bajo la etiqueta, p. ej. el rango de una franja de notas. */
  sublabel?: string
  /** `null` si no hay dato: la barra queda vacía. */
  percentage: number | null
  /** Valor que se lee en la punta de la barra, p. ej. "78,33 %". */
  value: string
  /** Detalle que aparece al pasar el puntero o enfocar la fila, p. ej. "0,47 de 0,60". */
  detail?: string
}

/**
 * Barras horizontales de una sola serie en una escala de 0 a 100 % (el logro de cada
 * RAP o la parte de los estudiantes en cada franja). Cada fila lleva su valor escrito,
 * así que la barra nunca es la única forma de leerlo; el detalle aparece con el puntero
 * o con el teclado. Se dibuja con HTML y Tailwind, sin librería de gráficas.
 */
export function PercentBars({ label, items }: { label: string; items: PercentBar[] }) {
  return (
    <ul aria-label={label} className="flex flex-col gap-3">
      {items.map((item) => (
        <li
          key={item.key}
          tabIndex={item.detail ? 0 : undefined}
          className="group focus-visible:ring-ring/50 relative grid grid-cols-[minmax(4rem,auto)_1fr_auto] items-center gap-3 rounded-sm outline-none focus-visible:ring-2"
        >
          <span className="flex flex-col">
            <span className="text-sm font-medium">{item.label}</span>
            {item.sublabel && <span className="text-muted-foreground text-xs tabular-nums">{item.sublabel}</span>}
          </span>
          <span aria-hidden className="bg-muted h-3 overflow-hidden rounded-r">
            <span
              className="bg-primary block h-full rounded-r transition-[width] group-hover:opacity-80"
              style={{ width: `${clamp(item.percentage)}%` }}
            />
          </span>
          <span className="text-sm tabular-nums">{item.value}</span>
          {item.detail && (
            <span
              aria-hidden
              className="bg-popover text-popover-foreground pointer-events-none absolute right-0 bottom-full z-10 mb-1 hidden rounded-md border px-2 py-1 text-xs shadow-sm group-hover:block group-focus-visible:block"
            >
              {item.detail}
            </span>
          )}
        </li>
      ))}
    </ul>
  )
}

/** Ancho de la barra: entre 0 y 100, y 0 si no hay dato. */
function clamp(percentage: number | null): number {
  if (percentage === null) return 0
  return Math.min(100, Math.max(0, percentage))
}
