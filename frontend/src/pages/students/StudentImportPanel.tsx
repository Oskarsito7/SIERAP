import { useState } from 'react'
import { Download } from 'lucide-react'

import { FileDropzone } from '@/components/forms/FileDropzone'
import { FormError } from '@/components/forms/FormField'
import { SelectField } from '@/components/forms/SelectField'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { ResponsiveTable } from '@/components/ui/responsive-table'
import { saveFile } from '@/lib/files'
import { STUDENT_CSV_HEADER, studentCsvError, studentCsvTemplate } from '@/lib/studentCsv'
import { getErrorMessage } from '@/services/api/httpClient'
import { studentService } from '@/services/api/studentService'
import type { StudentImportPreview, StudentImportResult } from '@/types/account'
import type { Program } from '@/types/catalog'

/** Descarga la plantilla del CSV (se genera en el navegador). */
function downloadTemplate() {
  saveFile(new Blob([studentCsvTemplate()], { type: 'text/csv;charset=utf-8' }), 'plantilla_estudiantes.csv')
}

function studentsWord(count: number): string {
  return count === 1 ? '1 estudiante' : `${count} estudiantes`
}

/**
 * Carga masiva de estudiantes por CSV, en dos pasos: primero la vista previa (el backend
 * valida cada fila y no escribe nada) y, si hay filas válidas, la confirmación que crea
 * solo esas. Cada estudiante creado recibe un correo para crear su contraseña.
 */
