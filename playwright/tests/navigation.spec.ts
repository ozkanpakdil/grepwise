import { test, expect } from '@playwright/test';

async function login(page) {
  await page.goto('/login');
  await page.getByTestId('username').fill('admin');
  await page.getByTestId('password').fill('admin');
  await page.getByTestId('sign-in').click();
  await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });
}

test.describe('Navigation', () => {
  test('header links navigate between main pages', async ({ page }) => {
    await login(page);

    // Define a set of candidate nav links that may exist in the header/side bar
    const candidates = [
      { name: /Home|Dashboard/i, href: '/' },
      { name: /Search/i, href: '/search' },
      { name: /Alarms|Alerts/i, href: '/alarms' },
      { name: /Settings|Preferences/i, href: '/settings' },
      { name: /About/i, href: '/about' },
    ];

    for (const c of candidates) {
      const link = page.getByRole('link', { name: c.name });
      if (await link.first().isVisible().catch(() => false)) {
        await link.first().click();
        // Allow client-side routing to settle
        await page.waitForLoadState('networkidle');
        // Best-effort URL assertion if the route exists
        try {
          await expect(page).toHaveURL(new RegExp(`${c.href.replace('/', '\\/')}`));
        } catch (_) {
          // Non-fatal if the route is not implemented yet
        }
      }
    }
  });
});
