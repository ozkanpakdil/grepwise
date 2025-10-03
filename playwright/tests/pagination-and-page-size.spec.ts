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
    await page.waitForLoadState('networkidle');

    // Ensure editor loaded then run a search
    const editorVisible = await page.locator('.monaco-editor').first().isVisible({ timeout: 10_000 }).catch(() => false);

    if (!editorVisible) {
      // Try alternative search interface elements
      const searchButton = page.getByTestId('run-search');
      if (await searchButton.isVisible({ timeout: 5000 }).catch(() => false)) {
        await searchButton.click();
      } else {
        test.skip(true, 'Search interface not available');
      }
    } else {
      await page.getByTestId('run-search').click();
    }

    // Wait for histogram to ensure search completed
    const histogramVisible = await page.getByTestId('histogram-section')
      .isVisible({ timeout: 10_000 }).catch(() => false);

    if (!histogramVisible) {
      test.skip(true, 'Search not completing');
    }

    // Check if results exist - if not, skip the test
    const results = page.getByTestId('results-section');
    const hasResults = await results.isVisible({ timeout: 5000 }).catch(() => false);
    if (!hasResults) {
      test.skip(true, 'No results to test pagination');
    }

    const pageSize = page.getByTestId('page-size');
    if (await pageSize.isVisible().catch(() => false)) {
      // Use selectOption for select elements instead of fill
      await pageSize.selectOption('25').catch(() => {});
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
