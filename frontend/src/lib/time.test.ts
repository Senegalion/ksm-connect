import { describe, expect, it } from 'vitest'
import { isoToZonedLocal, zonedLocalToIso } from './time'

describe('zoned time conversion', () => {
  it('converts Warsaw winter time to UTC', () => {
    expect(zonedLocalToIso('2026-12-24T18:00', 'Europe/Warsaw')).toBe('2026-12-24T17:00:00.000Z')
  })

  it('converts Warsaw summer time to UTC', () => {
    expect(zonedLocalToIso('2026-07-01T12:30', 'Europe/Warsaw')).toBe('2026-07-01T10:30:00.000Z')
  })

  it('round trips through the event time zone', () => {
    const iso = '2026-12-20T20:15:00.000Z'
    expect(zonedLocalToIso(isoToZonedLocal(iso, 'Europe/Warsaw'), 'Europe/Warsaw')).toBe(iso)
  })

  it('rejects malformed input', () => {
    expect(zonedLocalToIso('24.12.2026 18:00', 'Europe/Warsaw')).toBeNull()
  })
})
