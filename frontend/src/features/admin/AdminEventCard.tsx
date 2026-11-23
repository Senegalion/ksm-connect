import { Link } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { EVENT_STATUS_LABELS, EVENT_STATUS_TONES } from '../../lib/statusLabels'
import { formatDateTime } from '../../lib/time'
import type { AdminEventView } from '../../types/api'

export function AdminEventCard({ event }: { event: AdminEventView }) {
  return (
    <Link to={`/admin/events/${event.id}`} className="group block rounded-3xl border-2 border-line p-5 transition duration-200 hover:-translate-y-1 hover:border-ksm-blue-700">
      <Badge tone={EVENT_STATUS_TONES[event.status]}>{EVENT_STATUS_LABELS[event.status]}</Badge>
      <h2 className="mt-2 text-lg leading-snug group-hover:underline">{event.name}</h2>
      <p className="mt-2 text-[15px] text-ink-500">
        <span className="font-mono font-semibold text-ksm-blue-700">{event.activeParticipants}</span> zapisanych
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
