import { useQuery } from '@tanstack/react-query'
import { Alert } from '../components/ui/Alert'
import { ButtonLink } from '../components/ui/Button'
import { EmptyState } from '../components/ui/EmptyState'
import { PageLoader } from '../components/ui/Spinner'
import { EventCard } from '../features/events/EventCard'
import { useSession } from '../hooks/useSession'
import { errorMessage } from '../lib/errors'
import { queryKeys } from '../lib/queryKeys'
import { eventService } from '../services/eventService'

export function DashboardPage() {
  const { user } = useSession()
  const events = useQuery({ queryKey: queryKeys.myEvents, queryFn: eventService.myEvents })

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-[15px] font-semibold text-ksm-red-600">Cześć, {user?.firstName}!</p>
          <h1 className="display-wide text-4xl leading-[1] sm:text-5xl">Twoje wydarzenia</h1>
        </div>
        <ButtonLink to="/join" variant="secondary">Dołącz kodem</ButtonLink>
      </div>
      {events.isPending ? (
        <PageLoader />
      ) : events.isError ? (
        <Alert tone="error">{errorMessage(events.error)}</Alert>
      ) : events.data.length === 0 ? (
        <EmptyState
          title="Nie masz jeszcze wydarzeń"
          description="Poproś organizatora o kod zaproszenia albo zeskanuj kod QR aparatem w telefonie."
          action={<ButtonLink to="/join">Wpisz kod zaproszenia</ButtonLink>}
        />
      ) : (
        <div className="grid gap-5 md:grid-cols-2">
          {events.data.map((event) => <EventCard key={event.id} event={event} />)}
        </div>
      )}
    </div>
  )
}
