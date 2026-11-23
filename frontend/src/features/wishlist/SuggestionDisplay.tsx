import { ExternalLink, PenLine } from 'lucide-react'
import { displayHost, safeExternalUrl } from '../../lib/safeUrl'
import type { WishlistItem } from '../../types/api'

export function SuggestionDisplay({ item }: { item: WishlistItem }) {
  if (item.type === 'URL') {
    const href = safeExternalUrl(item.url)
    if (!href) {
      return <p className="text-sm text-ink-500">Nieprawidłowy link</p>
    }
    return (
      <a
        href={href}
        target="_blank"
        rel="noopener noreferrer nofollow"
        className="group flex min-h-12 items-center gap-3 rounded-xl border border-cream-200 bg-cream-50 px-3.5 py-2.5 transition hover:border-wine-600/40 hover:bg-wine-50"
      >
        <ExternalLink className="size-4 shrink-0 text-wine-700" aria-hidden />
        <span className="min-w-0 flex-1">
          <span className="block text-sm font-semibold text-ink-900">{displayHost(href)}</span>
          <span className="block truncate text-xs text-ink-500">{href}</span>
        </span>
      </a>
    )
  }
  return (
    <div className="flex gap-3 rounded-xl border border-cream-200 bg-cream-50 px-3.5 py-3">
      <PenLine className="mt-0.5 size-4 shrink-0 text-wine-700" aria-hidden />
      <p className="text-sm leading-relaxed break-words whitespace-pre-line text-ink-900">{item.description}</p>
    </div>
  )
}
