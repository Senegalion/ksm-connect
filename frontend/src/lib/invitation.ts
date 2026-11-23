const PENDING_CODE_KEY = 'ksm.pendingInvitation'
const PENDING_CODE_TTL_MS = 24 * 60 * 60 * 1000

export function normalizeInvitationInput(value: string): string {
  return value.toUpperCase().replace(/[^A-Z0-9]/g, '')
}

export function invitationCodeFromHash(hash: string): string | null {
  let raw: string
  try {
    raw = decodeURIComponent(hash.replace(/^#/, '')).trim()
  } catch {
    return null
  }
  return raw.length > 0 && raw.length <= 64 ? raw : null
}

export function rememberPendingInvitation(code: string) {
  try {
    localStorage.setItem(PENDING_CODE_KEY, JSON.stringify({ code, savedAt: Date.now() }))
  } catch {
    return
  }
}

export function clearPendingInvitation() {
  try {
    localStorage.removeItem(PENDING_CODE_KEY)
  } catch {
    return
  }
}

export function peekPendingInvitation(): string | null {
  try {
    const stored = localStorage.getItem(PENDING_CODE_KEY)
    if (!stored) {
      return null
    }
    const parsed = JSON.parse(stored) as { code?: unknown; savedAt?: unknown }
    if (typeof parsed.code !== 'string' || typeof parsed.savedAt !== 'number' || Date.now() - parsed.savedAt > PENDING_CODE_TTL_MS) {
      return null
    }
    return parsed.code
  } catch {
    return null
  }
}
