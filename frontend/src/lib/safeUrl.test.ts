import { describe, expect, it } from 'vitest'
import { displayHost, safeExternalUrl } from './safeUrl'

describe('safeExternalUrl', () => {
  it.each(['https://example.com/a', 'http://sklep.pl/x?y=1'])('accepts %s', (url) => {
    expect(safeExternalUrl(url)).not.toBeNull()
  })

  it.each(['javascript:alert(1)', 'data:text/html,x', 'file:///etc/passwd', 'not a url', '', null])('rejects %s', (url) => {
    expect(safeExternalUrl(url)).toBeNull()
  })

  it('shows a short host', () => {
    expect(displayHost('https://www.empik.com/ksiazka')).toBe('empik.com')
  })
})
