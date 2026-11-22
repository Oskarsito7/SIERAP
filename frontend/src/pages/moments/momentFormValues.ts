import { formatDate } from '@/lib/dates'
import type { MomentData, MomentSummary } from '@/types/moment'

/** Valores del formulario tal como los escribe el admin (todo como texto). */
export interface MomentFormValues {
  programId: string
  periodId: string
  number: string
  name: string
  subject: string
  semesterFrom: string
  semesterTo: string
  requirementLevel: string
  minMembers: string
  maxMembers: string
  groupFormationStart: string
  groupFormationEnd: string
  submissionStart: string
  submissionEnd: string
  gradingStart: string
  gradingEnd: string
}

/** Campo del formulario de un momento. */
export type MomentFormField = keyof MomentFormValues

/** Las tres ventanas de fechas de un momento con sus campos. */
export const MOMENT_WINDOWS: { title: string; start: MomentFormField; end: MomentFormField }[] = [
  { title: 'Conformación de grupos', start: 'groupFormationStart', end: 'groupFormationEnd' },
  { title: 'Entregas', start: 'submissionStart', end: 'submissionEnd' },
  { title: 'Calificación', start: 'gradingStart', end: 'gradingEnd' },
]

const INTEGER_FIELDS: MomentFormField[] = ['number', 'semesterFrom', 'semesterTo', 'minMembers', 'maxMembers']

/** Formulario vacío de un momento nuevo, en el periodo elegido. */
export function emptyMomentForm(periodId?: number): MomentFormValues {
  return {
    programId: '', periodId: periodId ? String(periodId) : '', number: '', name: '', subject: '',
    semesterFrom: '', semesterTo: '', requirementLevel: '', minMembers: '', maxMembers: '',
    groupFormationStart: '', groupFormationEnd: '', submissionStart: '', submissionEnd: '',
    gradingStart: '', gradingEnd: '',
  }
}

/** Valores del formulario a partir de un momento existente. */
export function toMomentForm(moment: MomentSummary): MomentFormValues {
  return {
    programId: String(moment.programId),
    periodId: String(moment.periodId),
    number: String(moment.number),
    name: moment.name,
    subject: moment.subject,
    semesterFrom: String(moment.semesterFrom),
    semesterTo: String(moment.semesterTo),
    requirementLevel: moment.requirementLevel,
    minMembers: String(moment.minMembers),
    maxMembers: String(moment.maxMembers),
    groupFormationStart: moment.groupFormationStart ?? '',
    groupFormationEnd: moment.groupFormationEnd ?? '',
    submissionStart: moment.submissionStart ?? '',
    submissionEnd: moment.submissionEnd ?? '',
    gradingStart: moment.gradingStart ?? '',
    gradingEnd: moment.gradingEnd ?? '',
  }
}

/** Datos para la API: los números como números y las fechas vacías como `null`. */
export function toMomentData(values: MomentFormValues): MomentData {
  const date = (value: string) => value || null
  return {
    periodId: Number(values.periodId),
    number: Number(values.number),
    name: values.name.trim(),
    subject: values.subject.trim(),
    semesterFrom: Number(values.semesterFrom),
    semesterTo: Number(values.semesterTo),
    requirementLevel: values.requirementLevel.trim(),
    minMembers: Number(values.minMembers),
    maxMembers: Number(values.maxMembers),
    groupFormationStart: date(values.groupFormationStart),
    groupFormationEnd: date(values.groupFormationEnd),
    submissionStart: date(values.submissionStart),
    submissionEnd: date(values.submissionEnd),
    gradingStart: date(values.gradingStart),
    gradingEnd: date(values.gradingEnd),
  }
}

/**
 * Validación del cliente, con los mismos mensajes que el admin vería de la API; el
 * backend vuelve a validar todo (también el orden de las ventanas) y responde 400 o 422.
 */
export function validateMomentForm(
  values: MomentFormValues,
  options: { requireProgram: boolean; upcDeadline: string | null },
): Partial<Record<MomentFormField, string>> {
  const errors: Partial<Record<MomentFormField, string>> = {}
  if (options.requireProgram && !values.programId) errors.programId = 'El programa es obligatorio'
  if (!values.periodId) errors.periodId = 'El periodo académico es obligatorio'
  if (!values.name.trim()) errors.name = 'El nombre es obligatorio'
  if (!values.subject.trim()) errors.subject = 'La asignatura es obligatoria'
  if (!values.requirementLevel.trim()) errors.requirementLevel = 'El nivel de exigencia es obligatorio'
  for (const field of INTEGER_FIELDS) {
    if (!/^\d+$/.test(values[field]) || Number(values[field]) < 1) errors[field] = 'Escribe un número entero desde 1'
  }
  if (!errors.semesterFrom && !errors.semesterTo && Number(values.semesterFrom) > Number(values.semesterTo)) {
    errors.semesterTo = 'Debe ser mayor o igual que el semestre inicial'
  }
  if (!errors.minMembers && !errors.maxMembers && Number(values.minMembers) > Number(values.maxMembers)) {
    errors.maxMembers = 'Debe ser mayor o igual que el mínimo'
  }
  for (const window of MOMENT_WINDOWS) {
    const start = values[window.start]
    const end = values[window.end]
    if (Boolean(start) !== Boolean(end)) errors[window.end] = 'Pon las dos fechas de la ventana o deja ambas vacías'
    else if (start && end && start > end) errors[window.end] = 'Debe ser igual o posterior a la fecha de inicio'
  }
  if (!errors.gradingEnd && options.upcDeadline && values.gradingEnd && values.gradingEnd > options.upcDeadline) {
    errors.gradingEnd = `Debe ser a más tardar el ${formatDate(options.upcDeadline)} (límite de la UPC)`
  }
  return errors
}
