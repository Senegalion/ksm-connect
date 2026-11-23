import { writeFileSync } from 'node:fs'

const apiOrigin = process.env.VITE_API_BASE_URL ? new URL(process.env.VITE_API_BASE_URL).origin : ''
const connectSources = ["'self'", apiOrigin].filter(Boolean).join(' ')

const config = {
  navigationFallback: {
    rewrite: '/index.html',
    exclude: ['/assets/*', '/favicon.svg'],
  },
  globalHeaders: {
    'Content-Security-Policy': `default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob:; font-src 'self'; connect-src ${connectSources}; frame-ancestors 'none'; base-uri 'none'; form-action 'self'; object-src 'none'`,
    'Strict-Transport-Security': 'max-age=31536000; includeSubDomains',
    'X-Content-Type-Options': 'nosniff',
    'X-Frame-Options': 'DENY',
    'Referrer-Policy': 'no-referrer',
    'Permissions-Policy': 'camera=(), microphone=(), geolocation=()',
  },
  routes: [
    { route: '/assets/*', headers: { 'Cache-Control': 'public, max-age=31536000, immutable' } },
    { route: '/index.html', headers: { 'Cache-Control': 'no-cache' } },
  ],
}

writeFileSync(new URL('../dist/staticwebapp.config.json', import.meta.url), JSON.stringify(config, null, 2))
