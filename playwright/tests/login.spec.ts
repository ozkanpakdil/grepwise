import { test, expect } from '@playwright/test';

test.describe('Login', () => {
  test('valid credentials login and logout', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('username').fill('admin');
    await page.getByTestId('password').fill('admin');
    await page.getByTestId('sign-in').click();
    await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });

    await page.getByTestId('logout').click();
    await expect(page).toHaveURL(/\/login/);
  });

  test('invalid credentials show error (best effort)', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('username').fill('admin');
    await page.getByTestId('password').fill('wrong-password');
    await page.getByTestId('sign-in').click();

    // Try common error patterns without asserting exact text to keep robust
    const possibleErrors = [
      page.getByRole('alert'),
      page.getByText(/invalid|failed|unauthorized|error/i),
    ];
    let sawError = false;
    for (const e of possibleErrors) {
      if (await e.first().isVisible({ timeout: 3000 }).catch(() => false)) {
        sawError = true;
        break;
      }
    }
    expect(sawError).toBeTruthy();
  });
});
