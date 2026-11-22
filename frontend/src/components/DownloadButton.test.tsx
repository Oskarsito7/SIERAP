import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { saveFile } from '@/lib/files'

import { DownloadButton } from './DownloadButton'

vi.mock('@/lib/files', () => ({ saveFile: vi.fn() }))

describe('DownloadButton', () => {
  beforeEach(() => vi.mocked(saveFile).mockReset())

  it('saves the downloaded file with the name proposed by the server', async () => {
    const blob = new Blob(['%PDF'])
    render(<DownloadButton download={async () => ({ blob, fileName: 'acta-grupo-1.pdf' })}>Acta en PDF</DownloadButton>)

    await userEvent.click(screen.getByRole('button', { name: 'Acta en PDF' }))

    expect(saveFile).toHaveBeenCalledWith(blob, 'acta-grupo-1.pdf')
    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
  })

  it('shows the message of the API when the download fails', async () => {
    const data = new Blob([JSON.stringify({ message: 'No tiene permisos para consultar este reporte.' })])
    const error = new AxiosError('error', 'ERR_BAD_REQUEST', undefined, null, { status: 403, data } as AxiosResponse)
    render(<DownloadButton download={() => Promise.reject(error)}>Informe en PDF</DownloadButton>)

    await userEvent.click(screen.getByRole('button', { name: 'Informe en PDF' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('No tiene permisos para consultar este reporte.')
    expect(saveFile).not.toHaveBeenCalled()
  })
})
