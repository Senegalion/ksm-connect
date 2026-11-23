import { describe, expect, it } from 'vitest'
import { suggestionSchema } from './schema'

describe('suggestionSchema', () => {
  it('accepts http(s) product links', () => {
    expect(suggestionSchema.safeParse({ type: 'URL', url: 'https://example.com/gra' }).success).toBe(true)
  })

  it.each(['javascript:alert(1)', 'data:text/html,x', 'example.com', 'https://user:pass@example.com'])('rejects %s', (url) => {
    expect(suggestionSchema.safeParse({ type: 'URL', url }).success).toBe(false)
  })

  it('requires non-empty bounded description', () => {
    expect(suggestionSchema.safeParse({ type: 'DESCRIPTION', description: '   ' }).success).toBe(false)
    expect(suggestionSchema.safeParse({ type: 'DESCRIPTION', description: 'a'.repeat(501) }).success).toBe(false)
    expect(suggestionSchema.safeParse({ type: 'DESCRIPTION', description: 'Ciepły szalik' }).success).toBe(true)
  })
})
