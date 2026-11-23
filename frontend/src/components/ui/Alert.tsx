import { AlertCircle, CheckCircle2, Info } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from './cn'

type Tone = 'info' | 'success' | 'error'

const STYLES: Record<Tone, string> = {
  info: 'bg-cream-50 border-cream-300 text-ink-700',
  success: 'bg-pine-50 border-pine-600/20 text-pine-700',
  error: 'bg-danger-50 border-danger-700/20 text-danger-700',
}

const ICONS = { info: Info, success: CheckCircle2, error: AlertCircle }

export function Alert({ tone = 'info', title, children, className }: { tone?: Tone; title?: string; children?: ReactNode; className?: string }) {
  const Icon = ICONS[tone]
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={cn('flex gap-3 rounded-2xl border px-4 py-3 text-sm', STYLES[tone], className)}>
      <Icon className="mt-0.5 size-4 shrink-0" aria-hidden />
      <div className="space-y-0.5">
        {title && <p className="font-semibold">{title}</p>}
        {children && <div className="leading-relaxed">{children}</div>}
      </div>
    </div>
  )
}
