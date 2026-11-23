const PL = 'pl-PL'

function partsInZone(date: Date, timeZone: string) {
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone,
    hourCycle: 'h23',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
  const values: Record<string, number> = {}
  for (const part of formatter.formatToParts(date)) {
    if (part.type !== 'literal') {
      values[part.type] = Number(part.value)
    }
  }
  return values as Record<'year' | 'month' | 'day' | 'hour' | 'minute' | 'second', number>
}

function offsetMillis(date: Date, timeZone: string): number {
  const p = partsInZone(date, timeZone)
  const asUtc = Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second)
  return asUtc - Math.floor(date.getTime() / 1000) * 1000
}

export function zonedLocalToIso(local: string, timeZone: string): string | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(local)
  if (!match) {
    return null
  }
  const [, y, mo, d, h, mi] = match.map(Number) as [number, number, number, number, number, number]
  const naiveUtc = Date.UTC(y, mo - 1, d, h, mi)
  let guess = naiveUtc - offsetMillis(new Date(naiveUtc), timeZone)
  guess = naiveUtc - offsetMillis(new Date(guess), timeZone)
  return new Date(guess).toISOString()
}

export function isoToZonedLocal(iso: string | undefined | null, timeZone: string): string {
  if (!iso) {
    return ''
  }
  const p = partsInZone(new Date(iso), timeZone)
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${p.year}-${pad(p.month)}-${pad(p.day)}T${pad(p.hour)}:${pad(p.minute)}`
}

export function formatDateTime(iso: string | undefined | null, timeZone: string): string {
  if (!iso) {
    return '—'
  }
  return new Intl.DateTimeFormat(PL, {
    timeZone,
    day: 'numeric',
    month: 'long',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso))
}

export function formatDate(iso: string | undefined | null, timeZone: string): string {
  if (!iso) {
    return '—'
  }
  return new Intl.DateTimeFormat(PL, { timeZone, day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(iso))
}
