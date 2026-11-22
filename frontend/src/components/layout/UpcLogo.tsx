import logoColor from '@/assets/brand/upc-logo-horizontal.svg'
import logoWhite from '@/assets/brand/upc-logo-horizontal-blanco.svg'
import { cn } from '@/lib/utils'

/**
 * Logosímbolo horizontal de la Universidad Popular del Cesar, tomado en vector del
 * Manual de Identidad Institucional 2024. A color sobre fondos claros y en blanco sobre
 * los verdes institucionales, como permite el manual ("Aplicaciones de color"); nunca se
 * deforma: solo se fija su alto.
 */
export function UpcLogo({ variant = 'color', className }: { variant?: 'color' | 'white'; className?: string }) {
  return (
    <img
      src={variant === 'white' ? logoWhite : logoColor}
      alt="Universidad Popular del Cesar"
      className={cn('h-10 w-auto', className)}
    />
  )
}
