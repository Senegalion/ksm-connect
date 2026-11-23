import type { ReactNode } from 'react'

interface EmptyStateProps {
  icon?: ReactNode
  title: string
  description?: string
  action?: ReactNode
}

export function EmptyState({ title, description, action }: EmptyStateProps) {
  return (
    <div className="space-y-3 border-y border-line py-8">
      <h3 className="text-lg">{title}</h3>
      {description && <p className="max-w-prose text-[15px] leading-relaxed text-ink-500">{description}</p>}
      {action && <div className="pt-1">{action}</div>}
    </div>
  )
}
