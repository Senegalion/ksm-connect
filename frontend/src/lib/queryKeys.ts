export const queryKeys = {
  session: ['session'] as const,
  publicInfo: ['public-info'] as const,
  myEvents: ['events', 'mine'] as const,
  myEvent: (eventId: string) => ['events', 'mine', eventId] as const,
  wishlist: (eventId: string) => ['events', eventId, 'wishlist'] as const,
  assignment: (eventId: string) => ['events', eventId, 'assignment'] as const,
  adminEvents: ['admin', 'events'] as const,
  adminEvent: (eventId: string) => ['admin', 'events', eventId] as const,
  adminParticipants: (eventId: string) => ['admin', 'events', eventId, 'participants'] as const,
  adminNotifications: (eventId: string) => ['admin', 'events', eventId, 'notifications'] as const,
  adminAudit: (eventId: string) => ['admin', 'events', eventId, 'audit'] as const,
}
