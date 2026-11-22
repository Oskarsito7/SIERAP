import { formatDate, todayIso } from '@/lib/dates'
import type { MomentSummary } from '@/types/moment'

/** Fase de un momento tal como la ve el usuario, con el color de su etiqueta. */
export interface MomentPhase {
  label: string
  /** Fecha hasta la que dura la fase, ya formateada; `null` si no aplica. */
  until: string | null
  variant: 'default' | 'secondary' | 'outline'
}

/**
 * Fase de un momento (ADR-012). Las ventanas abiertas salen de las banderas que
 * calcula el backend (`groupFormationOpen`, `submissionOpen`, `gradingOpen`); la fecha
 * del navegador solo distingue un momento cerrado de uno entre dos ventanas.
 */
export function momentPhase(moment: MomentSummary): MomentPhase {
  if (moment.status === 'CONFIGURATION') return { label: 'En configuración', until: null, variant: 'outline' }
  if (moment.gradingOpen) return { label: 'Calificación', until: optionalDate(moment.gradingEnd), variant: 'default' }
  if (moment.submissionOpen) return { label: 'Entregas', until: optionalDate(moment.submissionEnd), variant: 'default' }
  if (moment.groupFormationOpen) {
    return { label: 'Conformación de grupos', until: optionalDate(moment.groupFormationEnd), variant: 'default' }
  }
  if (moment.gradingEnd && todayIso() > moment.gradingEnd) return { label: 'Cerrado', until: null, variant: 'secondary' }
  return { label: 'Lanzado', until: null, variant: 'secondary' }
}

function optionalDate(date: string | null): string | null {
  return date ? formatDate(date) : null
}

/** Cantidad con su sustantivo en singular o plural, p. ej. "1 grupo" o "3 grupos". */
export function plural(count: number, singular: string, pluralForm = `${singular}s`): string {
  return `${count} ${count === 1 ? singular : pluralForm}`
}
