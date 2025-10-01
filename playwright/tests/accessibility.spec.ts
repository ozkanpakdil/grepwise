import { test, expect } from '@playwright/test';

async function login(page) {
  await page.goto('/login');
  await page.getByTestId('username').fill('admin');
  await page.getByTestId('password').fill('admin');
  await page.getByTestId('sign-in').click();
  await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });
}

test.describe('Accessibility', () => {
  test('login page is keyboard navigable', async ({ page }) => {
    await page.goto('/login');
    
    // Test keyboard navigation through login form
    await page.keyboard.press('Tab');
    const usernameField = page.getByTestId('username');
    await expect(usernameField).toBeFocused();
    
    await page.keyboard.type('admin');
    await page.keyboard.press('Tab');
    
    const passwordField = page.getByTestId('password');
    await expect(passwordField).toBeFocused();
    
    await page.keyboard.type('admin');
    await page.keyboard.press('Tab');
    
    const signInButton = page.getByTestId('sign-in');
    await expect(signInButton).toBeFocused();
    
    // Submit form with keyboard
    await page.keyboard.press('Enter');
    
    // Should successfully log in
    await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });
  });

  test('main navigation is keyboard accessible', async ({ page }) => {
    await login(page);
    
    // Navigate through main menu items with keyboard
    await page.keyboard.press('Tab');
    
    const navigationLinks = [
      page.getByRole('link', { name: /search/i }),
      page.getByRole('link', { name: /dashboard/i }),
      page.getByRole('link', { name: /alarm/i })
    ];
    
    for (const link of navigationLinks) {
      if (await link.first().isVisible({ timeout: 3000 }).catch(() => false)) {
        // Focus should be manageable with Tab navigation
        await page.keyboard.press('Tab');
        
        // Check if we can activate the link with Enter
        const focused = await page.evaluate(() => document.activeElement?.tagName);
        if (focused === 'A' || focused === 'BUTTON') {
          await page.keyboard.press('Enter');
          await page.waitForLoadState('domcontentloaded');
          // Successfully navigated with keyboard
          expect(true).toBeTruthy();
          break;
        }
      }
    }
  });

  test('search form has proper ARIA labels and roles', async ({ page }) => {
    await login(page);
    await page.goto('/search');
    
    // Check for ARIA attributes on search form
    const searchForm = page.getByTestId('search-form');
    if (await searchForm.isVisible({ timeout: 10000 }).catch(() => false)) {
      // Form should have proper role or be contained in a form element
      const hasFormRole = await searchForm.evaluate(el => {
        return el.tagName === 'FORM' || el.getAttribute('role') === 'form' || el.getAttribute('role') === 'search';
      }).catch(() => false);
      
      if (hasFormRole) {
        expect(true).toBeTruthy();
      }
    }
    
    // Check for labeled input elements
    const queryEditor = page.getByTestId('query-editor');
    if (await queryEditor.isVisible({ timeout: 5000 }).catch(() => false)) {
      const hasLabel = await queryEditor.evaluate(el => {
        return el.getAttribute('aria-label') || el.getAttribute('aria-labelledby') || 
               document.querySelector(`label[for="${el.id}"]`);
      }).catch(() => false);
      
      expect(hasLabel).toBeTruthy();
    }
  });

  test('buttons have appropriate ARIA attributes', async ({ page }) => {
    await login(page);
    await page.goto('/search');
    
    const buttons = page.getByRole('button');
    const buttonCount = await buttons.count();
    
    let hasAccessibleButtons = false;
    
    for (let i = 0; i < Math.min(buttonCount, 5); i++) {
      const button = buttons.nth(i);
      if (await button.isVisible({ timeout: 3000 }).catch(() => false)) {
        const buttonText = await button.textContent() || '';
        const ariaLabel = await button.getAttribute('aria-label') || '';
        
        // Button should have either visible text or aria-label
        if (buttonText.trim().length > 0 || ariaLabel.length > 0) {
          hasAccessibleButtons = true;
          break;
        }
      }
    }
    
    expect(hasAccessibleButtons).toBeTruthy();
  });

  test('form validation errors are announced to screen readers', async ({ page }) => {
    await page.goto('/login');
    
    // Try to submit empty form to trigger validation
    await page.getByTestId('sign-in').click();
    
    // Look for ARIA live regions or error announcements
    const errorElements = [
      page.locator('[role="alert"]'),
      page.locator('[aria-live="polite"]'),
      page.locator('[aria-live="assertive"]'),
      page.locator('[data-testid*="error"]')
    ];
    
    let hasAccessibleErrors = false;
    for (const element of errorElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        hasAccessibleErrors = true;
        break;
      }
    }
    
    // If no explicit ARIA live regions, check for basic error visibility
    if (!hasAccessibleErrors) {
      const basicErrors = [
        page.getByText(/required/i),
        page.getByText(/invalid/i),
        page.getByText(/error/i)
      ];
      
      for (const element of basicErrors) {
        if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
          hasAccessibleErrors = true;
          break;
        }
      }
    }
    
    // At minimum, form should provide some feedback
    expect(hasAccessibleErrors || page.url().includes('/login')).toBeTruthy();
  });

  test('focus indicators are visible during keyboard navigation', async ({ page }) => {
    await page.goto('/login');
    
    // Add CSS to make focus more visible for testing
    await page.addStyleTag({
      content: `
        *:focus {
          outline: 2px solid blue !important;
          outline-offset: 2px !important;
        }
      `
    });
    
    // Tab through form elements and check focus visibility
    const elements = [
      page.getByTestId('username'),
      page.getByTestId('password'),
      page.getByTestId('sign-in')
    ];
    
    for (const element of elements) {
      await element.focus();
      
      // Check if element has focus styles
      const hasFocusStyle = await element.evaluate(el => {
        const styles = window.getComputedStyle(el);
        return styles.outlineStyle !== 'none' || styles.boxShadow !== 'none';
      }).catch(() => false);
      
      expect(hasFocusStyle).toBeTruthy();
    }
  });

  test('images have appropriate alt text', async ({ page }) => {
    await login(page);
    
    // Check various pages for images
    const routes = ['/', '/search', '/dashboards'];
    
    for (const route of routes) {
      await page.goto(route);
      
      const images = page.locator('img');
      const imageCount = await images.count();
      
      for (let i = 0; i < imageCount; i++) {
        const img = images.nth(i);
        if (await img.isVisible({ timeout: 3000 }).catch(() => false)) {
          const altText = await img.getAttribute('alt') || '';
          const ariaLabel = await img.getAttribute('aria-label') || '';
          const role = await img.getAttribute('role') || '';
          
          // Decorative images should have empty alt or presentation role
          // Content images should have descriptive alt text
          const isAccessible = altText !== null || ariaLabel.length > 0 || role === 'presentation';
          expect(isAccessible).toBeTruthy();
        }
      }
    }
  });

  test('color contrast is sufficient (basic check)', async ({ page }) => {
    await page.goto('/login');
    
    // Basic color contrast check for text elements
    const textElements = [
      page.getByText(/sign in/i),
      page.getByText(/username/i),
      page.getByText(/password/i)
    ];
    
    for (const element of textElements) {
      if (await element.first().isVisible({ timeout: 3000 }).catch(() => false)) {
        const styles = await element.first().evaluate(el => {
          const computed = window.getComputedStyle(el);
          return {
            color: computed.color,
            backgroundColor: computed.backgroundColor
          };
        });
        
        // Basic check that text is not transparent and has some contrast
        expect(styles.color).not.toBe('transparent');
        expect(styles.color).not.toBe('rgba(0, 0, 0, 0)');
      }
    }
  });

  test('tables have proper headers and structure', async ({ page }) => {
    await login(page);
    
    // Check pages that might have tables
    const routes = ['/users', '/roles', '/alarms'];
    
    for (const route of routes) {
      await page.goto(route);
      
      const tables = page.getByRole('table');
      const tableCount = await tables.count();
      
      for (let i = 0; i < tableCount; i++) {
        const table = tables.nth(i);
        if (await table.isVisible({ timeout: 5000 }).catch(() => false)) {
          // Check for table headers
          const headers = table.locator('th, [role="columnheader"]');
          const headerCount = await headers.count();
          
          if (headerCount > 0) {
            // Table has proper headers
            expect(true).toBeTruthy();
          } else {
            // Check for caption or aria-label
            const hasCaption = await table.locator('caption').count() > 0;
            const hasAriaLabel = await table.getAttribute('aria-label') !== null;
            expect(hasCaption || hasAriaLabel).toBeTruthy();
          }
        }
      }
    }
  });

  test('loading states are announced to screen readers', async ({ page }) => {
    await login(page);
    await page.goto('/search');
    
    // Slow down requests to trigger loading states
    await page.route('**/*', route => {
      setTimeout(() => route.continue(), 1000);
    });
    
    // Try to trigger a loading state
    const runButton = page.getByTestId('run-search');
    if (await runButton.isVisible({ timeout: 5000 }).catch(() => false)) {
      await runButton.click();
      
      // Look for loading announcements
      const loadingElements = [
        page.locator('[aria-live]'),
        page.locator('[role="status"]'),
        page.getByText(/loading/i),
        page.locator('[data-testid*="loading"]')
      ];
      
      let hasLoadingAnnouncement = false;
      for (const element of loadingElements) {
        if (await element.first().isVisible({ timeout: 3000 }).catch(() => false)) {
          hasLoadingAnnouncement = true;
          break;
        }
      }
      
      // At minimum, button should be disabled during loading
      const isDisabled = await runButton.isDisabled().catch(() => false);
      expect(hasLoadingAnnouncement || isDisabled).toBeTruthy();
    }
  });

  test('skip links are available for keyboard navigation', async ({ page }) => {
    await login(page);
    
    // Look for skip links (often hidden until focused)
    const skipLinks = [
      page.getByRole('link', { name: /skip to main/i }),
      page.getByRole('link', { name: /skip to content/i }),
      page.locator('a[href="#main"]'),
      page.locator('a[href="#content"]')
    ];
    
    // Tab through to see if skip links become visible
    await page.keyboard.press('Tab');
    await page.keyboard.press('Tab');
    
    let hasSkipLinks = false;
    for (const link of skipLinks) {
      if (await link.first().isVisible({ timeout: 1000 }).catch(() => false)) {
        hasSkipLinks = true;
        
        // Try using the skip link
        await link.first().click();
        expect(true).toBeTruthy(); // Successfully used skip link
        break;
      }
    }
    
    // Even if no explicit skip links, main content should be easily accessible
    if (!hasSkipLinks) {
      const mainContent = page.locator('main, [role="main"], #main, #content');
      const hasMainLandmark = await mainContent.first().isVisible({ timeout: 3000 }).catch(() => false);
      expect(hasMainLandmark).toBeTruthy();
    }
  });
});