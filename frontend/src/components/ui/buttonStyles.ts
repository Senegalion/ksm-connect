import { cn } from './cn'

export type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'pine'
export type Size = 'md' | 'sm' | 'lg'

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-ink-900 text-white hover:bg-ink-700',
  pine: 'bg-ink-900 text-white hover:bg-ink-700',
  secondary: 'bg-paper text-ink-900 border border-line-strong hover:border-ink-900',
  ghost: 'px-0! text-ink-700 underline-offset-4 hover:underline',
  danger: 'bg-danger-700 text-white hover:bg-accent-800',
}

const SIZES: Record<Size, string> = {
  sm: 'min-h-10 px-3 text-sm gap-1.5',
  md: 'min-h-12 px-5 text-[15px] gap-2',
  lg: 'min-h-13 px-6 text-base gap-2',
}

export function buttonClasses(variant: Variant = 'primary', size: Size = 'md', fullWidth = false) {
  return cn(
    'inline-flex items-center justify-center rounded-md font-medium transition-colors duration-100 select-none',
    'disabled:opacity-45 disabled:cursor-not-allowed',
    VARIANTS[variant],
    SIZES[size],
    fullWidth && 'w-full',
  )
}
