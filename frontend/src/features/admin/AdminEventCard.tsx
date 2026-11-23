import { Link } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { EVENT_STATUS_LABELS, EVENT_STATUS_TONES } from '../../lib/statusLabels'
import { formatDateTime } from '../../lib/time'
import type { AdminEventView } from '../../types/api'

export function AdminEventCard({ event }: { event: AdminEventView }) {
  return (
    <Link to={`/admin/events/${event.id}`} className="group block border-t-2 border-ink-900 pt-4">
      <Badge tone={EVENT_STATUS_TONES[event.status]}>{EVENT_STATUS_LABELS[event.status]}</Badge>
      <h2 className="mt-2 text-lg leading-snug group-hover:underline">{event.name}</h2>
      <p className="mt-2 text-[15px] text-ink-500">
        <span className="font-mono text-ink-900">{event.activeParticipants}</span> zapisanych
        {event.startsAt && <> · {formatDateTime(event.startsAt, event.timezone)}</>}
      </p>
      {(event.drawOverdue || event.notifications.FAILED > 0) && (
        <p className="mt-2 text-[15px] font-medium text-danger-700">
          {event.drawOverdue ? 'Losowanie automatyczne wymaga uwagi' : `Nieudane powiadomienia: ${event.notifications.FAILED}`}
        </p>
      )}
    </Link>
  )
}
