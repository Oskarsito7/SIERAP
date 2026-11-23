import type { AxiosResponse } from 'axios'

import type { Page } from '@/types/api'

/** Tamaño de página de las listas con paginación del servidor (el backend admite de 1 a 200). */
export const PAGE_SIZE = 50

/** Cabecera con el total de elementos sin paginar (ADR-024). */
const TOTAL_COUNT_HEADER = 'x-total-count'

/**
 * Arma la página a partir de la respuesta del backend: el cuerpo son los elementos y
 * `X-Total-Count` el total. Si la cabecera falta o no es un número, se usa la cantidad
 * de elementos recibidos.
 */
export function toPage<T>(response: AxiosResponse<T[]>): Page<T> {
  const header = response.headers?.[TOTAL_COUNT_HEADER] as unknown
  const total = Number(header)
  return {
    items: response.data,
    total: header !== undefined && header !== null && header !== '' && Number.isInteger(total) && total >= 0
      ? total
      : response.data.length,
  }
}
