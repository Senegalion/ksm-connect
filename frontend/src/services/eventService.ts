import { api } from '../lib/api'
import type { AssignmentView, JoinResponse, ParticipantEventView, SuggestionType, WishlistView } from '../types/api'

export interface SuggestionInput {
  type: SuggestionType
  url?: string
  description?: string
}

export const eventService = {
  myEvents: () => api.get<ParticipantEventView[]>('/api/events'),
  myEvent: (eventId: string) => api.get<ParticipantEventView>(`/api/events/${eventId}`),
  withdraw: (eventId: string) => api.post<undefined>(`/api/events/${eventId}/withdraw`),
  previewInvitation: (code: string) => api.post<ParticipantEventView>('/api/invitations/preview', { code }),
  acceptInvitation: (code: string) => api.post<JoinResponse>('/api/invitations/accept', { code }),
  wishlist: (eventId: string) => api.get<WishlistView>(`/api/events/${eventId}/wishlist`),
  saveSuggestion: (eventId: string, position: number, input: SuggestionInput) =>
    api.put<WishlistView>(`/api/events/${eventId}/wishlist/${position}`, input),
  removeSuggestion: (eventId: string, position: number) =>
    api.delete<WishlistView>(`/api/events/${eventId}/wishlist/${position}`),
  assignment: (eventId: string) => api.get<AssignmentView>(`/api/events/${eventId}/assignment`),
  markAssignmentRevealed: (eventId: string) => api.post<AssignmentView>(`/api/events/${eventId}/assignment/reveal`),
}
