import { test, expect } from '@playwright/test';

async function login(page) {
  await page.goto('/login');
  await page.getByTestId('username').fill('admin');
  await page.getByTestId('password').fill('admin');
  await page.getByTestId('sign-in').click();
  await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });
}

test.describe('Dashboards Page', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('dashboards page loads and shows dashboard list', async ({ page }) => {
    await page.goto('/dashboards');
    await page.waitForLoadState('domcontentloaded');
    expect(page.url()).toContain('/dashboards');

    // Check that we have navigation
    const hasNav = await page.locator('[data-testid="logout"]').isVisible({ timeout: 10000 }).catch(() => false);
    expect(hasNav).toBeTruthy();
  });

  test('can navigate to dashboard creation if available', async ({ page }) => {
    await page.goto('/dashboards');
    
    const createButton = page.getByRole('button', { name: /create|add|new/i });
    if (await createButton.first().isVisible({ timeout: 5000 }).catch(() => false)) {
      await createButton.first().click();
      
      // Check for form elements that might appear
      const formCandidates = [
        page.getByRole('textbox'),
        page.locator('input'),
        page.locator('form'),
        page.getByText(/name|title/i)
      ];
      
      let hasForm = false;
      for (const element of formCandidates) {
        if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
          hasForm = true;
          break;
        }
      }
      expect(hasForm).toBeTruthy();
    }
  });

  test('individual dashboard view loads when available', async ({ page }) => {
    await page.goto('/dashboards');
    
    // Look for existing dashboard links
    const dashboardLinks = page.locator('[href*="/dashboards/"]').or(
      page.getByRole('link').filter({ hasText: /dashboard/i })
    );
    
    const count = await dashboardLinks.count();
    if (count > 0) {
      await dashboardLinks.first().click();
      await page.waitForLoadState('networkidle');
      
      // Verify we're on a dashboard view page
      expect(page.url()).toMatch(/\/dashboards\/[\w-]+/);
      
      // Check for dashboard-specific elements
      const dashboardElements = [
        page.getByText(/widget/i),
        page.getByText(/chart/i),
        page.locator('[data-testid*="widget"]'),
        page.locator('[data-testid*="chart"]')
      ];
      
      let hasValidDashboard = false;
      for (const element of dashboardElements) {
        if (await element.first().isVisible({ timeout: 10000 }).catch(() => false)) {
          hasValidDashboard = true;
          break;
        }
      }
      expect(hasValidDashboard).toBeTruthy();
    }
  });

  test('dashboard editing controls work when available', async ({ page }) => {
    await page.goto('/dashboards');
    
    // Look for edit buttons or options
    const editElements = [
      page.getByRole('button', { name: /edit/i }),
      page.getByText(/edit/i),
      page.locator('[data-testid*="edit"]')
    ];
    
    for (const element of editElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        await element.first().click();
        await page.waitForLoadState('networkidle');
        
        // Check for editing interface
        const editingElements = [
          page.getByRole('button', { name: /save|update/i }),
          page.getByRole('textbox'),
          page.locator('input[type="text"]')
        ];
        
        let hasEditInterface = false;
        for (const editEl of editingElements) {
          if (await editEl.first().isVisible({ timeout: 5000 }).catch(() => false)) {
            hasEditInterface = true;
            break;
          }
        }
        expect(hasEditInterface).toBeTruthy();
        break;
      }
    }
  });
});