import { Link } from 'react-router'

export function BrandMark({ className = '' }: { className?: string }) {
  return (
    <span className={`inline-flex h-7 items-center bg-accent-700 px-1.5 text-[13px] font-bold tracking-wide text-white ${className}`} aria-hidden>
      KSM
    </span>
  )
}

export function Brand({ to = '/' }: { to?: string }) {
  return (
    <Link to={to} className="flex items-center gap-2" aria-label="KSM Connect – strona główna">
      <BrandMark />
      <span className="text-[17px] font-semibold tracking-tight text-ink-900">Connect</span>
    </Link>
  )
}
