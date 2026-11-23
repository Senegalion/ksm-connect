import type { ReactNode } from 'react'
import { cn } from './cn'

export type Tone = 'neutral' | 'success' | 'warning' | 'wine' | 'danger'

const DOTS: Record<Tone, string> = {
  neutral: 'bg-ink-300',
  success: 'bg-success-700',
  warning: 'bg-warning-700',
  wine: 'bg-accent-600',
  danger: 'bg-danger-700',
}

export function Badge({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span className="inline-flex items-center gap-1.5 text-[13px] font-medium text-ink-700">
      <span className={cn('size-2 rounded-full', DOTS[tone])} aria-hidden />
      {children}
    </span>
  )
}
