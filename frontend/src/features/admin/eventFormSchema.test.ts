import { describe, expect, it } from 'vitest'
import { emptyEventForm, eventFormSchema, toRequest } from './eventFormSchema'

describe('event form', () => {
  it('requires draw time for scheduled mode', () => {
    const result = eventFormSchema.safeParse({ ...emptyEventForm, name: 'Wigilia', drawMode: 'SCHEDULED' })
    expect(result.success).toBe(false)
  })

  it('maps local Warsaw times to UTC instants', () => {
    const request = toRequest({ ...emptyEventForm, name: ' Wigilia ', startsAt: '2026-12-22T18:00', participantLimit: '40' })
    expect(request).toMatchObject({ name: 'Wigilia', startsAt: '2026-12-22T17:00:00.000Z', participantLimit: 40, timezone: 'Europe/Warsaw', description: null })
  })

  it('rejects draw before registration close', () => {
    const result = eventFormSchema.safeParse({ ...emptyEventForm, name: 'X', registrationClosesAt: '2026-12-20T20:00', drawAt: '2026-12-20T19:00' })
    expect(result.success).toBe(false)
  })
})
