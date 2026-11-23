import { useQuery } from '@tanstack/react-query'
import { ArrowLeft } from 'lucide-react'
import { Link, useParams } from 'react-router'
import { Alert } from '../components/ui/Alert'
import { PageLoader } from '../components/ui/Spinner'
import { AssignmentReveal } from '../features/draw/AssignmentReveal'
import { errorMessage } from '../lib/errors'
import { queryKeys } from '../lib/queryKeys'
import { eventService } from '../services/eventService'

export function AssignmentPage() {
  const { eventId = '' } = useParams()
  const event = useQuery({ queryKey: queryKeys.myEvent(eventId), queryFn: () => eventService.myEvent(eventId) })
  if (event.isPending) return <PageLoader />
  if (event.isError) return <Alert tone="error">{errorMessage(event.error)}</Alert>
  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <Link to={`/events/${eventId}`} className="inline-flex items-center gap-1.5 text-sm font-semibold text-ink-500 hover:text-wine-700">
        <ArrowLeft className="size-4" aria-hidden />{event.data.name}
      </Link>
      <AssignmentReveal eventId={eventId} timezone={event.data.timezone} />
    </div>
  )
}
