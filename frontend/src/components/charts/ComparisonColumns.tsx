/** Una categoría del eje X con el valor de cada periodo (0 a 100, o `null` si no hay dato). */
export interface ComparisonColumn {
  key: string | number
  label: string
  a: number | null
  b: number | null
}

const TICKS = [100, 75, 50, 25, 0]

/**
 * Columnas agrupadas para comparar dos periodos en una escala de 0 a 100 % (wireframe
 * AD-08). El periodo base (`labelA`) va en gris y el que se compara (`labelB`) en el
 * color principal, con su leyenda. Al pasar el puntero o enfocar una categoría se leen
 * los dos valores; la tabla que acompaña a la gráfica los muestra siempre. Se dibuja con
 * HTML y Tailwind, sin librería de gráficas.
 */
export function ComparisonColumns({
  label,
  labelA,
  labelB,
  items,
  format,
}: {
  label: string
  labelA: string
  labelB: string
  items: ComparisonColumn[]
  /** Cómo se escribe un valor en el detalle, p. ej. "78 %". */
  format: (value: number | null) => string
}) {
  return (
    <figure aria-label={label} className="flex flex-col gap-3">
      <figcaption className="flex flex-wrap justify-end gap-4 text-xs">
        <Swatch className="bg-muted-foreground" text={labelA} />
        <Swatch className="bg-primary" text={labelB} />
      </figcaption>
      <div className="overflow-x-auto pt-2">
        <div className="flex min-w-max gap-2">
          <div aria-hidden className="text-muted-foreground relative h-48 w-10 shrink-0 text-right text-xs tabular-nums">
            {TICKS.map((tick) => (
              <span key={tick} className="absolute right-0 -translate-y-1/2" style={{ top: `${100 - tick}%` }}>
                {tick} %
              </span>
            ))}
          </div>
          <div className="relative flex-1">
            <div aria-hidden className="absolute inset-x-0 top-0 h-48">
              {TICKS.map((tick) => (
                <span key={tick} className="bg-border absolute inset-x-0 h-px" style={{ top: `${100 - tick}%` }} />
              ))}
            </div>
            <ul className="relative flex justify-around gap-2 px-1 sm:gap-4 sm:px-2">
              {items.map((item) => (
                <li
                  key={item.key}
                  tabIndex={0}
                  aria-label={`${item.label}: ${labelA} ${format(item.a)}, ${labelB} ${format(item.b)}`}
                  className="group focus-visible:ring-ring/50 relative flex flex-col items-center rounded-sm px-1 outline-none focus-visible:ring-2"
                >
                  <span className="flex h-48 items-end gap-0.5">
                    <Column value={item.a} className="bg-muted-foreground" />
                    <Column value={item.b} className="bg-primary" />
                  </span>
                  <span aria-hidden className="pt-2 text-xs">
                    {item.label}
                  </span>
                  <span
                    aria-hidden
                    className="bg-popover text-popover-foreground pointer-events-none absolute top-0 left-1/2 z-10 hidden -translate-x-1/2 rounded-md border px-2 py-1 text-xs whitespace-nowrap shadow-sm group-hover:block group-focus-visible:block"
                  >
                    <span className="block font-medium">{item.label}</span>
                    <span className="block">
                      <strong className="tabular-nums">{format(item.a)}</strong> {labelA}
                    </span>
                    <span className="block">
                      <strong className="tabular-nums">{format(item.b)}</strong> {labelB}
                    </span>
                  </span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </figure>
  )
}

/** Columna de un periodo: crece desde la base, con la punta redondeada. */
function Column({ value, className }: { value: number | null; className: string }) {
  const height = value === null ? 0 : Math.min(100, Math.max(0, value))
  return (
    <span
      className={`block w-4 rounded-t transition-[height] group-hover:opacity-80 sm:w-5 ${className}`}
      style={{ height: `${height}%` }}
    />
  )
}

function Swatch({ className, text }: { className: string; text: string }) {
  return (
    <span className="flex items-center gap-1.5">
      <span aria-hidden className={`h-2.5 w-3 rounded-[2px] ${className}`} />
      {text}
    </span>
  )
}
