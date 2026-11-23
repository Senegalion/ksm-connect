import { AlertTriangle, ChevronRight, Users } from 'lucide-react'
import { Link } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { EVENT_STATUS_LABELS, EVENT_STATUS_TONES } from '../../lib/statusLabels'
import { formatDateTime } from '../../lib/time'
import type { AdminEventView } from '../../types/api'

export function AdminEventCard({ event }: { event: AdminEventView }) {
  return (
    <Link
      to={`/admin/events/${event.id}`}
      className="group flex flex-col gap-3 rounded-[var(--radius-card)] border border-cream-200 bg-white p-5 shadow-[var(--shadow-card)] transition hover:border-cream-300"
    >
      <div className="flex items-start justify-between gap-2">
        <Badge tone={EVENT_STATUS_TONES[event.status]}>{EVENT_STATUS_LABELS[event.status]}</Badge>
        <ChevronRight className="size-5 text-ink-300 group-hover:text-wine-700" aria-hidden />
      </div>
      <h2 className="text-lg leading-tight font-semibold">{event.name}</h2>
      <div className="flex flex-wrap gap-x-4 gap-y-1 text-sm text-ink-500">
        <span className="inline-flex items-center gap-1.5"><Users className="size-4" aria-hidden />{event.activeParticipants}</span>
        {event.startsAt && <span>{formatDateTime(event.startsAt, event.timezone)}</span>}
      </div>
      {(event.drawOverdue || event.notifications.FAILED > 0) && (
        <p className="inline-flex items-center gap-1.5 text-sm font-semibold text-danger-700">
          <AlertTriangle className="size-4" aria-hidden />
          {event.drawOverdue ? 'Losowanie automatyczne wymaga uwagi' : `Nieudane powiadomienia: ${event.notifications.FAILED}`}
        </p>
      )}
    </Link>
  )
}
