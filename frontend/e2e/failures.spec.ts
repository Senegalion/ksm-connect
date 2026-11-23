import { expect, test } from '@playwright/test'
import { apiAs, login, newUser, loginAsAdmin, registerAndVerify } from './support'

test('invalid and revoked invitation codes are rejected with a clear message', async ({ page, browser }) => {
  await loginAsAdmin(page)

  const api = await apiAs(page)
  const created = await (await api.post('/api/admin/events', { name: `Test kodów ${Date.now()}`, timezone: 'Europe/Warsaw', drawMode: 'MANUAL' })).json() as { id: string }
  await api.post(`/api/admin/events/${created.id}/open-registration`)
  const first = await (await api.post(`/api/admin/events/${created.id}/invitation`)).json() as { code: string }
  const second = await (await api.post(`/api/admin/events/${created.id}/invitation`)).json() as { code: string }

  const participantContext = await browser.newContext()
  const participantPage = await participantContext.newPage()
  const participant = newUser('Gosc')
  await registerAndVerify(participantPage, participant)
  await login(participantPage, participant)

  await participantPage.goto('/join')
  await participantPage.getByLabel('Kod zaproszenia').fill('KSM-AAAAA-AAAAA')
  await participantPage.getByRole('button', { name: 'Sprawdź kod' }).click()
  await expect(participantPage.getByText(/kod zaproszenia jest nieprawidłowy/i)).toBeVisible()

  await participantPage.getByLabel('Kod zaproszenia').fill(first.code)
  await participantPage.getByRole('button', { name: 'Sprawdź kod' }).click()
  await expect(participantPage.getByText(/kod zaproszenia jest nieprawidłowy/i)).toBeVisible()

  await participantPage.getByLabel('Kod zaproszenia').fill(second.code.toLowerCase())
  await participantPage.getByRole('button', { name: 'Sprawdź kod' }).click()
  await expect(participantPage.getByRole('button', { name: 'Dołączam!' })).toBeVisible()
  await participantContext.close()
})

test('draw is blocked while registration is open and concurrent draw requests produce one result', async ({ page, browser }) => {
  await loginAsAdmin(page)
  const api = await apiAs(page)
  const created = await (await api.post('/api/admin/events', { name: `Równoległe ${Date.now()}`, timezone: 'Europe/Warsaw', drawMode: 'MANUAL' })).json() as { id: string }
  await api.post(`/api/admin/events/${created.id}/open-registration`)
  const { code } = await (await api.post(`/api/admin/events/${created.id}/invitation`)).json() as { code: string }
  await api.post('/api/invitations/accept', { code })

  await page.goto(`/admin/events/${created.id}`)
  await expect(page.getByRole('button', { name: 'Przeprowadź losowanie' })).toBeDisabled()
  expect((await api.post(`/api/admin/events/${created.id}/draw`, { confirmed: true })).status()).toBe(409)

  const second = newUser('Drugi')
  const otherContext = await browser.newContext()
  const otherPage = await otherContext.newPage()
  await registerAndVerify(otherPage, second)
  await login(otherPage, second)
  await (await apiAs(otherPage)).post('/api/invitations/accept', { code })
  await otherContext.close()

  await api.post(`/api/admin/events/${created.id}/close-registration`)
  const responses = await Promise.all(Array.from({ length: 5 }, () => api.post(`/api/admin/events/${created.id}/draw`, { confirmed: true })))
  const statuses = responses.map((response) => response.status()).sort()
  expect(statuses.filter((status) => status === 200)).toHaveLength(1)
  expect(statuses.filter((status) => status === 409)).toHaveLength(4)
})

test('participants cannot open the organizer panel', async ({ page }) => {
  const participant = newUser('Ciekawski')
  await registerAndVerify(page, participant)
  await login(page, participant)
  await page.goto('/admin')
  await expect(page).toHaveURL(/\/dashboard/)
  const response = await page.request.get('/api/admin/events')
  expect(response.status()).toBe(403)
})
