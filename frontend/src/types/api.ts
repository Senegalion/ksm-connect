export type Role = 'ADMIN' | 'PARTICIPANT'

export interface SessionUser {
  id: string
  email: string
  firstName: string
  lastName: string
  phoneNumber?: string
  roles: Role[]
}

export interface SessionView {
  authenticated: boolean
  user?: SessionUser
}

export type EventStatus = 'DRAFT' | 'REGISTRATION_OPEN' | 'REGISTRATION_CLOSED' | 'DRAW_COMPLETED' | 'CANCELLED'
export type DrawMode = 'MANUAL' | 'SCHEDULED'
export type ParticipantStatus = 'ACTIVE' | 'WITHDRAWN'
export type RegistrationState = 'NOT_STARTED' | 'OPEN' | 'CLOSED'

export interface ParticipantEventView {
  id: string
  name: string
  description?: string
  location?: string
  startsAt?: string
  timezone: string
  registrationOpensAt?: string
  registrationClosesAt?: string
  drawMode: DrawMode
  drawAt?: string
  wishlistDeadline?: string
  status: EventStatus
  participantCount: number
  registrationState: RegistrationState
  participation?: {
    status: ParticipantStatus
    joinedAt: string
    wishlistEditable: boolean
  }
}

export interface JoinResponse {
  event: ParticipantEventView
  newlyJoined: boolean
}

export type SuggestionType = 'URL' | 'DESCRIPTION'

export interface WishlistItem {
  position: number
  type: SuggestionType
  url?: string
  description?: string
  updatedAt: string
}

export interface WishlistView {
  editable: boolean
  maxItems: number
  filledCount: number
  items: WishlistItem[]
}

export interface AssignmentView {
  eventStatus: EventStatus
  drawCompleted: boolean
  drawAt?: string
  recipient?: {
    firstName: string
    lastName: string
    wishlist: WishlistItem[]
  }
}

export type NotificationStatus = 'PENDING' | 'PROCESSING' | 'SENT' | 'RETRY_PENDING' | 'FAILED'

export interface InvitationStatus {
  exists: boolean
  createdAt?: string
  expiresAt?: string
  valid: boolean
}

export interface AdminEventView {
  id: string
  name: string
  description?: string
  location?: string
  startsAt?: string
  timezone: string
  registrationOpensAt?: string
  registrationClosesAt?: string
  drawMode: DrawMode
  drawAt?: string
  wishlistDeadline?: string
  participantLimit?: number
  status: EventStatus
  drawCompletedAt?: string
  createdAt: string
  updatedAt: string
  activeParticipants: number
  drawOverdue: boolean
  registrationState: RegistrationState
  invitation: InvitationStatus
  notifications: Record<NotificationStatus, number>
}

export interface AdminEventRequest {
  name: string
  description?: string | null
  location?: string | null
  startsAt?: string | null
  timezone: string
  registrationOpensAt?: string | null
  registrationClosesAt?: string | null
  drawMode: DrawMode
  drawAt?: string | null
  wishlistDeadline?: string | null
  participantLimit?: number | null
}

export interface GeneratedInvitation {
  code: string
  joinUrl: string
  expiresAt?: string
}

export interface ParticipantSummary {
  participantId: string
  firstName: string
  lastName: string
  email: string
  phoneNumber?: string
  status: ParticipantStatus
  joinedAt: string
}

export interface DeliveryStatusView {
  id: string
  participantId: string
  participantName?: string
  channel: 'EMAIL' | 'SMS'
  status: NotificationStatus
  attemptCount: number
  maxAttempts: number
  nextAttemptAt: string
  sentAt?: string
  lastError?: string
}

export interface DrawResult {
  eventId: string
  participantCount: number
  completedAt: string
}

export interface AuditEntry {
  type: string
  createdAt: string
  details?: string
}

export interface PublicInfo {
  privacyNoticeVersion: string
  defaultTimezone: string
  smsEnabled: boolean
}
