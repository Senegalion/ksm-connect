import { describe, expect, it } from 'vitest'
import { invitationCodeFromHash, normalizeInvitationInput } from './invitation'

describe('invitation helpers', () => {
  it('reads code from URL fragment', () => {
    expect(invitationCodeFromHash('#KSM-ABCDE-FGHJK')).toBe('KSM-ABCDE-FGHJK')
    expect(invitationCodeFromHash('')).toBeNull()
  })

  it('normalizes typed codes', () => {
    expect(normalizeInvitationInput(' ksm-abcde fghjk ')).toBe('KSMABCDEFGHJK')
  })
})
