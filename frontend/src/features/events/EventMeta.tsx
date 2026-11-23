import { CalendarDays, MapPin, Shuffle, Users } from 'lucide-react'
import { formatDateTime } from '../../lib/time'
import { pluralParticipants } from '../../lib/plural'
import type { ParticipantEventView } from '../../types/api'

export function EventMeta({ event }: { event: ParticipantEventView }) {
  const rows = [
    event.startsAt && { icon: CalendarDays, label: formatDateTime(event.startsAt, event.timezone) },
    event.location && { icon: MapPin, label: event.location },
    { icon: Users, label: `${event.participantCount} ${pluralParticipants(event.participantCount)}` },
    event.drawAt && event.status !== 'DRAW_COMPLETED' && { icon: Shuffle, label: `Losowanie: ${formatDateTime(event.drawAt, event.timezone)}` },
  ].filter(Boolean) as Array<{ icon: typeof CalendarDays; label: string }>

  return (
    <ul className="space-y-2 text-sm text-ink-700">
      {rows.map(({ icon: Icon, label }) => (
        <li key={label} className="flex items-start gap-2.5">
          <Icon className="mt-0.5 size-4 shrink-0 text-wine-700" aria-hidden />
          <span>{label}</span>
        </li>
      ))}
    </ul>
  )
}
