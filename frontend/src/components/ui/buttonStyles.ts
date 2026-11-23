import { cn } from './cn'

export type Variant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'pine' | 'yellow' | 'onDark'
export type Size = 'md' | 'sm' | 'lg'

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-ksm-blue-700 text-white hover:bg-ksm-blue-800 active:translate-y-px',
  pine: 'bg-ksm-blue-700 text-white hover:bg-ksm-blue-800 active:translate-y-px',
  yellow: 'bg-ksm-yellow-400 text-ksm-blue-900 hover:bg-ksm-yellow-300 active:translate-y-px',
  onDark: 'border-2 border-white/70 text-white hover:border-white hover:bg-white/10',
  secondary: 'bg-paper text-ksm-blue-800 border-2 border-ksm-blue-100 hover:border-ksm-blue-700',
  ghost: 'px-0! text-ink-700 underline-offset-4 hover:underline',
  danger: 'bg-danger-700 text-white hover:bg-ksm-red-600',
}

const SIZES: Record<Size, string> = {
  sm: 'min-h-10 px-3.5 text-sm gap-1.5',
  md: 'min-h-12 px-5 text-[15px] gap-2',
  lg: 'min-h-14 px-7 text-base gap-2',
}

export function buttonClasses(variant: Variant = 'primary', size: Size = 'md', fullWidth = false) {
  return cn(
    'inline-flex items-center justify-center rounded-lg font-semibold transition duration-150 select-none',
    'disabled:opacity-45 disabled:cursor-not-allowed disabled:translate-y-0',
    VARIANTS[variant],
    SIZES[size],
    fullWidth && 'w-full',
  )
}
