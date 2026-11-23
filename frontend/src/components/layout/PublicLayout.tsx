import type { ReactNode } from 'react'
import { Link } from 'react-router'
import { Brand, KsmStripe } from './Brand'
import { PosterArt } from './PosterArt'

export function PublicLayout({ children, wide = false }: { children: ReactNode; wide?: boolean }) {
  return (
    <div className="flex min-h-dvh flex-col">
      <header>
        <div className="mx-auto flex h-16 w-full max-w-6xl items-center px-4">
          <Brand />
        </div>
        <KsmStripe />
      </header>
      <main className={`mx-auto w-full flex-1 px-4 pt-10 pb-16 ${wide ? 'max-w-5xl' : 'max-w-md'}`}>{children}</main>
      <PublicFooter />
    </div>
  )
}

function PublicFooter() {
  return (
    <footer className="border-t border-line">
      <div className="mx-auto flex max-w-6xl flex-wrap gap-x-6 gap-y-1 px-4 py-5 text-[13px] text-ink-500">
        <span>KSM Connect</span>
        <Link to="/privacy" className="hover:text-ink-900 hover:underline">Informacja o przetwarzaniu danych</Link>
      </div>
    </footer>
  )
}

export function AuthLayout({ title, subtitle, children }: { title: string; subtitle?: ReactNode; children: ReactNode }) {
  return (
    <div className="grid min-h-dvh lg:grid-cols-[minmax(0,1fr)_minmax(0,1.1fr)]">
      <aside className="relative hidden overflow-hidden bg-ksm-blue-800 text-white lg:flex lg:flex-col">
        <div className="px-10 pt-8">
          <Brand inverted />
        </div>
        <div className="relative flex flex-1 items-end">
          <PosterArt className="absolute -right-16 -bottom-10 w-[560px] max-w-none" />
          <p className="display-wide relative z-10 max-w-sm px-10 pb-12 text-4xl leading-[1.02]">
            Prezent dla jednej osoby. <span className="text-ksm-yellow-400">Radość dla wszystkich.</span>
          </p>
        </div>
        <KsmStripe className="h-2" />
      </aside>
      <div className="flex flex-col">
        <header className="lg:hidden">
          <div className="flex h-16 items-center px-4">
            <Brand />
          </div>
          <KsmStripe />
        </header>
        <main className="mx-auto flex w-full max-w-md flex-1 flex-col justify-center px-4 py-10">
          <div className="animate-slide-up space-y-7">
            <div className="space-y-2">
              <h1 className="text-[32px] leading-[1.05]">{title}</h1>
              {subtitle && <p className="leading-relaxed text-ink-500">{subtitle}</p>}
            </div>
            {children}
          </div>
        </main>
        <PublicFooter />
      </div>
    </div>
  )
}

export function AuthCard({ title, subtitle, children }: { title: string; subtitle?: ReactNode; children: ReactNode }) {
  return (
    <div className="space-y-6">
      <div className="space-y-2">
        <h1 className="text-[30px] leading-tight">{title}</h1>
        {subtitle && <p className="leading-relaxed text-ink-500">{subtitle}</p>}
      </div>
      {children}
    </div>
  )
}
