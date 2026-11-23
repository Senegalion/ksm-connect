import { Badge } from '../../components/ui/Badge'
import { EVENT_STATUS_LABELS, EVENT_STATUS_TONES } from '../../lib/statusLabels'
import type { ParticipantEventView } from '../../types/api'
import { EventMeta } from './EventMeta'

function dateParts(iso: string | undefined, timeZone: string) {
  if (!iso) {
    return null
  }
  const date = new Date(iso)
  return {
    day: new Intl.DateTimeFormat('pl-PL', { timeZone, day: 'numeric' }).format(date),
    month: new Intl.DateTimeFormat('pl-PL', { timeZone, month: 'short' }).format(date).replace('.', ''),
  }
}

export function DateTile({ iso, timeZone, className = '' }: { iso?: string; timeZone: string; className?: string }) {
  const parts = dateParts(iso, timeZone)
  if (!parts) {
    return null
  }
  return (
    <div className={`flex size-20 shrink-0 flex-col items-center justify-center rounded-2xl bg-ksm-yellow-400 text-ksm-blue-900 ${className}`} aria-hidden>
      <span className="display-wide text-3xl leading-none">{parts.day}</span>
      <span className="mt-1 text-xs font-bold tracking-widest uppercase">{parts.month}</span>
    </div>
  )
}

export function EventHero({ event }: { event: ParticipantEventView }) {
  return (
    <header className="relative overflow-hidden rounded-3xl bg-ksm-blue-800 text-white">
      <div className="pointer-events-none absolute -top-20 -right-16 size-64 rounded-full bg-ksm-yellow-400 opacity-90" aria-hidden />
      <div className="pointer-events-none absolute right-24 -bottom-10 size-28 rotate-12 bg-ksm-red-600" aria-hidden />
      <div className="relative space-y-5 p-6 sm:p-10">
        <div className="flex items-start justify-between gap-4">
          <span className="rounded-full bg-white px-3 py-1"><Badge tone={EVENT_STATUS_TONES[event.status]}>{EVENT_STATUS_LABELS[event.status]}</Badge></span>
          <DateTile iso={event.startsAt} timeZone={event.timezone} className="sm:hidden" />
        </div>
        <div className="flex items-end gap-6">
          <DateTile iso={event.startsAt} timeZone={event.timezone} className="hidden size-24 sm:flex" />
          <h1 className="display-wide max-w-2xl text-[32px] leading-[1] text-white sm:text-5xl">{event.name}</h1>
        </div>
        {event.description && <p className="max-w-xl leading-relaxed whitespace-pre-line text-white/85">{event.description}</p>}
        <EventMeta event={event} inverted />
      </div>
    </header>
  )
}
