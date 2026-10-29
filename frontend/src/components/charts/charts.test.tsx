import { render, screen, within } from '@testing-library/react'
import { describe, expect, it } from 'vitest'

import { ComparisonColumns } from './ComparisonColumns'
import { PercentBars } from './PercentBars'

describe('PercentBars', () => {
  it('writes the value of each bar and sizes it on a 0 to 100 scale', () => {
    render(
      <PercentBars
        label="Logro por RAP"
        items={[
          { key: 1, label: 'RAP1', percentage: 78.33, value: '78,33 %', detail: '0,47 de 0,60' },
          { key: 2, label: 'RAP2', percentage: 120, value: '120 %' },
          { key: 3, label: 'RAP3', percentage: null, value: '—' },
        ]}
      />,
    )

    const items = within(screen.getByRole('list', { name: 'Logro por RAP' })).getAllByRole('listitem')
    expect(items[0]).toHaveTextContent('RAP178,33 %')
    const fills = items.map((item) => item.querySelector<HTMLElement>('.bg-primary')?.style.width)
    expect(fills).toEqual(['78.33%', '100%', '0%'])
    // Solo las filas con detalle se pueden enfocar para leerlo con el teclado.
    expect(items[0]).toHaveAttribute('tabindex', '0')
    expect(items[1]).not.toHaveAttribute('tabindex')
  })
})

describe('ComparisonColumns', () => {
  it('names both periods in the legend and in each category', () => {
    render(
      <ComparisonColumns
        label="Desempeño por RAP"
        labelA="2026-1"
        labelB="2026-2"
        format={(value) => (value === null ? '—' : `${value} %`)}
        items={[
          { key: 'RAP1', label: 'RAP1', a: 72, b: 78 },
          { key: 'RAP6', label: 'RAP6', a: null, b: 50 },
        ]}
      />,
    )

    const figure = screen.getByRole('figure', { name: 'Desempeño por RAP' })
    expect(within(figure).getByText('2026-1', { selector: 'figcaption span' })).toBeInTheDocument()
    expect(screen.getByRole('listitem', { name: 'RAP1: 2026-1 72 %, 2026-2 78 %' })).toBeInTheDocument()
    const rap6 = screen.getByRole('listitem', { name: 'RAP6: 2026-1 —, 2026-2 50 %' })
    const heights = [...rap6.querySelectorAll<HTMLElement>('.rounded-t')].map((column) => column.style.height)
    expect(heights).toEqual(['0%', '50%'])
  })
})
