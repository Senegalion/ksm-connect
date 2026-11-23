export function safeExternalUrl(value: string | undefined | null): string | null {
  if (!value) {
    return null
  }
  try {
    const url = new URL(value)
    return url.protocol === 'https:' || url.protocol === 'http:' ? url.href : null
  } catch {
    return null
  }
}

export function displayHost(value: string): string {
  try {
    return new URL(value).hostname.replace(/^www\./, '')
  } catch {
    return value
  }
}
