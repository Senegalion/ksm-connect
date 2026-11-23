import { z } from 'zod'
import { isoToZonedLocal, zonedLocalToIso } from '../../lib/time'
import type { AdminEventRequest, AdminEventView } from '../../types/api'

export const DEFAULT_TIMEZONE = 'Europe/Warsaw'

const localDateTime = z.string().refine((value) => value === '' || /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value), 'Nieprawidłowa data.')

export const eventFormSchema = z
  .object({
    name: z.string().trim().min(1, 'Podaj nazwę wydarzenia.').max(150),
    description: z.string().max(4000),
    location: z.string().max(200),
    startsAt: localDateTime,
    registrationOpensAt: localDateTime,
    registrationClosesAt: localDateTime,
    drawMode: z.enum(['MANUAL', 'SCHEDULED']),
    drawAt: localDateTime,
    wishlistDeadline: localDateTime,
    participantLimit: z.string().refine((value) => value === '' || (/^\d+$/.test(value) && Number(value) >= 2 && Number(value) <= 10000), 'Limit musi wynosić od 2 do 10000.'),
  })
  .refine((values) => values.drawMode === 'MANUAL' || values.drawAt !== '', {
    path: ['drawAt'],
    message: 'Podaj datę i godzinę losowania.',
  })
  .refine((values) => !values.registrationOpensAt || !values.registrationClosesAt || values.registrationClosesAt > values.registrationOpensAt, {
    path: ['registrationClosesAt'],
    message: 'Zamknięcie zapisów musi być po ich otwarciu.',
  })
  .refine((values) => !values.drawAt || !values.registrationClosesAt || values.drawAt >= values.registrationClosesAt, {
    path: ['drawAt'],
    message: 'Losowanie nie może być przed zamknięciem zapisów.',
  })

export type EventFormValues = z.infer<typeof eventFormSchema>

export const emptyEventForm: EventFormValues = {
  name: '',
  description: '',
  location: '',
  startsAt: '',
  registrationOpensAt: '',
  registrationClosesAt: '',
  drawMode: 'MANUAL',
  drawAt: '',
  wishlistDeadline: '',
  participantLimit: '',
}

export function toFormValues(event: AdminEventView): EventFormValues {
  const zone = event.timezone
  return {
    name: event.name,
    description: event.description ?? '',
    location: event.location ?? '',
    startsAt: isoToZonedLocal(event.startsAt, zone),
    registrationOpensAt: isoToZonedLocal(event.registrationOpensAt, zone),
    registrationClosesAt: isoToZonedLocal(event.registrationClosesAt, zone),
    drawMode: event.drawMode,
    drawAt: isoToZonedLocal(event.drawAt, zone),
    wishlistDeadline: isoToZonedLocal(event.wishlistDeadline, zone),
    participantLimit: event.participantLimit ? String(event.participantLimit) : '',
  }
}

export function toRequest(values: EventFormValues, timezone = DEFAULT_TIMEZONE): AdminEventRequest {
  const instant = (value: string) => (value ? zonedLocalToIso(value, timezone) : null)
  return {
    name: values.name.trim(),
    description: values.description.trim() || null,
    location: values.location.trim() || null,
    startsAt: instant(values.startsAt),
    timezone,
    registrationOpensAt: instant(values.registrationOpensAt),
    registrationClosesAt: instant(values.registrationClosesAt),
    drawMode: values.drawMode,
    drawAt: instant(values.drawAt),
    wishlistDeadline: instant(values.wishlistDeadline),
    participantLimit: values.participantLimit ? Number(values.participantLimit) : null,
  }
}
