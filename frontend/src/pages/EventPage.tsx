import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Alert } from '../components/ui/Alert'
import { Button, ButtonLink } from '../components/ui/Button'
import { ConfirmDialog } from '../components/ui/ConfirmDialog'
import { PageLoader } from '../components/ui/Spinner'
import { EventHero } from '../features/events/EventHero'
import { WishlistEditor } from '../features/wishlist/WishlistEditor'
import { errorMessage } from '../lib/errors'
import { queryKeys } from '../lib/queryKeys'
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
      <Link to="/dashboard" className="inline-flex min-h-10 items-center gap-1.5 text-sm font-semibold text-ksm-blue-700 hover:underline">
        ← Wydarzenia
      </Link>
      <EventHero event={data} />

      {drawn && (
        <section className="relative flex flex-col gap-4 overflow-hidden rounded-3xl bg-ksm-yellow-400 p-6 text-ksm-blue-900 sm:flex-row sm:items-center sm:justify-between sm:p-8">
          <div className="pointer-events-none absolute -right-6 -bottom-12 size-36 rounded-full bg-ksm-red-600" aria-hidden />
          <div className="relative space-y-1">
            <h2 className="display-wide text-2xl text-ksm-blue-900 sm:text-3xl">Twoja osoba na Wigilię</h2>
            <p className="text-[15px] font-medium">Losowanie zakończone. Twój los czeka na zdrapanie.</p>
          </div>
          <ButtonLink to={`/events/${eventId}/assignment`} size="lg" className="relative">Zdrap swój los</ButtonLink>
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
