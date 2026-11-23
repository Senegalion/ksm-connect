import { cn } from './cn'

export type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'pine'
export type Size = 'md' | 'sm' | 'lg'

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-wine-700 text-white hover:bg-wine-800 active:bg-wine-800 shadow-sm',
  pine: 'bg-pine-700 text-white hover:bg-pine-600 shadow-sm',
  secondary: 'bg-white text-ink-900 border border-cream-300 hover:bg-cream-50',
  ghost: 'text-ink-700 hover:bg-cream-200/60',
  danger: 'bg-danger-700 text-white hover:brightness-95',
}

const SIZES: Record<Size, string> = {
  sm: 'min-h-10 px-3.5 text-sm rounded-xl gap-1.5',
  md: 'min-h-12 px-5 text-[15px] rounded-xl gap-2',
  lg: 'min-h-14 px-6 text-base rounded-2xl gap-2.5',
}

export function buttonClasses(variant: Variant = 'primary', size: Size = 'md', fullWidth = false) {
  return cn(
    'inline-flex items-center justify-center font-semibold transition-colors duration-150 select-none',
    'disabled:opacity-55 disabled:cursor-not-allowed',
    VARIANTS[variant],
    SIZES[size],
    fullWidth && 'w-full',
  )
}
