import { pluralParticipants } from '../../lib/plural'
import { formatDateTime } from '../../lib/time'
import type { ParticipantEventView } from '../../types/api'

export function EventMeta({ event }: { event: ParticipantEventView }) {
  const rows = [
    event.startsAt && ['Termin', formatDateTime(event.startsAt, event.timezone)],
    event.location && ['Miejsce', event.location],
    ['Zapisani', `${event.participantCount} ${pluralParticipants(event.participantCount)}`],
    event.drawAt && event.status !== 'DRAW_COMPLETED' && ['Losowanie', formatDateTime(event.drawAt, event.timezone)],
  ].filter(Boolean) as Array<[string, string]>

  return (
    <dl className="grid grid-cols-[6.5rem_1fr] gap-x-3 gap-y-1.5 text-[15px]">
      {rows.map(([label, value]) => (
        <div key={label} className="contents">
          <dt className="text-ink-500">{label}</dt>
          <dd className="text-ink-900">{value}</dd>
        </div>
      ))}
    </dl>
  )
}
