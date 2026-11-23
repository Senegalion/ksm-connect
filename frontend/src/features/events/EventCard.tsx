import { ChevronRight, Gift } from 'lucide-react'
import { Link } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { EVENT_STATUS_LABELS, EVENT_STATUS_TONES } from '../../lib/statusLabels'
import type { ParticipantEventView } from '../../types/api'
import { EventMeta } from './EventMeta'

export function EventCard({ event }: { event: ParticipantEventView }) {
  const drawn = event.status === 'DRAW_COMPLETED'
  return (
    <Link
      to={`/events/${event.id}`}
      className="group block animate-rise rounded-[var(--radius-card)] border border-cream-200 bg-white p-5 shadow-[var(--shadow-card)] transition hover:-translate-y-0.5 hover:border-cream-300"
    >
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-2">
          <Badge tone={EVENT_STATUS_TONES[event.status]}>{EVENT_STATUS_LABELS[event.status]}</Badge>
          <h2 className="text-xl font-semibold leading-tight">{event.name}</h2>
        </div>
        <ChevronRight className="mt-1 size-5 shrink-0 text-ink-300 transition group-hover:translate-x-0.5 group-hover:text-wine-700" aria-hidden />
      </div>
      <div className="mt-4">
        <EventMeta event={event} />
      </div>
      {drawn && (
        <div className="mt-4 flex items-center gap-2 rounded-xl bg-wine-50 px-3 py-2.5 text-sm font-semibold text-wine-700">
          <Gift className="size-4" aria-hidden />
          Losowanie zakończone – sprawdź, komu robisz prezent
        </div>
      )}
    </Link>
  )
}
