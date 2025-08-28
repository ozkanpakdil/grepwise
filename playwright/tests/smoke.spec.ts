import { test, expect } from '@playwright/test';

test.describe('App smoke', () => {
  test('home page loads and shows navigation', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveURL(/\/?$/);
    const anyInput = page.locator('input');
    await expect(anyInput.first()).toBeVisible({ timeout: 10_000 });
  });

  test('search route renders', async ({ page }) => {
    await page.goto('/search');
    const inputCandidates = [
      page.getByRole('textbox'),
      page.locator('textarea'),
      page.locator('input[type="text"]'),
    ];
    let hasEditor = false;
    for (const c of inputCandidates) {
      if (await c.first().isVisible().catch(() => false)) {
        hasEditor = true;
        break;
      }
    }
    expect(hasEditor).toBeTruthy();
  });
});
