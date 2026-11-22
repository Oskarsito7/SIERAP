import { useState, type ReactNode } from 'react'
import { Download } from 'lucide-react'

import { Button } from '@/components/ui/button'
import { saveFile } from '@/lib/files'
import { getDownloadErrorMessage, type DownloadedFile } from '@/services/api/httpClient'

/**
 * Botón que descarga un archivo de la API (pedido con el token) y lo guarda con el
 * nombre que propone el servidor. Mientras descarga dice "Descargando…" y, si falla,
 * muestra el mensaje de la API debajo.
 */
export function DownloadButton({
  children,
  download,
  variant = 'outline',
}: {
  children: ReactNode
  download: () => Promise<DownloadedFile>
  variant?: 'outline' | 'default'
}) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const run = async () => {
    setError(null)
    setBusy(true)
    try {
      const file = await download()
      saveFile(file.blob, file.fileName)
    } catch (downloadError) {
      setError(await getDownloadErrorMessage(downloadError))
    } finally {
      setBusy(false)
    }
  }

  return (
    <span className="inline-flex flex-col items-start gap-1">
      <Button type="button" size="sm" variant={variant} disabled={busy} onClick={run}>
        <Download aria-hidden />
        {busy ? 'Descargando…' : children}
      </Button>
      {error && (
        <span role="alert" className="text-destructive text-xs">
          {error}
        </span>
      )}
    </span>
  )
}
