import { cn } from '../ui/cn'

export function PosterArt({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 480 480" className={cn('select-none', className)} aria-hidden>
      <g className="animate-pop-in" style={{ animationDelay: '80ms', transformBox: 'fill-box', transformOrigin: 'center' }}>
        <circle cx="300" cy="190" r="150" fill="#fdd20b" />
      </g>
      <g className="animate-pop-in" style={{ animationDelay: '220ms', transformBox: 'fill-box', transformOrigin: 'center' }}>
        <path d="M60 470 A190 190 0 0 1 250 280 L250 470 Z" fill="#d6232b" />
      </g>
      <g className="animate-pop-in" style={{ animationDelay: '360ms', transformBox: 'fill-box', transformOrigin: 'center' }}>
        <rect x="268" y="300" width="56" height="170" fill="#ffffff" />
        <rect x="342" y="300" width="56" height="170" fill="#ffffff" opacity="0.9" />
        <rect x="416" y="300" width="56" height="170" fill="#ffffff" opacity="0.75" />
      </g>
      <g className="animate-float" style={{ transformBox: 'fill-box', transformOrigin: 'center' }}>
        <rect x="70" y="80" width="80" height="80" fill="#2453b8" stroke="#ffffff" strokeWidth="4" transform="rotate(12 110 120)" />
      </g>
      <g className="animate-pop-in" style={{ animationDelay: '480ms', transformBox: 'fill-box', transformOrigin: 'center' }}>
        <rect x="230" y="120" width="140" height="140" rx="6" fill="#1b3f94" transform="rotate(-8 300 190)" />
        <rect x="292" y="120" width="16" height="140" fill="#fdd20b" transform="rotate(-8 300 190)" />
        <rect x="230" y="182" width="140" height="16" fill="#fdd20b" transform="rotate(-8 300 190)" />
        <path d="M300 122 c-26 -36 -66 -24 -52 -2 c8 12 34 10 52 2 z M300 122 c26 -36 66 -24 52 -2 c-8 12 -34 10 -52 2 z" fill="#d6232b" transform="rotate(-8 300 190)" />
      </g>
      <circle cx="430" cy="60" r="22" fill="#ffffff" className="animate-float" style={{ animationDelay: '1.5s', transformBox: 'fill-box', transformOrigin: 'center' }} />
    </svg>
  )
}
