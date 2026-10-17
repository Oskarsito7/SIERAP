import { useState, type FormEvent } from 'react'

import { LoadStatus } from '@/components/LoadStatus'
import { FormError } from '@/components/forms/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useLoad } from '@/lib/useLoad'
import { accountService } from '@/services/api/accountService'
import { getErrorMessage } from '@/services/api/httpClient'
import { momentService } from '@/services/api/momentService'
import type { EvaluatorType } from '@/types/account'
import type { MomentDetail } from '@/types/moment'

import { formatPercent, parsePercent, redistribute } from './weightValues'

const TEACHER = 'Docente del grupo'

/**
 * "Pesos de la nota" de un momento (wireframe AD-04, ADR-018): el porcentaje del docente
 * y de cada tipo de evaluador externo. Un tipo sin peso (casilla vacía) no cuenta en la
 * nota. Mientras se configura no tienen que sumar 100 %: eso se revisa al lanzar.
 */
export function WeightsTab({ moment, onChange }: { moment: MomentDetail; onChange: () => Promise<void> }) {
  const types = useLoad(() => accountService.listEvaluatorTypes(), [])

  if (types.state.kind !== 'success') {
    return <LoadStatus state={types.state} onRetry={types.reload} label="Cargando los tipos de evaluador…" />
  }
  return <WeightsForm key={weightsKey(moment)} moment={moment} types={types.state.data} onChange={onChange} />
}

interface Row {
  /** `null` para el docente. */
  typeId: number | null
  label: string
  value: string
}

function WeightsForm({
  moment,
  types,
  onChange,
}: {
  moment: MomentDetail
  types: EvaluatorType[]
  onChange: () => Promise<void>
}) {
  const editable = moment.status === 'CONFIGURATION'
  const weightOf = (typeId: number) => moment.evaluatorWeights.find((item) => item.evaluatorTypeId === typeId)?.weight
  // Los tipos inactivos solo aparecen si ya tienen peso en el momento.
  const visibleTypes = types.filter((type) => type.active || weightOf(type.id) !== undefined)
  const [rows, setRows] = useState<Row[]>(() => [
    { typeId: null, label: TEACHER, value: formatPercent(moment.teacherWeight) },
    ...visibleTypes.map((type) => {
      const weight = weightOf(type.id)
      return { typeId: type.id, label: type.name, value: weight === undefined ? '' : formatPercent(weight) }
    }),
  ])
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const parsed = rows.map((row) => ({ ...row, weight: parsePercent(row.value) }))
  const total = parsed.reduce((sum, row) => sum + (row.weight ?? 0), 0)
  const counted = parsed
    .filter((row) => row.weight !== null)
    .map((row) => ({ label: row.label, weight: row.weight as number }))
  const example = counted.length > 1 && Math.abs(total - 100) < 0.001 ? counted[counted.length - 1] : null

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setNotice(null)
    const found: Record<string, string> = {}
    for (const row of parsed) {
      const empty = row.value.trim() === ''
      if (row.typeId === null && empty) found[row.label] = 'El peso del docente es obligatorio'
      else if (!empty && row.weight === null) found[row.label] = 'Escribe un porcentaje mayor que 0 y hasta 100'
    }
    setErrors(found)
    if (Object.keys(found).length > 0) return

    setSaving(true)
    try {
      await momentService.updateWeights(moment.id, {
        teacherWeight: parsed[0].weight as number,
        evaluatorWeights: parsed
          .filter((row) => row.typeId !== null && row.weight !== null)
          .map((row) => ({ evaluatorTypeId: row.typeId as number, weight: row.weight as number })),
      })
      await onChange()
      setNotice('Se guardaron los pesos de la nota.')
    } catch (saveError) {
      setError(getErrorMessage(saveError))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(0,3fr)_minmax(0,2fr)]">
      <Card>
        <CardHeader>
          <CardTitle>Pesos de la nota</CardTitle>
          <CardDescription>
            Deja vacío el peso de un tipo de evaluador que no califica en este momento: su evaluación no contará.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <form className="flex flex-col gap-4" onSubmit={submit} noValidate>
            <table className="w-full text-left text-sm">
              <thead className="text-muted-foreground border-b">
                <tr>
                  <th className="py-2 pr-4 font-medium">Quién califica</th>
                  <th className="py-2 font-medium">Peso (%)</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row, index) => (
                  <tr key={row.label} className="border-b align-top">
                    <td className="py-3 pr-4 font-medium">{row.label}</td>
                    <td className="py-2">
                      <Input
                        aria-label={`Peso de ${row.label}`}
                        inputMode="decimal"
                        className="max-w-32"
                        value={row.value}
                        disabled={!editable}
                        aria-invalid={errors[row.label] ? true : undefined}
                        onChange={(event) =>
                          setRows((current) =>
                            current.map((item, position) =>
                              position === index ? { ...item, value: event.target.value } : item,
                            ),
                          )
                        }
                      />
                      {errors[row.label] && <p className="text-destructive mt-1 text-xs">{errors[row.label]}</p>}
                    </td>
                  </tr>
                ))}
                <tr>
                  <td className="py-3 pr-4 font-semibold">Total</td>
                  <td className="py-3 font-semibold" aria-label="Total de los pesos">
                    {formatPercent(total)} %
                  </td>
                </tr>
              </tbody>
            </table>
            {Math.abs(total - 100) >= 0.001 && (
              <p role="note" className="text-sm">
                Para lanzar el momento los pesos deben sumar 100 % (hoy suman {formatPercent(total)} %).
              </p>
            )}
            {notice && (
              <p role="status" className="bg-muted rounded-md px-3 py-2 text-sm">
                {notice}
              </p>
            )}
            <FormError message={error} />
            {editable && (
              <Button type="submit" className="w-fit" disabled={saving}>
                {saving ? 'Guardando…' : 'Guardar pesos'}
              </Button>
            )}
          </form>
        </CardContent>
      </Card>

      {example && (
        <Card className="bg-muted/40 self-start">
          <CardHeader>
            <CardTitle>Ejemplo de reparto</CardTitle>
            <CardDescription>
              Si un grupo no tiene evaluación enviada de un {example.label}, su {formatPercent(example.weight)} % se
              reparte entre los demás en proporción:
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ul className="divide-y text-sm">
              {redistribute(counted, example.label).map((item) => (
                <li key={item.label} className="flex justify-between py-2">
                  <span>{item.label}</span>
                  <span>
                    {formatPercent(item.before)} % → {formatPercent(item.after)} %
                  </span>
                </li>
              ))}
            </ul>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

/** Vuelve a armar el formulario cuando cambian los pesos guardados. */
function weightsKey(moment: MomentDetail): string {
  return JSON.stringify([moment.teacherWeight, moment.evaluatorWeights])
}
