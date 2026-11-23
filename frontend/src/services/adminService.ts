import { api } from '../lib/api'
import type {
  AdminEventRequest,
  AdminEventView,
  AuditEntry,
  DeliveryStatusView,
  DrawResult,
  GeneratedInvitation,
  ParticipantSummary,
} from '../types/api'

const base = (eventId: string) => `/api/admin/events/${eventId}`

export type LifecycleAction = 'open-registration' | 'close-registration' | 'cancel'

export const adminService = {
  events: () => api.get<AdminEventView[]>('/api/admin/events'),
  event: (eventId: string) => api.get<AdminEventView>(base(eventId)),
  create: (input: AdminEventRequest) => api.post<AdminEventView>('/api/admin/events', input),
  update: (eventId: string, input: AdminEventRequest) => api.put<AdminEventView>(base(eventId), input),
  changeLifecycle: (eventId: string, action: LifecycleAction) => api.post<AdminEventView>(`${base(eventId)}/${action}`),
  regenerateInvitation: (eventId: string) => api.post<GeneratedInvitation>(`${base(eventId)}/invitation`),
  participants: (eventId: string) => api.get<ParticipantSummary[]>(`${base(eventId)}/participants`),
  removeParticipant: (eventId: string, participantId: string) =>
    api.delete<undefined>(`${base(eventId)}/participants/${participantId}`),
  draw: (eventId: string) => api.post<DrawResult>(`${base(eventId)}/draw`, { confirmed: true }),
  notifications: (eventId: string) => api.get<DeliveryStatusView[]>(`${base(eventId)}/notifications`),
  retryNotification: (eventId: string, notificationId: string) =>
    api.post<undefined>(`${base(eventId)}/notifications/${notificationId}/retry`),
  retryFailed: (eventId: string) => api.post<{ requeued: number }>(`${base(eventId)}/notifications/retry-failed`),
  audit: (eventId: string) => api.get<AuditEntry[]>(`${base(eventId)}/audit`),
}
