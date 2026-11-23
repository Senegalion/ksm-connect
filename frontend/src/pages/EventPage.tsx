import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Alert } from '../components/ui/Alert'
import { Badge } from '../components/ui/Badge'
import { Button, ButtonLink } from '../components/ui/Button'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { PageLoader } from '../components/ui/Spinner'
import { EventMeta } from '../features/events/EventMeta'
import { WishlistEditor } from '../features/wishlist/WishlistEditor'
import { errorMessage } from '../lib/errors'
import { queryKeys } from '../lib/queryKeys'
import { EVENT_STATUS_LABELS, EVENT_STATUS_TONES } from '../lib/statusLabels'
import { eventService } from '../services/eventService'

export function EventPage() {
  const { eventId = '' } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [confirmWithdraw, setConfirmWithdraw] = useState(false)
  const event = useQuery({ queryKey: queryKeys.myEvent(eventId), queryFn: () => eventService.myEvent(eventId) })
  const withdraw = useMutation({
    mutationFn: () => eventService.withdraw(eventId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.myEvents })
      navigate('/dashboard', { replace: true })
    },
  })

  if (event.isPending) return <PageLoader />
  if (event.isError) {
    return (
      <div className="space-y-4">
        <Alert tone="error">{errorMessage(event.error)}</Alert>
        <ButtonLink to="/dashboard" variant="secondary">Wróć do wydarzeń</ButtonLink>
      </div>
    )
  }
  const data = event.data
  const drawn = data.status === 'DRAW_COMPLETED'

  return (
    <div className="space-y-8">
      <Link to="/dashboard" className="inline-flex min-h-10 items-center gap-1.5 text-sm text-ink-500 hover:text-ink-900">
        ← Wydarzenia
      </Link>
      <header className="space-y-4">
        <Badge tone={EVENT_STATUS_TONES[data.status]}>{EVENT_STATUS_LABELS[data.status]}</Badge>
        <h1 className="text-3xl leading-tight sm:text-4xl">{data.name}</h1>
        {data.description && <p className="max-w-2xl leading-relaxed whitespace-pre-line text-ink-700">{data.description}</p>}
        <EventMeta event={data} />
      </header>

      {drawn && (
        <section className="flex flex-col gap-4 border-t-4 border-accent-700 pt-5 sm:flex-row sm:items-end sm:justify-between">
          <div className="space-y-1">
            <h2 className="text-xl">Twoja osoba na Wigilię</h2>
            <p className="text-[15px] text-ink-500">Losowanie zakończone. Zobacz, komu przygotujesz prezent.</p>
          </div>
          <ButtonLink to={`/events/${eventId}/assignment`} size="lg">Zobacz</ButtonLink>
        </section>
      )}

      <WishlistEditor eventId={eventId} />

      {data.status === 'REGISTRATION_OPEN' && (
        <div className="border-t border-line pt-6">
          <Button variant="ghost" onClick={() => setConfirmWithdraw(true)}>Zrezygnuj z udziału</Button>
          {withdraw.isError && <Alert tone="error" className="mt-3">{errorMessage(withdraw.error)}</Alert>}
        </div>
      )}
      <ConfirmDialog
        open={confirmWithdraw}
        title="Zrezygnować z udziału?"
        tone="danger"
        confirmLabel="Rezygnuję"
        loading={withdraw.isPending}
        onConfirm={() => withdraw.mutate()}
        onClose={() => setConfirmWithdraw(false)}
      >
        <p>Nie weźmiesz udziału w losowaniu. Możesz wrócić, wpisując kod zaproszenia ponownie, dopóki zapisy są otwarte.</p>
      </ConfirmDialog>
    </div>
  )
}
