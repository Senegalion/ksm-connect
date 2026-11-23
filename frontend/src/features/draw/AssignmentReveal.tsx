import { useQuery } from '@tanstack/react-query'
import { Eye, Gift, Hourglass, Sparkles } from 'lucide-react'
import { useState } from 'react'
import { Alert } from '../../components/ui/Alert'
import { Card, CardBody } from '../../components/ui/Card'
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
        icon={<Hourglass className="size-6" aria-hidden />}
        title="Losowanie jeszcze się nie odbyło"
        description={drawAt ? `Zaplanowano je na ${formatDateTime(drawAt, timezone)}. Gdy się zakończy, dostaniesz e-mail i SMS.` : 'Gdy organizator przeprowadzi losowanie, dostaniesz e-mail i SMS.'}
      />
    )
  }

  return (
    <section aria-labelledby="assignment-heading" className="space-y-4">
      <h2 id="assignment-heading" className="text-2xl font-semibold">Twoja osoba na Wigilię</h2>
      <Card className="overflow-hidden">
        <div className="relative bg-gradient-to-br from-wine-700 via-wine-700 to-wine-800 px-6 py-8 text-center text-white">
          <Sparkles className="absolute top-4 left-4 size-5 text-gold-500/70" aria-hidden />
          <Sparkles className="absolute right-5 bottom-4 size-4 text-gold-500/60" aria-hidden />
          <p className="text-sm text-white/80">Przygotowujesz prezent dla</p>
          {revealed ? (
            <p className="mt-2 animate-reveal font-display text-3xl font-semibold sm:text-4xl" data-testid="recipient-name">
              {recipient.firstName} {recipient.lastName}
            </p>
          ) : (
            <button
              type="button"
              onClick={() => setRevealed(true)}
              className="mx-auto mt-3 flex min-h-12 items-center gap-2 rounded-2xl bg-white/15 px-5 font-semibold ring-1 ring-white/30 backdrop-blur transition hover:bg-white/25"
            >
              <Eye className="size-5" aria-hidden />
              Dotknij, aby odsłonić
            </button>
          )}
        </div>
        {revealed && (
          <CardBody className="animate-rise space-y-4">
            <div className="flex items-center gap-2">
              <Gift className="size-5 text-wine-700" aria-hidden />
              <h3 className="text-lg font-semibold">Lista prezentów</h3>
            </div>
            {recipient.wishlist.length === 0 ? (
              <EmptyState
                icon={<Gift className="size-6" aria-hidden />}
                title={`${recipient.firstName} nie dodał(-a) jeszcze propozycji`}
                description="Zajrzyj tu później – lista może się jeszcze pojawić. A może to dobra okazja, żeby wymyślić coś własnego?"
              />
            ) : (
              <ul className="space-y-2">
                {recipient.wishlist.map((item) => (
                  <li key={item.position}><SuggestionDisplay item={item} /></li>
                ))}
              </ul>
            )}
            <p className="rounded-xl bg-gold-100/70 px-4 py-3 text-sm leading-relaxed text-gold-700">
              Pamiętaj, że to tylko propozycje, a nie obowiązkowa lista zakupów – najważniejsza jest pamięć i radość z obdarowywania. ❤️
            </p>
          </CardBody>
        )}
      </Card>
    </section>
  )
}
