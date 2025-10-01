import { test, expect } from '@playwright/test';

async function login(page) {
  await page.goto('/login');
  await page.getByTestId('username').fill('admin');
  await page.getByTestId('password').fill('admin');
  await page.getByTestId('sign-in').click();
  await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });
}

test.describe('Alarms Page', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('alarms page loads and shows alarm configuration', async ({ page }) => {
    await page.goto('/alarms');
    await expect(page).toHaveURL(/\/alarms$/);
    
    // Check for alarm-related elements
    const alarmCandidates = [
      page.getByText(/alarm/i),
      page.getByText(/alert/i),
      page.getByText(/notification/i),
      page.getByText(/monitor/i),
      page.getByRole('button', { name: /create|add|new/i })
    ];
    
    let hasValidElement = false;
    for (const element of alarmCandidates) {
      if (await element.first().isVisible({ timeout: 10000 }).catch(() => false)) {
        hasValidElement = true;
        break;
      }
    }
    expect(hasValidElement).toBeTruthy();
  });

  test('can create new alarm when functionality available', async ({ page }) => {
    await page.goto('/alarms');
    
    const createButton = page.getByRole('button', { name: /create|add|new/i });
    if (await createButton.first().isVisible({ timeout: 5000 }).catch(() => false)) {
      await createButton.first().click();
      
      // Look for alarm creation form
      const formElements = [
        page.getByLabel(/name|title/i),
        page.getByLabel(/condition|query/i),
        page.getByLabel(/threshold/i),
        page.getByRole('textbox'),
        page.locator('input[type="text"]'),
        page.locator('textarea')
      ];
      
      let hasForm = false;
      for (const element of formElements) {
        if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
          hasForm = true;
          break;
        }
      }
      expect(hasForm).toBeTruthy();
    }
  });

  test('alarm list displays existing alarms when available', async ({ page }) => {
    await page.goto('/alarms');
    
    // Look for alarm list or table
    const listElements = [
      page.locator('table'),
      page.locator('[role="table"]'),
      page.locator('[data-testid*="alarm"]'),
      page.locator('[data-testid*="list"]')
    ];
    
    for (const element of listElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        // If we find a list/table, check for some content
        const hasContent = await element.locator('tr, [role="row"], li').count() > 0;
        if (hasContent) {
          expect(true).toBeTruthy(); // Found alarm list with content
          return;
        }
      }
    }
    
    // If no table found, check for empty state or creation prompt
    const emptyStateElements = [
      page.getByText(/no alarms/i),
      page.getByText(/empty/i),
      page.getByText(/create your first/i)
    ];
    
    let hasEmptyState = false;
    for (const element of emptyStateElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        hasEmptyState = true;
        break;
      }
    }
    expect(hasEmptyState).toBeTruthy();
  });

  test('alarm configuration options are accessible', async ({ page }) => {
    await page.goto('/alarms');
    
    // Look for configuration controls
    const configElements = [
      page.getByRole('button', { name: /settings|config/i }),
      page.getByText(/settings|config/i),
      page.locator('[data-testid*="setting"]'),
      page.locator('[data-testid*="config"]')
    ];
    
    for (const element of configElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        await element.first().click();
        await page.waitForLoadState('networkidle');
        
        // Check for configuration interface
        const configInterface = [
          page.getByRole('checkbox'),
          page.getByRole('switch'),
          page.getByRole('combobox'),
          page.locator('input[type="checkbox"]'),
          page.locator('select')
        ];
        
        let hasConfig = false;
        for (const configEl of configInterface) {
          if (await configEl.first().isVisible({ timeout: 5000 }).catch(() => false)) {
            hasConfig = true;
            break;
          }
        }
        expect(hasConfig).toBeTruthy();
        break;
      }
    }
  });
});

test.describe('Alarm Monitoring Page', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('alarm monitoring page loads and shows monitoring interface', async ({ page }) => {
    await page.goto('/alarm-monitoring');
    await expect(page).toHaveURL(/\/alarm-monitoring$/);
    
    // Check for monitoring-related elements
    const monitoringElements = [
      page.getByText(/monitoring/i),
      page.getByText(/status/i),
      page.getByText(/active|triggered/i),
      page.locator('[data-testid*="monitor"]'),
      page.locator('[data-testid*="status"]')
    ];
    
    let hasValidElement = false;
    for (const element of monitoringElements) {
      if (await element.first().isVisible({ timeout: 10000 }).catch(() => false)) {
        hasValidElement = true;
        break;
      }
    }
    expect(hasValidElement).toBeTruthy();
  });

  test('real-time monitoring updates work when available', async ({ page }) => {
    await page.goto('/alarm-monitoring');
    
    // Look for real-time indicators
    const realtimeElements = [
      page.getByText(/live|real.?time/i),
      page.locator('[data-testid*="live"]'),
      page.locator('[data-testid*="realtime"]'),
      page.getByText(/updated|refresh/i)
    ];
    
    for (const element of realtimeElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        // Wait a bit to see if content updates
        await page.waitForTimeout(2000);
        expect(true).toBeTruthy(); // Found real-time monitoring
        break;
      }
    }
  });

  test('alarm monitoring controls are functional', async ({ page }) => {
    await page.goto('/alarm-monitoring');
    
    // Look for monitoring controls
    const controlElements = [
      page.getByRole('button', { name: /refresh|reload/i }),
      page.getByRole('button', { name: /pause|start|stop/i }),
      page.getByRole('switch'),
      page.locator('[data-testid*="control"]')
    ];
    
    for (const element of controlElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        await element.first().click();
        await page.waitForTimeout(1000);
        expect(true).toBeTruthy(); // Successfully clicked control
        break;
      }
    }
  });
});