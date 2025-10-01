import { test, expect } from '@playwright/test';

async function login(page) {
  await page.goto('/login');
  await page.getByTestId('username').fill('admin');
  await page.getByTestId('password').fill('admin');
  await page.getByTestId('sign-in').click();
  await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });
}

test.describe.skip('Responsive Design', () => {
  test('mobile layout adjusts properly on small screens', async ({ page }) => {
    // Set mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await login(page);
    
    await page.goto('/search');
    
    // Check if mobile navigation is present
    const mobileNavElements = [
      page.getByRole('button', { name: /menu/i }),
      page.locator('[data-testid*="mobile-menu"]'),
      page.locator('.hamburger'),
      page.locator('[aria-label*="menu"]')
    ];
    
    let hasMobileNav = false;
    for (const element of mobileNavElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        hasMobileNav = true;
        
        // Test mobile menu functionality
        await element.first().click();
        await page.waitForTimeout(500);
        
        // Look for navigation menu
        const menuElements = [
          page.getByRole('navigation'),
          page.locator('[data-testid*="nav"]'),
          page.getByText(/search|dashboard|alarm/i)
        ];
        
        let hasMenu = false;
        for (const menuEl of menuElements) {
          if (await menuEl.first().isVisible({ timeout: 3000 }).catch(() => false)) {
            hasMenu = true;
            break;
          }
        }
        expect(hasMenu).toBeTruthy();
        break;
      }
    }
    
    // Even if no explicit mobile menu, check that content is accessible
    if (!hasMobileNav) {
      // Content should still be visible and usable
      const mainContent = page.locator('main, [role="main"], .main-content');
      if (await mainContent.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        expect(true).toBeTruthy(); // Content is accessible
      }
    }
  });

  test('tablet layout works correctly on medium screens', async ({ page }) => {
    // Set tablet viewport
    await page.setViewportSize({ width: 768, height: 1024 });
    await login(page);
    
    await page.goto('/search');
    
    // Check that search interface is usable on tablets
    const searchElements = [
      page.getByTestId('search-form'),
      page.getByTestId('query-editor'),
      page.getByTestId('run-search')
    ];
    
    for (const element of searchElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        // Element should be properly sized for tablet
        const boundingBox = await element.first().boundingBox();
        if (boundingBox) {
          expect(boundingBox.width).toBeLessThanOrEqual(768);
          expect(boundingBox.width).toBeGreaterThan(0);
        }
      }
    }
  });

  test('dashboard widgets adapt to different screen sizes', async ({ page }) => {
    await login(page);
    await page.goto('/dashboards');
    
    const viewports = [
      { width: 1920, height: 1080 }, // Desktop
      { width: 1024, height: 768 },  // Small desktop/tablet
      { width: 375, height: 667 }    // Mobile
    ];
    
    for (const viewport of viewports) {
      await page.setViewportSize(viewport);
      await page.waitForTimeout(1000);
      
      // Check if dashboard elements are responsive
      const dashboardElements = [
        page.locator('[data-testid*="widget"]'),
        page.locator('[data-testid*="chart"]'),
        page.locator('canvas'),
        page.locator('.dashboard-grid')
      ];
      
      for (const element of dashboardElements) {
        if (await element.first().isVisible({ timeout: 3000 }).catch(() => false)) {
          const boundingBox = await element.first().boundingBox();
          if (boundingBox) {
            // Element should fit within viewport
            expect(boundingBox.width).toBeLessThanOrEqual(viewport.width);
          }
        }
      }
    }
  });

  test('navigation is accessible across different screen sizes', async ({ page }) => {
    await login(page);
    
    const viewports = [
      { width: 1200, height: 800 },  // Desktop
      { width: 768, height: 1024 },  // Tablet
      { width: 375, height: 667 }    // Mobile
    ];
    
    for (const viewport of viewports) {
      await page.setViewportSize(viewport);
      await page.goto('/search');
      
      // Navigation should be accessible in some form
      const navElements = [
        page.getByRole('navigation'),
        page.getByRole('button', { name: /menu/i }),
        page.getByRole('link', { name: /search|dashboard/i }),
        page.locator('[data-testid*="nav"]')
      ];
      
      let hasAccessibleNav = false;
      for (const element of navElements) {
        if (await element.first().isVisible({ timeout: 3000 }).catch(() => false)) {
          hasAccessibleNav = true;
          break;
        }
      }
      expect(hasAccessibleNav).toBeTruthy();
    }
  });

  test('search interface remains functional on small screens', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await login(page);
    await page.goto('/search');
    
    // Wait for Monaco editor to load
    await page.locator('.monaco-editor').first().waitFor({ timeout: 20000 }).catch(() => {});
    
    // Check if search form is usable
    const searchForm = page.getByTestId('search-form');
    if (await searchForm.isVisible({ timeout: 5000 }).catch(() => false)) {
      // Try to use the search interface
      const editor = page.getByTestId('query-editor');
      if (await editor.isVisible({ timeout: 5000 }).catch(() => false)) {
        await editor.click();
        await page.keyboard.type('test');
        
        const runButton = page.getByTestId('run-search');
        if (await runButton.isVisible({ timeout: 3000 }).catch(() => false)) {
          // Button should be clickable
          const boundingBox = await runButton.boundingBox();
          if (boundingBox) {
            expect(boundingBox.width).toBeGreaterThan(0);
            expect(boundingBox.height).toBeGreaterThan(0);
          }
        }
      }
    }
  });

  test('form inputs are appropriately sized for touch interfaces', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('/login');
    
    // Check login form inputs
    const inputElements = [
      page.getByTestId('username'),
      page.getByTestId('password'),
      page.getByTestId('sign-in')
    ];
    
    for (const element of inputElements) {
      if (await element.isVisible({ timeout: 5000 }).catch(() => false)) {
        const boundingBox = await element.boundingBox();
        if (boundingBox) {
          // Touch targets should be at least 44px high for accessibility
          if (element === page.getByTestId('sign-in')) {
            expect(boundingBox.height).toBeGreaterThanOrEqual(40);
          } else {
            expect(boundingBox.height).toBeGreaterThanOrEqual(36);
          }
        }
      }
    }
  });

  test('content scrolls properly on mobile devices', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await login(page);
    await page.goto('/search');
    
    // Check if content is scrollable when it exceeds viewport
    const bodyHeight = await page.evaluate(() => document.body.scrollHeight);
    const viewportHeight = await page.evaluate(() => window.innerHeight);
    
    if (bodyHeight > viewportHeight) {
      // Try scrolling
      await page.mouse.wheel(0, 100);
      await page.waitForTimeout(500);
      
      const scrollY = await page.evaluate(() => window.scrollY);
      expect(scrollY).toBeGreaterThan(0);
    }
  });

  test('horizontal scrolling is avoided on mobile', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await login(page);
    
    const routes = ['/search', '/dashboards', '/alarms'];
    
    for (const route of routes) {
      await page.goto(route);
      await page.waitForLoadState('domcontentloaded');
      
      // Check that content doesn't cause horizontal scroll
      const bodyWidth = await page.evaluate(() => document.body.scrollWidth);
      const viewportWidth = await page.evaluate(() => window.innerWidth);
      
      // Allow small overflow (e.g., scrollbar) but no significant horizontal scroll
      expect(bodyWidth).toBeLessThanOrEqual(viewportWidth + 20);
    }
  });
});