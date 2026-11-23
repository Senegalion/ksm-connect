import type { ReactNode } from 'react'
import { cn } from './cn'

export type Tone = 'neutral' | 'success' | 'warning' | 'wine' | 'danger'

const TONES: Record<Tone, string> = {
  neutral: 'bg-cream-200 text-ink-700',
  success: 'bg-pine-50 text-pine-700',
  warning: 'bg-gold-100 text-gold-700',
  wine: 'bg-wine-50 text-wine-700',
  danger: 'bg-danger-50 text-danger-700',
}

export function Badge({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span className={cn('inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-semibold', TONES[tone])}>
      {children}
    </span>
  )
}
