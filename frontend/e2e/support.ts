import { expect, type APIRequestContext, type Page } from '@playwright/test'
import { execFileSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const MAILPIT_URL = process.env.MAILPIT_URL ?? 'http://localhost:8025'
const MAILPIT_HEADERS: Record<string, string> = process.env.MAILPIT_USER && process.env.MAILPIT_PASSWORD
  ? { Authorization: `Basic ${Buffer.from(`${process.env.MAILPIT_USER}:${process.env.MAILPIT_PASSWORD}`).toString('base64')}` }
  : {}
const REMOTE_ADMIN = process.env.E2E_ADMIN_EMAIL && process.env.E2E_ADMIN_PASSWORD
  ? { email: process.env.E2E_ADMIN_EMAIL, password: process.env.E2E_ADMIN_PASSWORD }
  : null
const REPO_ROOT = fileURLToPath(new URL('../../', import.meta.url))
export const PASSWORD = 'zimowa wigilia przy choince'

export interface TestUser {
  firstName: string
  lastName: string
  email: string
}

let counter = 0

export function newUser(firstName: string): TestUser {
  counter += 1
  const unique = `${Date.now().toString(36)}${counter}${Math.random().toString(36).slice(2, 6)}`
  return { firstName, lastName: 'Testowa', email: `${firstName.toLowerCase()}.${unique}@e2e.ksm.local` }
}

interface MailpitSummary {
  ID: string
  Subject: string
}

export async function waitForEmail(request: APIRequestContext, to: string, subjectFragment: string): Promise<string> {
  let body = ''
  await expect.poll(async () => {
    const search = await request.get(`${MAILPIT_URL}/api/v1/search`, { params: { query: `to:"${to}"` }, headers: MAILPIT_HEADERS })
    const { messages } = (await search.json()) as { messages: MailpitSummary[] }
    const match = messages.find((message) => message.Subject.includes(subjectFragment))
    if (!match) {
      return false
    }
    const message = await request.get(`${MAILPIT_URL}/api/v1/message/${match.ID}`, { headers: MAILPIT_HEADERS })
    body = ((await message.json()) as { Text: string }).Text
    return true
  }, { timeout: 30_000, intervals: [500, 1000, 2000] }).toBe(true)
  return body
}

export function firstLink(text: string, pathFragment: string): string {
  const match = text.match(new RegExp(`https?://\\S*${pathFragment}\\S*`))
  if (!match) {
    throw new Error(`No link containing ${pathFragment}`)
  }
  return match[0]
}

export async function registerAndVerify(page: Page, user: TestUser) {
  await page.goto('/register')
  await page.getByLabel('Imię').fill(user.firstName)
  await page.getByLabel('Nazwisko').fill(user.lastName)
  await page.getByLabel('E-mail').fill(user.email)
  await page.getByLabel('Numer telefonu').fill('600 100 200')
  await page.getByLabel('Hasło').fill(PASSWORD)
  await page.getByLabel(/Zapoznałem/).check()
  await page.getByRole('button', { name: 'Załóż konto' }).click()
  await expect(page.getByRole('heading', { name: 'Sprawdź swoją skrzynkę' })).toBeVisible()

  const email = await waitForEmail(page.request, user.email, 'Potwierdź')
  await page.goto(firstLink(email, '/verify-email'))
  await expect(page.getByText('Adres e-mail potwierdzony!')).toBeVisible()
}

export async function login(page: Page, user: TestUser, password = PASSWORD) {
  await page.goto('/login')
  await page.getByLabel('E-mail').fill(user.email)
  await page.getByLabel('Hasło').fill(password)
  await page.getByRole('button', { name: 'Zaloguj się' }).click()
  await expect(page).not.toHaveURL(/\/login/)
}

export async function logout(page: Page) {
  await page.getByRole('button', { name: 'Wyloguj' }).click()
  await expect(page).toHaveURL(/\/login/)
}

export async function loginAsAdmin(page: Page): Promise<TestUser> {
  if (REMOTE_ADMIN) {
    const admin = { firstName: 'Admin', lastName: 'E2E', email: REMOTE_ADMIN.email }
    await login(page, admin, REMOTE_ADMIN.password)
    return admin
  }
  const admin = newUser('Organizator')
  await registerAndVerify(page, admin)
  promoteToAdmin(admin.email)
  await login(page, admin)
  return admin
}

export function promoteToAdmin(email: string) {
  execFileSync('docker', [
    'compose', 'run', '--rm', '--no-deps', 'backend',
    '--spring.main.web-application-type=none',
    '--ksm.scheduling.enabled=false',
    `--ksm.tasks.promote-admin=${email}`,
  ], { cwd: REPO_ROOT, stdio: 'ignore' })
}

export async function expectNoHorizontalScroll(page: Page) {
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)
  expect(overflow).toBeLessThanOrEqual(0)
}

export async function apiAs(page: Page) {
  const csrf = await (await page.request.get('/api/auth/csrf')).json() as { headerName: string; token: string }
  return {
    post: (path: string, data: unknown = {}) => page.request.post(path, { data, headers: { [csrf.headerName]: csrf.token } }),
  }
}
