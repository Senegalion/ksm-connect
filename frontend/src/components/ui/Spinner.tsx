import { cn } from './cn'

export function Spinner({ className }: { className?: string }) {
  return (
    <span
      role="status"
      aria-label="Ładowanie"
      className={cn('inline-block size-5 animate-spin rounded-full border-2 border-current border-r-transparent', className)}
    />
  )
}

export function PageLoader({ label = 'Ładowanie…' }: { label?: string }) {
  return (
    <div className="flex min-h-[40vh] flex-col items-center justify-center gap-3 text-ink-500">
      <Spinner className="size-7 text-wine-700" />
      <p className="text-sm">{label}</p>
    </div>
  )
}
