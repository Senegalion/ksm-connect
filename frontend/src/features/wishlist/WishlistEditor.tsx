import { useQuery } from '@tanstack/react-query'
import { Alert } from '../../components/ui/Alert'
import { PageLoader } from '../../components/ui/Spinner'
import { errorMessage } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { eventService } from '../../services/eventService'
import { SuggestionCard } from './SuggestionCard'

export function WishlistEditor({ eventId }: { eventId: string }) {
  const wishlist = useQuery({ queryKey: queryKeys.wishlist(eventId), queryFn: () => eventService.wishlist(eventId) })

  if (wishlist.isPending) {
    return <PageLoader label="Wczytujemy listę prezentów…" />
  }
  if (wishlist.isError) {
    return <Alert tone="error">{errorMessage(wishlist.error)}</Alert>
  }
  const { items, editable, filledCount, maxItems } = wishlist.data
  const byPosition = new Map(items.map((item) => [item.position, item]))

  return (
    <section aria-labelledby="wishlist-heading" className="space-y-4">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div className="space-y-1">
          <h2 id="wishlist-heading" className="text-2xl">Moja lista prezentów</h2>
          <p className="text-sm text-ink-500">Dodaj do trzech propozycji – link do produktu albo krótki opis.</p>
        </div>
        <div className="flex items-center gap-2" aria-label={`Uzupełniono ${filledCount} z ${maxItems}`}>
          <span className="font-mono text-sm text-ink-700">{filledCount}/{maxItems}</span>
          <span className="text-sm text-ink-500">uzupełnione</span>
        </div>
      </div>
      {!editable && (
        <Alert tone="info">
          Lista jest zamknięta. Nie można już wprowadzać zmian.
        </Alert>
      )}
      <div className="grid gap-8 md:grid-cols-3 md:gap-6">
        {Array.from({ length: maxItems }, (_, index) => index + 1).map((position) => (
          <SuggestionCard key={position} eventId={eventId} position={position} item={byPosition.get(position)} editable={editable} />
        ))}
      </div>
    </section>
  )
}
