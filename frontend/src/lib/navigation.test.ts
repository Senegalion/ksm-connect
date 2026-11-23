import { describe, expect, it } from 'vitest'
import { safeNextPath } from './navigation'

describe('safeNextPath', () => {
  it('keeps internal paths', () => {
    expect(safeNextPath('/events/1')).toBe('/events/1')
  })

  it.each([null, '', 'https://evil.example', '//evil.example', 'javascript:alert(1)'])('falls back for %s', (value) => {
    expect(safeNextPath(value)).toBe('/dashboard')
  })
})
