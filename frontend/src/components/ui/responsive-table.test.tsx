import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { ResponsiveTable } from './responsive-table'

describe('ResponsiveTable', () => {
  it('copies each column title to its cells so the phone layout can show it', () => {
    render(
      <ResponsiveTable aria-label="Estudiantes">
        <thead>
          <tr>
            <th>Nombre</th>
            <th>Código</th>
            <th>
              <span className="sr-only">Acciones</span>
            </th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>Ana Gómez</td>
            <td>2021115001</td>
            <td>
              <button type="button">Editar</button>
            </td>
          </tr>
        </tbody>
      </ResponsiveTable>,
    )

    expect(screen.getByRole('table', { name: 'Estudiantes' })).toHaveClass('responsive-table')
    expect(screen.getByRole('cell', { name: 'Ana Gómez' })).toHaveAttribute('data-label', 'Nombre')
    expect(screen.getByRole('cell', { name: '2021115001' })).toHaveAttribute('data-label', 'Código')
    // Las columnas con título solo para lectores de pantalla no muestran etiqueta.
    expect(screen.getByRole('cell', { name: 'Editar' })).toHaveAttribute('data-label', '')
  })
})
