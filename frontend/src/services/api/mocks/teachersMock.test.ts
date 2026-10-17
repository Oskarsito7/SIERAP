import { describe, expect, it } from 'vitest'

import { getErrorMessage } from '../httpClient'
import { teachersMock } from './teachersMock'

describe('teachersMock', () => {
  it('keeps the created teachers in memory and rejects a repeated email like the API', async () => {
    const created = await teachersMock.create({ firstNames: 'Ana', lastNames: 'Torres', documentType: 'CE',
      documentNumber: '99', email: 'Ana.Torres@unicesar.edu.co' })

    expect(created).toMatchObject({ fullName: 'Ana Torres', email: 'ana.torres@unicesar.edu.co', groupCount: 0 })
    expect((await teachersMock.list()).map((teacher) => teacher.id)).toContain(created.id)

    const repeated = teachersMock.create({ firstNames: 'Otra', lastNames: 'Persona', documentType: 'CC',
      documentNumber: '1', email: 'ana.torres@unicesar.edu.co' })
    await expect(repeated).rejects
      .toSatisfy((error) => getErrorMessage(error) === 'Ya existe una cuenta con ese correo.')
  })

  it('changes the state of a teacher', async () => {
    const [first] = await teachersMock.list()

    expect((await teachersMock.setActive(first.id, false)).active).toBe(false)
    expect((await teachersMock.list()).find((teacher) => teacher.id === first.id)?.active).toBe(false)
  })
})
