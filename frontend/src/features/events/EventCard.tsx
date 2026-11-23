import { Link } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { EVENT_STATUS_LABELS, EVENT_STATUS_TONES } from '../../lib/statusLabels'
import type { ParticipantEventView } from '../../types/api'
import { EventMeta } from './EventMeta'

export function EventCard({ event }: { event: ParticipantEventView }) {
  const drawn = event.status === 'DRAW_COMPLETED'
  return (
    <Link to={`/events/${event.id}`} className="group block border-t-2 border-ink-900 pt-4">
      <Badge tone={EVENT_STATUS_TONES[event.status]}>{EVENT_STATUS_LABELS[event.status]}</Badge>
      <h2 className="mt-2 text-xl leading-snug group-hover:underline">{event.name}</h2>
      <div className="mt-4">
        <EventMeta event={event} />
      </div>
      {drawn && <p className="mt-4 text-[15px] font-medium text-accent-700">Losowanie zakończone. Zobacz, komu robisz prezent →</p>}
    </Link>
  )
}
