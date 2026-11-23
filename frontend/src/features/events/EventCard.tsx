import { Link } from 'react-router'
import { Badge } from '../../components/ui/Badge'
import { pluralParticipants } from '../../lib/plural'
import { EVENT_STATUS_LABELS, EVENT_STATUS_TONES } from '../../lib/statusLabels'
import type { ParticipantEventView } from '../../types/api'
import { DateTile } from './EventHero'

export function EventCard({ event }: { event: ParticipantEventView }) {
  const drawn = event.status === 'DRAW_COMPLETED'
  return (
    <Link
      to={`/events/${event.id}`}
      className="group relative block overflow-hidden rounded-3xl border-2 border-line bg-paper p-6 transition duration-200 hover:-translate-y-1 hover:border-ksm-blue-700"
    >
      <div className="pointer-events-none absolute -top-10 -right-10 size-32 rounded-full bg-ksm-yellow-400/25 transition-transform duration-300 group-hover:scale-125" aria-hidden />
      <div className="relative flex items-start gap-4">
        <DateTile iso={event.startsAt} timeZone={event.timezone} />
        <div className="min-w-0 space-y-2">
          <Badge tone={EVENT_STATUS_TONES[event.status]}>{EVENT_STATUS_LABELS[event.status]}</Badge>
          <h2 className="text-2xl leading-tight">{event.name}</h2>
          <p className="text-[15px] text-ink-500">
            {event.location && <>{event.location} · </>}
            {event.participantCount} {pluralParticipants(event.participantCount)}
          </p>
        </div>
      </div>
      <div className="relative mt-5 flex items-center justify-between gap-3 border-t border-line pt-4 text-[15px] font-semibold">
        <span className={drawn ? 'text-ksm-red-600' : 'text-ksm-blue-700'}>
          {!drawn ? 'Uzupełnij listę prezentów' : event.participation?.assignmentRevealed ? 'Zobacz swoją osobę i jej listę' : 'Twój los czeka na zdrapanie'}
        </span>
        <span className="text-ksm-blue-700 transition-transform group-hover:translate-x-1" aria-hidden>→</span>
      </div>
    </Link>
  )
}
