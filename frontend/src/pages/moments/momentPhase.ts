import { formatDate } from '@/lib/dates'
import type { MomentSummary } from '@/types/moment'

type WindowKey = 'groupFormation' | 'submission' | 'grading'

const WINDOWS: { key: WindowKey; open: string; upcoming: string }[] = [
  { key: 'groupFormation', open: 'Conformación de grupos abierta', upcoming: 'Conformación de grupos' },
  { key: 'submission', open: 'Entregas abiertas', upcoming: 'Entregas' },
  { key: 'grading', open: 'Calificación abierta', upcoming: 'Calificación' },
]

function windowOf(moment: MomentSummary, key: WindowKey): { start: string; end: string } | null {
  const start = moment[`${key}Start`]
  const end = moment[`${key}End`]
  return start && end ? { start, end } : null
}

/**
 * Fase del momento hoy, para la lista de momentos (wireframe AD-01): la ventana abierta
 * con su fecha de cierre, o la próxima con su fecha de apertura. Lanzar el momento cierra
 * la conformación de grupos (`docs/04`), así que después del lanzamiento esa ventana
 * ya no cuenta como abierta.
 */
export function momentPhase(moment: MomentSummary, today: string): { label: string; detail: string | null } {
  const windows = WINDOWS.map((window) => ({ ...window, dates: windowOf(moment, window.key) }))
  if (windows.every((window) => window.dates === null)) return { label: 'Sin ventanas definidas', detail: null }

  const grading = windows[2].dates
  if (grading && today > grading.end) return { label: 'Calificación cerrada', detail: `el ${formatDate(grading.end)}` }

  for (const window of windows) {
    if (!window.dates || today < window.dates.start || today > window.dates.end) continue
    if (window.key === 'groupFormation' && moment.status === 'LAUNCHED') continue
    return { label: window.open, detail: `hasta el ${formatDate(window.dates.end)}` }
  }

  const next = windows.find((window) => window.dates && today < window.dates.start)
  if (next?.dates) return { label: next.upcoming, detail: `desde el ${formatDate(next.dates.start)}` }
  return { label: 'Sin ventana abierta', detail: null }
}

/** Paso de la línea de tiempo de un momento (wireframe AD-02). */
export interface MomentStep {
  label: string
  detail: string
  current: boolean
}

function shortRange(dates: { start: string; end: string } | null): string {
  if (!dates) return 'Sin fechas'
  return `${formatDate(dates.start).slice(0, 5)} – ${formatDate(dates.end).slice(0, 5)}`
}

/**
 * Pasos del flujo de un momento (ADR-012): configuración, conformación, lanzamiento,
 * entregas, calificación y cierre, con el paso en que está hoy.
 */
export function momentSteps(moment: MomentSummary, today: string): MomentStep[] {
  const formation = windowOf(moment, 'groupFormation')
  const submission = windowOf(moment, 'submission')
  const grading = windowOf(moment, 'grading')
  const inside = (dates: { start: string; end: string } | null) =>
    dates !== null && today >= dates.start && today <= dates.end

  let current: number
  if (moment.status === 'CONFIGURATION') current = inside(formation) ? 1 : 0
  else if (grading && today > grading.end) current = 5
  else if (inside(grading)) current = 4
  else if (inside(submission)) current = 3
  else current = 2

  return [
    { label: 'Configuración', detail: 'Admin y docentes' },
    { label: 'Conformación', detail: shortRange(formation) },
    { label: 'Lanzamiento', detail: 'Admin' },
    { label: 'Entregas', detail: shortRange(submission) },
    { label: 'Calificación', detail: shortRange(grading) },
    { label: 'Cierre', detail: 'Notas finales' },
  ].map((step, index) => ({ ...step, current: index === current }))
}
