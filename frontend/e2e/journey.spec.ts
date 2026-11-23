import { expect, test, type Browser, type Page } from '@playwright/test'
import { apiAs, expectNoHorizontalScroll, firstLink, login, logout, newUser, promoteToAdmin, registerAndVerify, waitForEmail, type TestUser } from './support'

async function newParticipantPage(browser: Browser): Promise<Page> {
  const { viewport, userAgent, isMobile, hasTouch, deviceScaleFactor, locale, timezoneId, baseURL } = test.info().project.use
  const context = await browser.newContext({ viewport, userAgent, isMobile, hasTouch, deviceScaleFactor, locale, timezoneId, baseURL })
  return context.newPage()
}

test('complete Secret Santa journey from event creation to assignment reveal', async ({ page, browser }) => {
  const admin = newUser('Organizator')
  await registerAndVerify(page, admin)
  promoteToAdmin(admin.email)
  await login(page, admin)

  await page.goto('/admin')
  await page.getByRole('link', { name: 'Nowe wydarzenie' }).click()
  const eventName = `Wigilia KSM E2E ${Date.now()}`
  await page.getByLabel('Nazwa wydarzenia').fill(eventName)
  await page.getByLabel('Opis').fill('Opłatek, kolędy i wymiana prezentów.')
  await page.getByLabel('Miejsce').fill('Sala parafialna')
  await page.getByRole('button', { name: 'Utwórz wydarzenie' }).click()
  await expect(page.getByRole('heading', { name: eventName })).toBeVisible()

  await page.getByRole('button', { name: 'Otwórz zapisy' }).click()
  await expect(page.getByText('Zapisy otwarte').first()).toBeVisible()
  await page.getByRole('button', { name: 'Wygeneruj kod i QR' }).click()
  const code = (await page.getByTestId('invitation-code').textContent())?.trim() ?? ''
  expect(code).toMatch(/^KSM-[A-Z2-9]{5}-[A-Z2-9]{5}$/)
  await expect(page.getByAltText('Kod QR z linkiem do zapisów')).toBeVisible()

  const participants: TestUser[] = [newUser('Anna'), newUser('Piotr'), newUser('Karolina')]
  const pages: Page[] = []
  for (const [index, participant] of participants.entries()) {
    const participantPage = await newParticipantPage(browser)
    await participantPage.goto(`/join#${code}`)
    await expect(participantPage.getByRole('heading', { name: 'Masz zaproszenie!' })).toBeVisible()
    await participantPage.getByRole('button', { name: 'Załóż konto' }).click()
    await registerAndVerify(participantPage, participant)
    await login(participantPage, participant)

    await expect(participantPage.getByRole('heading', { name: eventName })).toBeVisible()
    await participantPage.getByRole('button', { name: 'Dołączam!' }).click()
    await expect(participantPage.getByRole('heading', { name: 'Jesteś zapisany(-a)!' })).toBeVisible()
    await participantPage.getByRole('link', { name: 'Uzupełnij listę prezentów' }).click()
    await expect(participantPage.getByRole('heading', { name: 'Moja lista prezentów' })).toBeVisible()

    if (index === 0) {
      const gift1 = participantPage.getByRole('article', { name: 'Prezent 1' })
      await gift1.getByRole('button', { name: 'Link' }).click()
      await gift1.getByLabel('Link do produktu').fill('javascript:alert(1)')
      await gift1.getByRole('button', { name: 'Zapisz' }).click()
      await expect(gift1.getByText('Podaj pełny adres zaczynający się od https://')).toBeVisible()
      await gift1.getByLabel('Link do produktu').fill('https://example.com/ksiazka-o-gwiazdach')
      await gift1.getByRole('button', { name: 'Zapisz' }).click()
      await expect(gift1.getByRole('link', { name: /example\.com/ })).toBeVisible()

      const gift2 = participantPage.getByRole('article', { name: 'Prezent 2' })
      await gift2.getByRole('button', { name: 'Opis' }).click()
      await gift2.getByLabel('Opis prezentu').fill('<b>Dobra</b> gra planszowa')
      await gift2.getByRole('button', { name: 'Zapisz' }).click()
      await expect(gift2.getByText('<b>Dobra</b> gra planszowa')).toBeVisible()
      await expect(participantPage.getByText('2/3')).toBeVisible()
    }
    await expectNoHorizontalScroll(participantPage)
    pages.push(participantPage)
  }

  await page.reload()
  await expect(page.getByText('Uczestnicy').first()).toBeVisible()
  await page.getByRole('button', { name: 'Zamknij zapisy' }).click()
  await expect(page.getByText('Zapisy zamknięte').first()).toBeVisible()
  await page.getByRole('button', { name: 'Przeprowadź losowanie' }).click()
  await page.getByRole('dialog').getByRole('button', { name: 'Losuj pary' }).click()
  await expect(page.getByText(/Wylosowano pary dla 3 osób/)).toBeVisible()
  await expect(page.getByRole('button', { name: 'Przeprowadź losowanie' })).toHaveCount(0)

  const admins = await apiAs(page)
  const eventId = new URL(page.url()).pathname.split('/').pop() ?? ''
  const secondDraw = await admins.post(`/api/admin/events/${eventId}/draw`, { confirmed: true })
  expect(secondDraw.status()).toBe(409)

  const revealedNames = new Set<string>()
  for (const [index, participant] of participants.entries()) {
    const participantPage = pages[index]
    if (!participantPage) {
      throw new Error('Missing participant page')
    }
    const mail = await waitForEmail(participantPage.request, participant.email, 'Twoje losowanie')
    expect(mail).toContain(`Cześć ${participant.firstName}!`)
    await participantPage.goto(firstLink(mail, '/assignment'))
    await participantPage.getByRole('button', { name: 'Dotknij, aby odsłonić' }).click()
    const recipient = (await participantPage.getByTestId('recipient-name').textContent())?.trim() ?? ''
    expect(recipient).not.toContain(participant.firstName)
    expect(mail).toContain(recipient)
    revealedNames.add(recipient)
    await expect.poll(async () => (await participantPage.request.get(`/api/events/${eventId}/assignment`)).json().then((body: { revealed: boolean }) => body.revealed)).toBe(true)
    await participantPage.reload()
    await expect(participantPage.getByTestId('recipient-name')).toHaveText(recipient)
    await expect(participantPage.getByRole('button', { name: 'Dotknij, aby odsłonić' })).toHaveCount(0)
    await expect(participantPage.locator('canvas')).toHaveCount(0)

    if (recipient.startsWith('Anna')) {
      const link = participantPage.getByRole('link', { name: /example\.com/ })
      await expect(link).toHaveAttribute('href', 'https://example.com/ksiazka-o-gwiazdach')
      await expect(link).toHaveAttribute('rel', /noopener/)
      await expect(link).toHaveAttribute('target', '_blank')
      await expect(participantPage.getByText('<b>Dobra</b> gra planszowa')).toBeVisible()
    } else {
      await expect(participantPage.getByText(/nie dodał\(-a\) jeszcze propozycji/)).toBeVisible()
    }
    await expectNoHorizontalScroll(participantPage)
  }
  expect(revealedNames.size).toBe(3)

  await page.getByRole('tab', { name: 'Powiadomienia' }).click()
  await expect(page.getByText('Wysłane', { exact: true })).toHaveCount(3)
  await logout(page)
})
