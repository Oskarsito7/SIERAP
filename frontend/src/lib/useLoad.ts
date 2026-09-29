import { useCallback, useEffect, useState, type Dispatch, type SetStateAction } from 'react'

import { getErrorMessage } from '@/services/api/httpClient'

/** Estado de una carga: cargando, con datos o con el mensaje de error de la API. */
export type LoadState<T> = { kind: 'loading' } | { kind: 'success'; data: T } | { kind: 'error'; message: string }

type KeyedState<T> = { key: string; state: LoadState<T> }

/**
 * Carga datos de la API al montar el componente y cada vez que cambian `deps`.
 * Devuelve el estado, `reload` para reintentar y `setData` para reemplazar los datos
 * con la respuesta de una acción (p. ej. el grupo actualizado) sin volver a pedirlos.
 *
 * Cada resultado se guarda con la clave de las dependencias que lo produjeron: si ya
 * no coincide con la actual, el estado es "cargando" y la respuesta vieja se descarta.
 */
export function useLoad<T>(
  load: () => Promise<T>,
  deps: readonly unknown[],
): { state: LoadState<T>; reload: () => void; setData: Dispatch<SetStateAction<T>> } {
  const [attempt, setAttempt] = useState(0)
  const key = JSON.stringify([...deps, attempt])
  const [result, setResult] = useState<KeyedState<T> | null>(null)

  useEffect(() => {
    let cancelled = false
    load()
      .then((data) => {
        if (!cancelled) setResult({ key, state: { kind: 'success', data } })
      })
      .catch((error: unknown) => {
        if (!cancelled) setResult({ key, state: { kind: 'error', message: getErrorMessage(error) } })
      })
    return () => {
      cancelled = true
    }
    // `load` cambia en cada render; las dependencias reales van en `key`.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [key])

  const reload = useCallback(() => setAttempt((previous) => previous + 1), [])
  const setData = useCallback<Dispatch<SetStateAction<T>>>((value) => {
    setResult((current) => {
      if (!current || current.state.kind !== 'success') return current
      const previous = current.state.data
      const next = typeof value === 'function' ? (value as (old: T) => T)(previous) : value
      return { key: current.key, state: { kind: 'success', data: next } }
    })
  }, [])

  const state: LoadState<T> = result && result.key === key ? result.state : { kind: 'loading' }
  return { state, reload, setData }
}