export function StudentImportPanel({
  programs,
  initialProgramId,
  onImported,
  onClose,
}: {
  programs: Program[]
  /** Programa que la pantalla ya tiene elegido. */
  initialProgramId: number
  /** Se llama cuando se crearon estudiantes, para recargar la lista. */
  onImported: (created: number, programId: number) => void
  onClose: () => void
}) {
  const [programId, setProgramId] = useState(initialProgramId)
  const [file, setFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string | undefined>()
  const [preview, setPreview] = useState<StudentImportPreview | null>(null)
  const [result, setResult] = useState<StudentImportResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const resetResults = () => {
    setPreview(null)
    setResult(null)
    setError(null)
  }

  const review = async () => {
    resetResults()
    const problem = studentCsvError(file)
    setFileError(problem ?? undefined)
    if (problem || !file) return
    setBusy(true)
    try {
      setPreview(await studentService.previewImport(programId, file))
    } catch (previewError) {
      setError(getErrorMessage(previewError))
    } finally {
      setBusy(false)
    }
  }

  const create = async () => {
    if (!file) return
    setError(null)
    setBusy(true)
    try {
      const imported = await studentService.importStudents(programId, file)
      setResult(imported)
      setPreview(null)
      onImported(imported.created, programId)
    } catch (importError) {
      setError(getErrorMessage(importError))
    } finally {
      setBusy(false)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>Cargar estudiantes (CSV)</CardTitle>
        <CardDescription>
          El archivo debe tener la cabecera <code>{STUDENT_CSV_HEADER.join(',')}</code> (separada por comas o punto y
          coma), con tipo de documento CC, TI o CE, hasta 500 estudiantes y 1 MB. Primero se revisa el archivo; no se
          crea nada hasta que lo confirmes.
        </CardDescription>
      </CardHeader>
      <CardContent className="flex flex-col gap-4">
        <div>
          <Button type="button" variant="outline" size="sm" onClick={downloadTemplate}>
            <Download aria-hidden />
            Descargar plantilla
          </Button>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <SelectField
            label="Programa de los estudiantes"
            value={String(programId)}
            disabled={busy}
            onChange={(event) => {
              setProgramId(Number(event.target.value))
              resetResults()
            }}
          >
            {programs.map((program) => (
              <option key={program.id} value={String(program.id)}>
                {program.name}
              </option>
            ))}
          </SelectField>
          <FileDropzone
            label="Archivo CSV"
            hint="Un archivo .csv de hasta 1 MB, con UTF-8."
            accept=".csv,text/csv"
            file={file}
            error={fileError}
            disabled={busy}
            onChange={(chosen) => {
              setFile(chosen)
              setFileError(undefined)
              resetResults()
            }}
          />
        </div>

        <FormError message={error} />

        {result ? (
          <ImportResult result={result} />
        ) : (
          <div className="flex flex-wrap gap-3">
            <Button type="button" onClick={() => void review()} disabled={busy}>
              {busy && !preview ? 'Revisando…' : 'Revisar archivo'}
            </Button>
          </div>
        )}

        {preview && (
          <>
            <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
              {preview.valid} {preview.valid === 1 ? 'válida' : 'válidas'}, {preview.invalid} con errores
            </p>
            <PreviewTable preview={preview} />
            <div className="flex flex-wrap items-center gap-3">
              <Button type="button" onClick={() => void create()} disabled={busy || preview.valid === 0}>
                {busy ? 'Creando…' : `Crear ${studentsWord(preview.valid)}`}
              </Button>
              {preview.invalid > 0 && preview.valid > 0 && (
                <span className="text-muted-foreground text-sm">Las filas con errores no se crean.</span>
              )}
            </div>
          </>
        )}

        <div>
          <Button type="button" variant="outline" onClick={onClose}>
            {result ? 'Cerrar' : 'Cancelar'}
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}

function PreviewTable({ preview }: { preview: StudentImportPreview }) {
  return (
    <ResponsiveTable>
      <thead className="text-muted-foreground border-b">
        <tr>
          <th className="py-2 pr-4 font-medium">Fila</th>
          <th className="py-2 pr-4 font-medium">Nombre</th>
          <th className="py-2 pr-4 font-medium">Documento</th>
          <th className="py-2 pr-4 font-medium">Correo</th>
          <th className="py-2 pr-4 font-medium">Código</th>
          <th className="py-2 pr-4 text-right font-medium">Semestre</th>
          <th className="py-2 font-medium">Resultado</th>
        </tr>
      </thead>
      <tbody>
        {preview.rows.map((row) => (
          <tr key={row.row} className="border-b align-top last:border-0">
            <td className="py-3 pr-4 tabular-nums">{row.row}</td>
            <td className="py-3 pr-4">{`${row.firstNames} ${row.lastNames}`.trim() || '—'}</td>
            <td className="py-3 pr-4">{`${row.documentType} ${row.documentNumber}`.trim() || '—'}</td>
            <td className="py-3 pr-4 break-all">{row.email || '—'}</td>
            <td className="py-3 pr-4 tabular-nums">{row.studentCode || '—'}</td>
            <td className="py-3 pr-4 text-right tabular-nums">{row.currentSemester || '—'}</td>
            <td className="py-3">
              {row.errors.length === 0 ? (
                <Badge>Válida</Badge>
              ) : (
                <ul className="text-destructive list-disc pl-4">
                  {row.errors.map((message) => (
                    <li key={message}>{message}</li>
                  ))}
                </ul>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </ResponsiveTable>
  )
}

function ImportResult({ result }: { result: StudentImportResult }) {
  return (
    <div className="flex flex-col gap-3">
      <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
        {result.created === 1
          ? 'Se creó 1 estudiante. Recibirá un correo para crear su contraseña.'
          : `Se crearon ${result.created} estudiantes. Cada uno recibirá un correo para crear su contraseña.`}
      </p>
      {result.errors.length > 0 && (
        <div role="alert" className="border-destructive/50 text-destructive rounded-md border px-3 py-2 text-sm">
          <p>No se crearon {result.errors.length === 1 ? '1 fila' : `${result.errors.length} filas`}:</p>
          <ul className="list-disc pl-4">
            {result.errors.map((item) => (
              <li key={item.row}>
                Fila {item.row}: {item.message}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}
