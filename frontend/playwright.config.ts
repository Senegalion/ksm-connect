import { defineConfig, devices } from '@playwright/test'

const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:5173'

export default defineConfig({
  testDir: './e2e',
  timeout: 90_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    locale: 'pl-PL',
    timezoneId: 'Europe/Warsaw',
  },
  projects: [
    { name: 'desktop', use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 860 } }, testIgnore: /mobile/ },
    { name: 'mobile-360', use: { ...devices['Galaxy S9+'], viewport: { width: 360, height: 760 } }, testMatch: /mobile|journey/ },
    { name: 'mobile-390', use: { ...devices['iPhone 13'], defaultBrowserType: 'chromium' }, testMatch: /mobile/ },
  ],
  webServer: process.env.E2E_BASE_URL
    ? undefined
    : { command: 'npm run dev -- --host 127.0.0.1 --port 5173 --strictPort', url: baseURL, reuseExistingServer: true, timeout: 60_000 },
})
