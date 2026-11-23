import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { Brand } from './Brand'

export function PublicLayout({ children, wide = false }: { children: ReactNode; wide?: boolean }) {
  return (
    <div className="flex min-h-dvh flex-col">
      <header className="mx-auto flex h-16 w-full max-w-5xl items-center px-4">
        <Brand />
      </header>
      <main className={`mx-auto w-full flex-1 px-4 pt-4 pb-12 ${wide ? 'max-w-5xl' : 'max-w-md'}`}>{children}</main>
      <footer className="mx-auto w-full max-w-5xl px-4 pb-8 text-center text-xs text-ink-500">
        <Link to="/privacy" className="underline-offset-4 hover:underline">Informacja o przetwarzaniu danych</Link>
        <span className="mx-2">·</span>
        <span>KSM Connect</span>
      </footer>
    </div>
  )
}

export function AuthCard({ title, subtitle, children }: { title: string; subtitle?: ReactNode; children: ReactNode }) {
  return (
    <div className="animate-rise space-y-6">
      <div className="space-y-2">
        <h1 className="text-3xl font-semibold">{title}</h1>
        {subtitle && <p className="leading-relaxed text-ink-500">{subtitle}</p>}
      </div>
      <div className="rounded-[var(--radius-card)] border border-cream-200 bg-white p-5 shadow-[var(--shadow-card)] sm:p-6">
        {children}
      </div>
    </div>
  )
}
