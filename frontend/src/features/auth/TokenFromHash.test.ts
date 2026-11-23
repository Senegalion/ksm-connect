import { describe, expect, it } from 'vitest'
import { tokenFromHash } from './TokenFromHash'

describe('tokenFromHash', () => {
  it('extracts a well-formed token', () => {
    expect(tokenFromHash('#token=abcdefghijklmnopqrstuvwxyz_-0123')).toBe('abcdefghijklmnopqrstuvwxyz_-0123')
  })

  it('rejects missing or malformed tokens', () => {
    expect(tokenFromHash('')).toBeNull()
    expect(tokenFromHash('#token=<script>')).toBeNull()
    expect(tokenFromHash('#other=abc')).toBeNull()
  })
})
