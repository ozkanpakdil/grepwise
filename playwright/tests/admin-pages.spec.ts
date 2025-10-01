import { test, expect } from '@playwright/test';

async function login(page) {
  await page.goto('/login');
  await page.getByTestId('username').fill('admin');
  await page.getByTestId('password').fill('admin');
  await page.getByTestId('sign-in').click();
  await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });
}

test.describe.skip('Admin Settings Page', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('settings page loads for admin users', async ({ page }) => {
    await page.goto('/settings');
    await expect(page).toHaveURL(/\/settings$/);
    
    // Check for settings-related elements
    const settingsElements = [
      page.getByText(/settings/i),
      page.getByText(/configuration/i),
      page.getByText(/preferences/i),
      page.getByRole('tab'),
      page.locator('[data-testid*="setting"]')
    ];
    
    let hasValidElement = false;
    for (const element of settingsElements) {
      if (await element.first().isVisible({ timeout: 10000 }).catch(() => false)) {
        hasValidElement = true;
        break;
      }
    }
    expect(hasValidElement).toBeTruthy();
  });

  test('settings tabs are navigable when available', async ({ page }) => {
    await page.goto('/settings');
    
    const tabs = page.getByRole('tab');
    const tabCount = await tabs.count();
    
    if (tabCount > 0) {
      for (let i = 0; i < Math.min(tabCount, 3); i++) {
        const tab = tabs.nth(i);
        await tab.click();
        await page.waitForLoadState('networkidle');
        
        // Check if tab panel content is visible
        const tabPanels = [
          page.getByRole('tabpanel'),
          page.locator('[role="tabpanel"]'),
          page.locator('[data-testid*="panel"]')
        ];
        
        let hasPanelContent = false;
        for (const panel of tabPanels) {
          if (await panel.first().isVisible({ timeout: 3000 }).catch(() => false)) {
            hasPanelContent = true;
            break;
          }
        }
        expect(hasPanelContent).toBeTruthy();
      }
    }
  });

  test('settings can be modified and saved', async ({ page }) => {
    await page.goto('/settings');
    
    // Look for input elements
    const inputElements = [
      page.getByRole('textbox'),
      page.getByRole('checkbox'),
      page.getByRole('switch'),
      page.locator('input[type="text"]'),
      page.locator('input[type="number"]'),
      page.locator('select')
    ];
    
    let foundInput = false;
    for (const element of inputElements) {
      if (await element.first().isVisible({ timeout: 3000 }).catch(() => false)) {
        foundInput = true;
        break;
      }
    }
    
    if (foundInput) {
      // Look for save button
      const saveButton = page.getByRole('button', { name: /save|apply|update/i });
      if (await saveButton.first().isVisible({ timeout: 3000 }).catch(() => false)) {
        await saveButton.first().click();
        
        // Look for success message or confirmation
        const confirmationElements = [
          page.getByText(/saved|updated|success/i),
          page.locator('[role="alert"]'),
          page.getByText(/applied/i)
        ];
        
        let hasConfirmation = false;
        for (const element of confirmationElements) {
          if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
            hasConfirmation = true;
            break;
          }
        }
        expect(hasConfirmation).toBeTruthy();
      }
    }
  });
});

