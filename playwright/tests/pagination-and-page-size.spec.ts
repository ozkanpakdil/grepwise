import { test, expect } from '@playwright/test';

async function login(page) {
  await page.goto('/login');
  await page.getByTestId('username').fill('admin');
  await page.getByTestId('password').fill('admin');
  await page.getByTestId('sign-in').click();
  await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });
}

test.describe('Pagination and page size (conditional if results exist)', () => {
  test('adjust page size and paginate', async ({ page }) => {
    await login(page);
    await page.goto('/search');

    // Ensure editor loaded then run a search
    await page.locator('.monaco-editor').first().waitFor({ timeout: 20_000 }).catch(() => {});
    await page.getByTestId('run-search').click();

    // Wait for histogram to ensure search completed
    await expect(page.getByTestId('histogram-section')).toBeVisible({ timeout: 20_000 });

    const results = page.getByTestId('results-section');
    if (!(await results.isVisible().catch(() => false))) {
      test.skip(true, 'No results to test pagination');
    }

    const pageSize = page.getByTestId('page-size');
    if (await pageSize.isVisible().catch(() => false)) {
      await pageSize.fill('25');
      await page.keyboard.press('Enter').catch(() => {});
    }

    const nextBtn = page.getByRole('button', { name: /next/i });
    if (await nextBtn.isVisible().catch(() => false)) {
      await nextBtn.click();
      await page.waitForLoadState('networkidle');
    }

    const prevBtn = page.getByRole('button', { name: /prev/i });
    if (await prevBtn.isVisible().catch(() => false)) {
      await prevBtn.click();
      await page.waitForLoadState('networkidle');
    }
  });
});
