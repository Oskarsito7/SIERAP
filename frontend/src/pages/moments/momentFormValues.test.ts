import { describe, expect, it } from 'vitest'

import { momentFixture } from '../groups/testData'
import {
  emptyMomentForm,
  toMomentData,
  toMomentForm,
  validateMomentForm,
  type MomentFormValues,
} from './momentFormValues'

const valid: MomentFormValues = toMomentForm(momentFixture())
const options = { requireProgram: false, upcDeadline: '2026-11-28' }

describe('validateMomentForm', () => {
  it('accepts a complete moment', () => {
    expect(validateMomentForm(valid, options)).toEqual({})
  })

  it('asks for the required data and whole numbers from 1', () => {
    const errors = validateMomentForm({ ...emptyMomentForm(), number: '0', minMembers: '2.5' },
      { ...options, requireProgram: true })
    expect(errors).toMatchObject({
      programId: 'El programa es obligatorio',
      periodId: 'El periodo académico es obligatorio',
      name: 'El nombre es obligatorio',
      number: 'Escribe un número entero desde 1',
      minMembers: 'Escribe un número entero desde 1',
    })
  })

  it('checks the ranges of semesters and members', () => {
    const values = { ...valid, semesterFrom: '4', semesterTo: '3', minMembers: '6', maxMembers: '5' }
    expect(validateMomentForm(values, options)).toEqual({
        semesterTo: 'Debe ser mayor o igual que el semestre inicial',
        maxMembers: 'Debe ser mayor o igual que el mínimo',
      })
  })

  it('wants each window complete or empty, in order and within the UPC deadline', () => {
    expect(validateMomentForm({ ...valid, submissionEnd: '' }, options))
      .toEqual({ submissionEnd: 'Pon las dos fechas de la ventana o deja ambas vacías' })
    expect(validateMomentForm({ ...valid, groupFormationEnd: '2026-09-01' }, options))
      .toEqual({ groupFormationEnd: 'Debe ser igual o posterior a la fecha de inicio' })
    expect(validateMomentForm({ ...valid, gradingEnd: '2026-11-30' }, options))
      .toEqual({ gradingEnd: 'Debe ser a más tardar el 28/11/2026 (límite de la UPC)' })
    expect(validateMomentForm({ ...valid, gradingStart: '', gradingEnd: '' }, options)).toEqual({})
  })
})

describe('toMomentData', () => {
  it('sends numbers as numbers and empty dates as null', () => {
    const data = toMomentData({ ...valid, name: '  Momento 1 ', gradingStart: '', gradingEnd: '' })
    expect(data).toMatchObject({ periodId: 1, number: 1, name: 'Momento 1', semesterFrom: 1, maxMembers: 5,
      gradingStart: null, gradingEnd: null, submissionStart: '2026-10-01' })
  })
})
