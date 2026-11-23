export function tokenFromHash(hash: string): string | null {
  const params = new URLSearchParams(hash.replace(/^#/, ''))
  const token = params.get('token')
  return token && /^[A-Za-z0-9_-]{20,128}$/.test(token) ? token : null
}
