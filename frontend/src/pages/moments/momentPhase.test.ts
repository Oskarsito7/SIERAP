import { describe, expect, it } from 'vitest'

import { momentFixture } from '../groups/testData'
import { momentPhase, momentSteps } from './momentPhase'

// Ventanas del fixture: conformación 20–30/09, entregas 01–31/10, calificación 02–20/11 de 2026.
const configuring = momentFixture()
const launched = momentFixture({ status: 'LAUNCHED' })

describe('momentPhase', () => {
  it('says when there are no windows yet', () => {
    const moment = momentFixture({
      groupFormationStart: null, groupFormationEnd: null, submissionStart: null, submissionEnd: null,
      gradingStart: null, gradingEnd: null,
    })
    expect(momentPhase(moment, '2026-09-25')).toEqual({ label: 'Sin ventanas definidas', detail: null })
  })

  it('shows the open window with its end and the next one with its start', () => {
    expect(momentPhase(configuring, '2026-09-25'))
      .toEqual({ label: 'Conformación de grupos abierta', detail: 'hasta el 30/09/2026' })
    expect(momentPhase(configuring, '2026-09-10'))
      .toEqual({ label: 'Conformación de grupos', detail: 'desde el 20/09/2026' })
    expect(momentPhase(launched, '2026-10-05')).toEqual({ label: 'Entregas abiertas', detail: 'hasta el 31/10/2026' })
    expect(momentPhase(launched, '2026-11-01')).toEqual({ label: 'Calificación', detail: 'desde el 02/11/2026' })
    expect(momentPhase(launched, '2026-11-21')).toEqual({ label: 'Calificación cerrada', detail: 'el 20/11/2026' })
  })

  it('does not count group formation as open once the moment was launched', () => {
    expect(momentPhase(launched, '2026-09-25')).toEqual({ label: 'Entregas', detail: 'desde el 01/10/2026' })
  })
})

describe('momentSteps', () => {
  const current = (steps: ReturnType<typeof momentSteps>) => steps.find((step) => step.current)?.label

  it('marks the step of today', () => {
    expect(current(momentSteps(configuring, '2026-09-10'))).toBe('Configuración')
    expect(current(momentSteps(configuring, '2026-09-25'))).toBe('Conformación')
    expect(current(momentSteps(launched, '2026-09-25'))).toBe('Lanzamiento')
    expect(current(momentSteps(launched, '2026-10-05'))).toBe('Entregas')
    expect(current(momentSteps(launched, '2026-11-05'))).toBe('Calificación')
    expect(current(momentSteps(launched, '2026-12-01'))).toBe('Cierre')
  })

  it('shows the dates of each window', () => {
    expect(momentSteps(configuring, '2026-09-10').map((step) => step.detail))
      .toEqual(['Admin y docentes', '20/09 – 30/09', 'Admin', '01/10 – 31/10', '02/11 – 20/11', 'Notas finales'])
  })
})
