import type { EventStatus, NotificationStatus } from '../types/api'

export const EVENT_STATUS_LABELS: Record<EventStatus, string> = {
  DRAFT: 'Szkic',
  REGISTRATION_OPEN: 'Zapisy otwarte',
  REGISTRATION_CLOSED: 'Zapisy zamknięte',
  DRAW_COMPLETED: 'Po losowaniu',
  CANCELLED: 'Odwołane',
}

export const EVENT_STATUS_TONES: Record<EventStatus, 'neutral' | 'success' | 'warning' | 'wine' | 'danger'> = {
  DRAFT: 'neutral',
  REGISTRATION_OPEN: 'success',
  REGISTRATION_CLOSED: 'warning',
  DRAW_COMPLETED: 'wine',
  CANCELLED: 'danger',
}

export const NOTIFICATION_STATUS_LABELS: Record<NotificationStatus, string> = {
  PENDING: 'Oczekuje',
  PROCESSING: 'W trakcie wysyłki',
  SENT: 'Wysłane',
  RETRY_PENDING: 'Ponowienie zaplanowane',
  FAILED: 'Nieudane',
}

export const NOTIFICATION_STATUS_TONES: Record<NotificationStatus, 'neutral' | 'success' | 'warning' | 'wine' | 'danger'> = {
  PENDING: 'neutral',
  PROCESSING: 'neutral',
  SENT: 'success',
  RETRY_PENDING: 'warning',
  FAILED: 'danger',
}
