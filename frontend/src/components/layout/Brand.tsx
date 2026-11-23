import { Link } from 'react-router'
import { cn } from '../ui/cn'

export function BrandMark({ className = 'h-8 w-8' }: { className?: string }) {
  return (
    <svg viewBox="0 0 32 32" className={className} aria-hidden>
      <rect width="32" height="32" rx="7" fill="#1b3f94" />
      <circle cx="11" cy="12" r="6" fill="#fdd20b" />
      <path d="M17 6h9v20h-9z" fill="#ffffff" />
      <path d="M17 18h9v8h-9z" fill="#d6232b" />
      <path d="M6 21h9v5H6z" fill="#d6232b" />
    </svg>
  )
}

export function Brand({ to = '/', inverted = false }: { to?: string; inverted?: boolean }) {
  return (
    <Link to={to} className="flex items-center gap-2.5" aria-label="KSM Connect – strona główna">
      <BrandMark />
      <span className={cn('font-display text-[17px] leading-none font-extrabold tracking-tight [font-stretch:118%]', inverted ? 'text-white' : 'text-ksm-blue-900')}>
        KSM<span className={inverted ? 'text-ksm-yellow-400' : 'text-ksm-red-600'}> Connect</span>
      </span>
    </Link>
  )
}

export function KsmStripe({ className = 'h-1.5' }: { className?: string }) {
  return (
    <div className={cn('flex w-full', className)} aria-hidden>
      <span className="flex-1 bg-ksm-yellow-400" />
      <span className="flex-1 bg-ksm-red-600" />
      <span className="flex-1 bg-ksm-blue-700" />
    </div>
  )
}
