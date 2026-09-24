import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

/** Merges Tailwind classes resolving conflicts (standard shadcn/ui utility). */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
