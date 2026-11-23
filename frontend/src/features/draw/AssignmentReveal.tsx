import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { EmptyState } from '../../components/ui/EmptyState'
import { PageLoader } from '../../components/ui/Spinner'
import { errorMessage } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { formatDateTime } from '../../lib/time'
import { eventService } from '../../services/eventService'
import { SuggestionDisplay } from '../wishlist/SuggestionDisplay'

export function AssignmentReveal({ eventId, timezone }: { eventId: string; timezone: string }) {
  const [revealed, setRevealed] = useState(false)
  const assignment = useQuery({
    queryKey: queryKeys.assignment(eventId),
    queryFn: () => eventService.assignment(eventId),
    gcTime: 0,
  })

  if (assignment.isPending) {
    return <PageLoader />
  }
  if (assignment.isError) {
    return <Alert tone="error">{errorMessage(assignment.error)}</Alert>
  }
  const { drawCompleted, recipient, drawAt } = assignment.data

  if (!drawCompleted || !recipient) {
    return (
      <EmptyState
        title="Losowanie jeszcze się nie odbyło"
        description={drawAt ? `Zaplanowano je na ${formatDateTime(drawAt, timezone)}. Gdy się zakończy, dostaniesz e-mail i SMS.` : 'Gdy organizator przeprowadzi losowanie, dostaniesz e-mail i SMS.'}
      />
    )
  }

  return (
    <section aria-labelledby="assignment-heading" className="space-y-8">
      <div className="space-y-4 border-t-4 border-accent-700 pt-6">
        <h2 id="assignment-heading" className="text-sm font-medium tracking-wide text-ink-500 uppercase">Twoja osoba na Wigilię</h2>
        <p className="text-ink-700">Przygotowujesz prezent dla</p>
        {revealed ? (
          <p className="text-4xl leading-tight font-semibold sm:text-5xl" data-testid="recipient-name">
            {recipient.firstName} {recipient.lastName}
          </p>
        ) : (
          <div className="space-y-2">
            <Button variant="secondary" size="lg" onClick={() => setRevealed(true)}>Dotknij, aby odsłonić</Button>
            <p className="text-sm text-ink-500">Imię jest ukryte, żeby nikt nie zobaczył go przez ramię.</p>
          </div>
        )}
      </div>

      {revealed && (
        <div className="space-y-4">
          <h3 className="text-lg">Lista prezentów</h3>
          {recipient.wishlist.length === 0 ? (
            <EmptyState
              title={`${recipient.firstName} nie dodał(-a) jeszcze propozycji`}
              description="Zajrzyj tu później, lista może się jeszcze pojawić. Albo wymyśl coś od siebie."
            />
          ) : (
            <ul className="divide-y divide-line border-y border-line">
              {recipient.wishlist.map((item) => (
                <li key={item.position} className="py-3"><SuggestionDisplay item={item} /></li>
              ))}
            </ul>
          )}
          <p className="text-[15px] leading-relaxed text-ink-500">
            To tylko propozycje, a nie obowiązkowa lista zakupów. Najważniejsza jest pamięć i radość z obdarowywania.
          </p>
        </div>
      )}
    </section>
  )
}
