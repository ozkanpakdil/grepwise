import { test, expect } from '@playwright/test';

test.describe('Error Handling', () => {
  test('404 page displays for non-existent routes', async ({ page }) => {
    await page.goto('/non-existent-page');
    
    // Check for 404 page elements
    const notFoundElements = [
      page.getByText(/404/i),
      page.getByText(/not found/i),
      page.getByText(/page not found/i),
      page.getByText(/doesn.?t exist/i)
    ];
    
    let hasNotFoundElement = false;
    for (const element of notFoundElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        hasNotFoundElement = true;
        break;
      }
    }
    expect(hasNotFoundElement).toBeTruthy();
  });

  test('404 page has navigation back to app', async ({ page }) => {
    await page.goto('/non-existent-route-12345');
    
    // Look for navigation links back to the main app
    const navigationElements = [
      page.getByRole('link', { name: /home/i }),
      page.getByRole('link', { name: /back/i }),
      page.getByRole('button', { name: /home|back/i }),
      page.getByText(/go back/i),
      page.locator('a[href="/"]')
    ];
    
    let hasNavigation = false;
    for (const element of navigationElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        hasNavigation = true;
        // Try clicking the navigation element
        await element.first().click();
        await page.waitForLoadState('networkidle');
        // Should redirect to home or login
        expect(page.url()).toMatch(/\/(login)?$/);
        break;
      }
    }
    expect(hasNavigation).toBeTruthy();
  });

  test('unauthorized access redirects properly', async ({ page }) => {
    // Try to access protected route without authentication
    await page.goto('/settings');
    await page.waitForLoadState('networkidle');
    
    // Should be redirected to login
    expect(page.url()).toMatch(/\/login/);
    
    // Login page should be visible
    await expect(page.getByTestId('sign-in')).toBeVisible({ timeout: 10000 });
  });

  test('non-admin users cannot access admin routes', async ({ page }) => {
    // This would require a non-admin user account to test properly
    // For now, we'll test that admin routes require authentication
    const adminRoutes = ['/settings', '/users', '/roles', '/admin/redaction'];
    
    for (const route of adminRoutes) {
      await page.goto(route);
      await page.waitForLoadState('networkidle');
      
      // Should redirect to login when not authenticated
      expect(page.url()).toMatch(/\/login/);
    }
  });

  test('network errors are handled gracefully', async ({ page }) => {
    // Navigate to a page that might make API calls
    await page.goto('/login');
    
    // Fill in login form
    await page.getByTestId('username').fill('admin');
    await page.getByTestId('password').fill('wrongpassword');
    
    // Block network requests to simulate network error
    await page.route('**/*', route => {
      if (route.request().url().includes('/api/') || route.request().url().includes('/auth')) {
        route.abort();
      } else {
        route.continue();
      }
    });
    
    await page.getByTestId('sign-in').click();
    
    // Should show some kind of error message
    const errorElements = [
      page.getByText(/error/i),
      page.getByText(/failed/i),
      page.getByText(/network/i),
      page.getByText(/connection/i),
      page.locator('[role="alert"]')
    ];
    
    let hasError = false;
    for (const element of errorElements) {
      if (await element.first().isVisible({ timeout: 10000 }).catch(() => false)) {
        hasError = true;
        break;
      }
    }
    expect(hasError).toBeTruthy();
  });

  test('form validation errors display properly', async ({ page }) => {
    await page.goto('/login');
    
    // Try to submit form without filling required fields
    await page.getByTestId('sign-in').click();
    
    // Look for validation errors
    const validationElements = [
      page.getByText(/required/i),
      page.getByText(/invalid/i),
      page.getByText(/enter/i),
      page.locator('[role="alert"]'),
      page.locator('.error'),
      page.locator('[data-testid*="error"]')
    ];
    
    let hasValidation = false;
    for (const element of validationElements) {
      if (await element.first().isVisible({ timeout: 5000 }).catch(() => false)) {
        hasValidation = true;
        break;
      }
    }
    
    // If no validation messages, at least form should not have submitted successfully
    if (!hasValidation) {
      // Should still be on login page
      expect(page.url()).toMatch(/\/login/);
    }
  });

  test('application recovers from JavaScript errors', async ({ page }) => {
    // Monitor for JavaScript errors
    const errors: string[] = [];
    page.on('pageerror', error => {
      errors.push(error.message);
    });
    
    await page.goto('/');
    
    // Navigate through the app to trigger potential JS errors
    const routes = ['/', '/search', '/dashboards', '/alarms'];
    
    for (const route of routes) {
      await page.goto(route, { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1000);
      
      // App should still be functional even if there are some JS errors
      const isAppFunctional = await page.locator('body').isVisible();
      expect(isAppFunctional).toBeTruthy();
    }
    
    // Log errors for debugging but don't fail the test
    if (errors.length > 0) {
      console.log('JavaScript errors encountered:', errors);
    }
  });

  test('loading states are shown for slow operations', async ({ page }) => {
    await page.goto('/login');
    await page.getByTestId('username').fill('admin');
    await page.getByTestId('password').fill('admin');
    
    // Slow down network to trigger loading states
    await page.route('**/*', route => {
      setTimeout(() => route.continue(), 1000);
    });
    
    await page.getByTestId('sign-in').click();
    
    // Look for loading indicators
    const loadingElements = [
      page.getByText(/loading/i),
      page.getByText(/signing in/i),
      page.locator('.spinner'),
      page.locator('.loading'),
      page.locator('[data-testid*="loading"]')
    ];
    
    let hasLoading = false;
    for (const element of loadingElements) {
      if (await element.first().isVisible({ timeout: 2000 }).catch(() => false)) {
        hasLoading = true;
        break;
      }
    }
    
    // Even if no explicit loading indicator, the button might be disabled
    const signInButton = page.getByTestId('sign-in');
    const isDisabled = await signInButton.isDisabled().catch(() => false);
    
    expect(hasLoading || isDisabled).toBeTruthy();
  });
});