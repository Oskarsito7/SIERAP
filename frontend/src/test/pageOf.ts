import type { Page } from '@/types/api'

/** Página de prueba: los elementos dados y, si no se indica, su cantidad como total. */
export function pageOf<T>(items: T[], total: number = items.length): Page<T> {
  return { items, total }
}
