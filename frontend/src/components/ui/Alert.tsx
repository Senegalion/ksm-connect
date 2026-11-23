import type { ReactNode } from 'react'
import { cn } from './cn'

type Tone = 'info' | 'success' | 'error'

const STYLES: Record<Tone, string> = {
  info: 'border-ink-300 bg-muted text-ink-700',
  success: 'border-success-700 bg-success-50 text-success-700',
  error: 'border-danger-700 bg-danger-50 text-danger-700',
}

export function Alert({ tone = 'info', title, children, className }: { tone?: Tone; title?: string; children?: ReactNode; className?: string }) {
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={cn('border-l-[3px] px-4 py-3 text-sm leading-relaxed', STYLES[tone], className)}>
      {title && <p className="font-semibold">{title}</p>}
      {children && <div>{children}</div>}
    </div>
  )
}
