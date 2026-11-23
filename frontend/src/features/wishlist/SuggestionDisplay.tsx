import { displayHost, safeExternalUrl } from '../../lib/safeUrl'
import type { WishlistItem } from '../../types/api'

export function SuggestionDisplay({ item }: { item: WishlistItem }) {
  if (item.type === 'URL') {
    const href = safeExternalUrl(item.url)
    if (!href) {
      return <p className="text-sm text-ink-500">Nieprawidłowy link</p>
    }
    return (
      <a href={href} target="_blank" rel="noopener noreferrer nofollow" className="group block min-h-11">
        <span className="block font-medium text-ksm-blue-700 group-hover:underline">{displayHost(href)} ↗</span>
        <span className="block truncate text-[13px] text-ink-500">{href}</span>
      </a>
    )
  }
  return <p className="leading-relaxed break-words whitespace-pre-line text-ink-900">{item.description}</p>
}
