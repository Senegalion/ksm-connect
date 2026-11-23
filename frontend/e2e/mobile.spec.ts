import { expect, test } from '@playwright/test'
import { expectNoHorizontalScroll, login, newUser, registerAndVerify } from './support'

test('public and participant pages fit narrow screens with comfortable touch targets', async ({ page }) => {
  for (const path of ['/', '/login', '/register', '/forgot-password', '/privacy', '/join']) {
    await page.goto(path)
    await expectNoHorizontalScroll(page)
  }
  await page.goto('/register')
  await expect(page.getByLabel('E-mail')).toHaveAttribute('inputmode', 'email')
  await expect(page.getByLabel('Numer telefonu')).toHaveAttribute('type', 'tel')

  const user = newUser('Mobilny')
  await registerAndVerify(page, user)
  await login(page, user)
  await expectNoHorizontalScroll(page)

  const bottomNav = page.getByRole('navigation', { name: 'Nawigacja dolna' })
  await expect(bottomNav).toBeVisible()
  for (const link of await bottomNav.getByRole('link').all()) {
    const box = await link.boundingBox()
    expect(box?.height ?? 0).toBeGreaterThanOrEqual(44)
  }
  await bottomNav.getByRole('link', { name: 'Dołącz' }).click()
  await expect(page.getByLabel('Kod zaproszenia')).toBeVisible()
  await expectNoHorizontalScroll(page)
  await bottomNav.getByRole('link', { name: 'Konto' }).click()
  await expect(page.getByRole('heading', { name: 'Konto' })).toBeVisible()
  await expectNoHorizontalScroll(page)
})
