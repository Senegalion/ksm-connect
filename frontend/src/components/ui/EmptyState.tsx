import type { ReactNode } from 'react'

export function EmptyState({ icon, title, description, action }: { icon: ReactNode; title: string; description?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-[var(--radius-card)] border border-dashed border-cream-300 bg-cream-50/70 px-6 py-10 text-center">
      <div className="flex size-12 items-center justify-center rounded-2xl bg-white text-wine-700 shadow-sm">{icon}</div>
      <h3 className="text-lg font-semibold">{title}</h3>
      {description && <p className="max-w-sm text-sm leading-relaxed text-ink-500">{description}</p>}
      {action}
    </div>
  )
}
