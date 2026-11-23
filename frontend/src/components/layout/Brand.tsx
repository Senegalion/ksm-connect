import { Link } from 'react-router'

export function BrandMark({ className = 'size-9' }: { className?: string }) {
  return (
    <svg viewBox="0 0 64 64" className={className} aria-hidden>
      <rect width="64" height="64" rx="16" fill="#7a1f2b" />
      <path d="M14 30h36v22a4 4 0 0 1-4 4H18a4 4 0 0 1-4-4z" fill="#fdf6ec" />
      <rect x="10" y="22" width="44" height="10" rx="3" fill="#fdf6ec" />
      <path d="M29 22h6v34h-6z" fill="#c9963a" />
      <path d="M32 22c-6-10-16-8-13-2 2 3 8 3 13 2zm0 0c6-10 16-8 13-2-2 3-8 3-13 2z" fill="#c9963a" />
    </svg>
  )
}

export function Brand({ to = '/' }: { to?: string }) {
  return (
    <Link to={to} className="flex items-center gap-2.5 rounded-xl" aria-label="KSM Connect – strona główna">
      <BrandMark />
      <span className="font-display text-lg font-semibold tracking-tight text-ink-900">
        KSM <span className="text-wine-700">Connect</span>
      </span>
    </Link>
  )
}
