import { useQuery } from '@tanstack/react-query'
import { useState } from 'react'
import { KsmStripe } from '../../components/layout/Brand'
import { Alert } from '../../components/ui/Alert'
import { Button } from '../../components/ui/Button'
import { Confetti } from '../../components/ui/Confetti'
import { EmptyState } from '../../components/ui/EmptyState'
import { PageLoader } from '../../components/ui/Spinner'
import { errorMessage } from '../../lib/errors'
import { queryKeys } from '../../lib/queryKeys'
import { formatDateTime } from '../../lib/time'
import { eventService } from '../../services/eventService'
import { SuggestionDisplay } from '../wishlist/SuggestionDisplay'
import { ScratchCard } from './ScratchCard'

export function AssignmentReveal({ eventId, eventName, timezone }: { eventId: string; eventName: string; timezone: string }) {
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
      <div className="space-y-2">
        <p className="text-sm font-semibold text-ksm-red-600">{eventName}</p>
        <h1 id="assignment-heading" className="display-wide text-[34px] leading-[1] sm:text-5xl">Twoja osoba na Wigilię</h1>
      </div>

      <div className="relative">
        {revealed && <Confetti />}
        <ScratchCard revealed={revealed} onReveal={() => setRevealed(true)}>
          <div className="ticket-edges overflow-hidden rounded-2xl bg-ksm-blue-800 text-white select-none">
            <div className="flex items-stretch">
              <div className="flex w-14 shrink-0 items-center justify-center border-r-2 border-dashed border-white/25 bg-ksm-yellow-400 sm:w-20">
                <span className="display-wide -rotate-90 text-lg tracking-[0.3em] text-ksm-blue-900 sm:text-2xl">LOS</span>
              </div>
              <div className="min-w-0 flex-1 px-6 py-8 sm:px-10 sm:py-12">
                <p className="text-sm font-semibold text-ksm-yellow-300">Przygotowujesz prezent dla</p>
                <p
                  className={`display-wide mt-3 text-[40px] leading-[0.95] break-words text-white sm:text-6xl ${revealed ? 'animate-pop-in' : ''}`}
                  data-testid="recipient-name"
                >
                  {recipient.firstName}{' '}
                  <br />
                  <span className="text-white/75">{recipient.lastName}</span>
                </p>
              </div>
            </div>
            <KsmStripe className="h-2" />
          </div>
        </ScratchCard>
      </div>

      {!revealed && (
        <div className="flex flex-col items-start gap-1">
          <Button variant="secondary" onClick={() => setRevealed(true)}>Dotknij, aby odsłonić</Button>
          <p className="text-[13px] text-ink-500">Albo zdrap los palcem. Nikt poza Tobą nie widzi tego wyniku.</p>
        </div>
      )}

      {revealed && (
        <div className="animate-slide-up space-y-5" style={{ animationDelay: '250ms' }}>
          <div className="flex items-end justify-between gap-4 border-b-2 border-ksm-blue-800 pb-3">
            <h2 className="text-2xl">Lista prezentów</h2>
            <span className="font-mono text-sm text-ink-500">{recipient.wishlist.length}/3</span>
          </div>
          {recipient.wishlist.length === 0 ? (
            <EmptyState
              title={`${recipient.firstName} nie dodał(-a) jeszcze propozycji`}
              description="Zajrzyj tu później, lista może się jeszcze pojawić. Albo wymyśl coś od siebie."
            />
          ) : (
            <ol className="space-y-3">
              {recipient.wishlist.map((item, index) => (
                <li key={item.position} className="flex gap-4 rounded-2xl bg-muted p-4">
                  <span className={`display-wide flex size-10 shrink-0 items-center justify-center rounded-lg text-lg ${['bg-ksm-yellow-400 text-ksm-blue-900', 'bg-ksm-red-600 text-white', 'bg-ksm-blue-700 text-white'][index % 3]}`} aria-hidden>
                    {item.position}
                  </span>
                  <div className="min-w-0 flex-1 pt-1.5"><SuggestionDisplay item={item} /></div>
                </li>
              ))}
            </ol>
          )}
          <p className="rounded-2xl border-2 border-dashed border-ksm-yellow-400 px-5 py-4 text-[15px] leading-relaxed text-ink-700">
            To tylko propozycje, a nie obowiązkowa lista zakupów. Najważniejsza jest pamięć i radość z obdarowywania. ❤️
          </p>
        </div>
      )}
    </section>
  )
}