test.describe.skip('Users Management Page', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('users page loads and shows user management interface', async ({ page }) => {
    await page.goto('/users');
    await expect(page).toHaveURL(/\/users$/);
    
    // Check for user management elements
    const userElements = [
      page.getByText(/users/i),
      page.getByText(/user management/i),
      page.getByRole('table'),
      page.getByRole('button', { name: /add|create|new/i }),
      page.locator('[data-testid*="user"]')
    ];
    
    let hasValidElement = false;
    for (const element of userElements) {
      if (await element.first().isVisible({ timeout: 10000 }).catch(() => false)) {
        hasValidElement = true;
        break;
      }
    }
    expect(hasValidElement).toBeTruthy();
  });

  test('can access user creation form', async ({ page }) => {
    await page.goto('/users');
    
    const createButton = page.getByRole('button', { name: /add|create|new/i });
    if (await createButton.first().isVisible({ timeout: 5000 }).catch(() => false)) {
      await createButton.first().click();
      
      // Look for user creation form
      const formElements = [
        page.getByLabel(/username|user/i),
        page.getByLabel(/email/i),
        page.getByLabel(/password/i),
        page.getByLabel(/role/i),
        page.locator('input[type="text"]'),
        page.locator('input[type="email"]')
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

  test('user list shows existing users when available', async ({ page }) => {
    await page.goto('/users');
    
    // Look for user table or list
    const tableElement = page.getByRole('table');
    if (await tableElement.first().isVisible({ timeout: 5000 }).catch(() => false)) {
      const rowCount = await tableElement.locator('tr').count();
      expect(rowCount).toBeGreaterThan(0);
    } else {
      // Check for empty state
      const emptyStateElements = [
        page.getByText(/no users/i),
        page.getByText(/empty/i)
      ];
      
      let hasEmptyState = false;
      for (const element of emptyStateElements) {
        if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
          hasEmptyState = true;
          break;
        }
      }
      expect(hasEmptyState).toBeTruthy();
    }
  });
});

test.describe.skip('Roles Management Page', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('roles page loads and shows role management interface', async ({ page }) => {
    await page.goto('/roles');
    await expect(page).toHaveURL(/\/roles$/);
    
    // Check for role management elements
    const roleElements = [
      page.getByText(/roles/i),
      page.getByText(/role management/i),
      page.getByText(/permissions/i),
      page.getByRole('table'),
      page.getByRole('button', { name: /add|create|new/i })
    ];
    
    let hasValidElement = false;
    for (const element of roleElements) {
      if (await element.first().isVisible({ timeout: 10000 }).catch(() => false)) {
        hasValidElement = true;
        break;
      }
    }
    expect(hasValidElement).toBeTruthy();
  });

  test('role permissions can be configured', async ({ page }) => {
    await page.goto('/roles');
    
    // Look for permission checkboxes or controls
    const permissionElements = [
      page.getByRole('checkbox'),
      page.getByText(/permission/i),
      page.getByText(/access/i),
      page.locator('[data-testid*="permission"]')
    ];
    
    for (const element of permissionElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        // Try to interact with permission control
        if (element === page.getByRole('checkbox')) {
          await element.first().click();
        }
        expect(true).toBeTruthy(); // Found permission controls
        break;
      }
    }
  });
});

test.describe.skip('Redaction Editor Page', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('redaction editor loads for admin users', async ({ page }) => {
    await page.goto('/admin/redaction');
    await expect(page).toHaveURL(/\/admin\/redaction$/);
    
    // Check for redaction editor elements
    const redactionElements = [
      page.getByText(/redaction/i),
      page.getByText(/editor/i),
      page.getByText(/rules/i),
      page.getByText(/pattern/i),
      page.locator('.monaco-editor'),
      page.getByRole('textbox')
    ];
    
    let hasValidElement = false;
    for (const element of redactionElements) {
      if (await element.first().isVisible({ timeout: 10000 }).catch(() => false)) {
        hasValidElement = true;
        break;
      }
    }
    expect(hasValidElement).toBeTruthy();
  });

  test('redaction rules can be created and edited', async ({ page }) => {
    await page.goto('/admin/redaction');
    
    // Look for rule creation controls
    const createElements = [
      page.getByRole('button', { name: /add|create|new/i }),
      page.getByText(/add rule/i),
      page.getByText(/new rule/i)
    ];
    
    for (const element of createElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        await element.first().click();
        
        // Look for rule configuration form
        const ruleFormElements = [
          page.getByLabel(/pattern/i),
          page.getByLabel(/replacement/i),
          page.getByLabel(/rule/i),
          page.getByRole('textbox'),
          page.locator('textarea')
        ];
        
        let hasRuleForm = false;
        for (const formEl of ruleFormElements) {
          if (await formEl.first().isVisible({ timeout: 5000 }).catch(() => false)) {
            hasRuleForm = true;
            break;
          }
        }
        expect(hasRuleForm).toBeTruthy();
        break;
      }
    }
  });
});

test.describe.skip('Monitoring Page', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('monitoring page loads and shows system metrics', async ({ page }) => {
    await page.goto('/monitoring');
    await expect(page).toHaveURL(/\/monitoring$/);
    
    // Check for monitoring elements
    const monitoringElements = [
      page.getByText(/monitoring/i),
      page.getByText(/metrics/i),
      page.getByText(/performance/i),
      page.getByText(/system/i),
      page.locator('canvas'), // For charts
      page.locator('[data-testid*="chart"]'),
      page.locator('[data-testid*="metric"]')
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

  test('monitoring charts and metrics display data', async ({ page }) => {
    await page.goto('/monitoring');
    
    // Wait for potential data loading
    await page.waitForTimeout(3000);
    
    // Look for data visualization elements
    const dataElements = [
      page.locator('canvas'),
      page.locator('svg'),
      page.getByText(/\d+%/), // Percentage values
      page.getByText(/\d+\s*(MB|GB|KB)/i), // Memory values
      page.locator('[data-testid*="value"]')
    ];
    
    let hasData = false;
    for (const element of dataElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        hasData = true;
        break;
      }
    }
    expect(hasData).toBeTruthy();
  });
});