import { test, expect } from '@playwright/test';

// Helper: perform login using default credentials from UI
async function login(page) {
  await page.goto('/login');
  // Fill username and password (defaults admin/admin)
  await page.getByTestId('username').fill('admin');
  await page.getByTestId('password').fill('admin');
  await page.getByTestId('sign-in').click();
  // Wait for auth state to be applied: URL no longer /login and logout button visible
  await expect(page).not.toHaveURL(/\/login(\b|$)/);
  await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20000 });
}

// Runs on each browser project due to config
test.describe('Login then Search flow', () => {
  test('can login and run a basic search', async ({ page }) => {
    await login(page);

    // Navigate to search route explicitly to be safe
    await page.goto('/search', { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle');

    // Verify search form visible (allow more time for Monaco + hydration)
    const form = page.getByTestId('search-form');
    await expect(form).toBeVisible({ timeout: 20000 });
    // Ensure Monaco editor DOM mounted
    await page.locator('.monaco-editor').first().waitFor({ timeout: 20000 });

    // Enter a simple query into Monaco editor. Monaco isn't a normal textarea; type into the editor container.
    const editor = page.getByTestId('query-editor');
    await expect(editor).toBeVisible();

    // Click into the monaco editor and type
    await editor.click();
    await page.keyboard.type('test');

    // Select time range to 1h to avoid custom date pickers
    await page.getByTestId('time-range').click();
    await page.getByRole('option', { name: /Last 1 hour/i }).click();

    // Set page size small to make pagination easier if needed
    const pageSize = page.getByTestId('page-size');
    await pageSize.fill('50');

    // Run search
    await page.getByTestId('run-search').click();

    // Wait until search finishes: histogram or empty state should appear; results may or may not.
    await expect(page.getByTestId('histogram-section')).toBeVisible({ timeout: 20000 });

    const resultsSection = page.getByTestId('results-section');
    if (await resultsSection.isVisible().catch(() => false)) {
      // Results exist: assert summary and optionally expand a row
      await expect(page.getByTestId('results-summary')).toBeVisible();

      const firstRow = page.getByTestId('result-row').first();
      if (await firstRow.isVisible().catch(() => false)) {
        await firstRow.click();
        await expect(page.getByTestId('result-row-expanded')).toBeVisible();
      }
    } else {
      // No results: assert empty-state message to confirm search completed
      await expect(page.getByText('No logs found matching your query')).toBeVisible();
    }
  });

  test('filters and sorting controls are interactable after login', async ({ page }) => {
    await login(page);
    await page.goto('/search', { waitUntil: 'domcontentloaded' });
    await page.waitForLoadState('networkidle');

    // Ensure editor ready, then run a quick search
    await page.locator('.monaco-editor').first().waitFor({ timeout: 20000 });
    await page.getByTestId('run-search').click();

    // If results exist, results-section will be visible; otherwise filters panel toggle may not be present.
    const resultsHeader = page.getByTestId('results-section');
    if (await resultsHeader.isVisible().catch(() => false)) {
      const toggle = page.getByTestId('toggle-filters');
      await expect(toggle).toBeVisible();
      await toggle.click();

      await expect(page.getByTestId('filters-panel')).toBeVisible();
      await page.getByTestId('filter-level').fill('INFO');
      await page.getByTestId('filter-message').fill('test');
      await page.getByTestId('filter-source').fill('app');

      await page.getByTestId('col-level').click();
      await page.getByTestId('col-message').click();
    } else {
      // No results case: still assert histogram to ensure page is stable
      await expect(page.getByTestId('histogram-section')).toBeVisible();
    }
  });
  test('logout and redirect to login, then re-login', async ({ page }) => {
    await login(page);

    // Click logout button in header
    const logoutBtn = page.getByTestId('logout');
    await expect(logoutBtn).toBeVisible();
    await logoutBtn.click();

    // Should be redirected to /login
    await expect(page).toHaveURL(/\/login/);

    // Login again to ensure session resets correctly
    await page.getByTestId('username').fill('admin');
    await page.getByTestId('password').fill('admin');
    await page.getByTestId('sign-in').click();
    await expect(page).toHaveURL(/\/?$/);
  });
});
