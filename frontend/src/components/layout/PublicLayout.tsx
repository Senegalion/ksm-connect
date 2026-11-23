import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { Brand } from './Brand'

export function PublicLayout({ children, wide = false }: { children: ReactNode; wide?: boolean }) {
  return (
    <div className="flex min-h-dvh flex-col">
      <header className="border-b border-line">
        <div className="mx-auto flex h-14 w-full max-w-5xl items-center px-4">
          <Brand />
        </div>
      </header>
      <main className={`mx-auto w-full flex-1 px-4 pt-10 pb-16 ${wide ? 'max-w-5xl' : 'max-w-md'}`}>{children}</main>
      <footer className="border-t border-line">
        <div className="mx-auto flex max-w-5xl flex-wrap gap-x-6 gap-y-1 px-4 py-5 text-[13px] text-ink-500">
          <span>KSM Connect</span>
          <Link to="/privacy" className="hover:text-ink-900 hover:underline">Informacja o przetwarzaniu danych</Link>
        </div>
      </footer>
    </div>
  )
}

export function AuthCard({ title, subtitle, children }: { title: string; subtitle?: ReactNode; children: ReactNode }) {
  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <h1 className="text-[28px] leading-tight">{title}</h1>
        {subtitle && <p className="leading-relaxed text-ink-500">{subtitle}</p>}
      </div>
      {children}
    </div>
  )
}
