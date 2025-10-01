# Grepwise Playwright Test Suite

Comprehensive end-to-end testing for the Grepwise log analysis platform using Playwright.

## Test Coverage

This test suite provides comprehensive coverage across:

### Core Functionality
- **Authentication** (`login.spec.ts`) - Login, logout, session management
- **Search Engine** (`login-and-search.spec.ts`) - Query execution, filtering, results
- **Navigation** (`navigation.spec.ts`) - Page routing, menu functionality
- **Pagination** (`pagination-and-page-size.spec.ts`) - Results pagination controls

### Application Pages
- **Dashboards** (`dashboards.spec.ts`) - Dashboard creation, editing, viewing
- **Alarms** (`alarms.spec.ts`) - Alarm configuration, monitoring setup
- **Admin Features** (`admin-pages.spec.ts`) - Settings, users, roles, redaction

### Quality Assurance
- **Responsive Design** (`responsive-design.spec.ts`) - Mobile, tablet, desktop layouts
- **Accessibility** (`accessibility.spec.ts`) - WCAG compliance, keyboard navigation
- **Error Handling** (`error-handling.spec.ts`) - Network failures, validation errors
- **Theme Switching** (`theme-switching.spec.ts`) - Dark/light mode functionality

### System Tests
- **Smoke Tests** (`smoke.spec.ts`) - Basic application functionality

## Running Tests

### Prerequisites
- Node.js 18+ installed
- Backend server running on `localhost:8080`
- Frontend server running on `localhost:3000`

### Local Development
```bash
# Install dependencies
npm ci

# Install Playwright browsers
npm run install-browsers

# Run all tests
npm test

# Run tests in headed mode (visible browser)
npm run test:headed

# Run tests with UI mode
npm run test:ui

# Run specific test file
npx playwright test dashboards.spec.ts

# Run tests on specific browser
npx playwright test --project=chromium
```

### CI/CD Integration
Tests run automatically on:
- Push to main branch
- Pull requests
- Manual workflow dispatch

## Test Reports

### HTML Reports
- Generated automatically after test runs
- Available at `playwright-report/index.html`
- Published to GitHub Pages at: `https://ozkanpakdil.github.io/grepwise/playwright/`

### Report Features
- Test execution timeline
- Screenshots on failure
- Video recordings for failed tests
- Network activity traces
- Browser console logs

## Configuration

Tests are configured via `playwright.config.ts`:

- **Browsers**: Chrome, Firefox, WebKit
- **Timeout**: 30 seconds per test
- **Retries**: 2 attempts on CI
- **Parallel**: Yes (by default)
- **Base URL**: `http://localhost:3000`

### Environment Variables
- `BASE_URL` - Override default base URL
- `PW_NO_SERVER` - Skip starting web server
- `CI` - Enables CI-specific settings

## Writing New Tests

### Test Structure
```typescript
import { test, expect } from '@playwright/test';

async function login(page) {
  await page.goto('/login');
  await page.getByTestId('username').fill('admin');
  await page.getByTestId('password').fill('admin');
  await page.getByTestId('sign-in').click();
  await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });
}

test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test('should perform specific action', async ({ page }) => {
    await page.goto('/feature');
    // Test implementation
  });
});
```

### Best Practices
1. **Use data-testid attributes** for reliable element selection
2. **Include timeouts** for async operations
3. **Handle flaky elements** with retry logic
4. **Test across all browsers** in the configuration
5. **Clean up state** between tests
6. **Use page.waitForLoadState()** after navigation

### Selectors Strategy
1. `page.getByTestId()` - Preferred for app elements
2. `page.getByRole()` - For semantic HTML elements  
3. `page.getByText()` - For content-based selection
4. CSS selectors - As last resort

## Debugging

### Debug Mode
```bash
# Run with debug console
npx playwright test --debug

# Run specific test with trace
npx playwright test --trace on dashboards.spec.ts

# Show test report
npx playwright show-report
```

### Common Issues
- **Timeout errors**: Increase timeout or add explicit waits
- **Flaky tests**: Add better wait conditions
- **Element not found**: Check selectors and timing

## Architecture

### Why a top-level `playwright/` folder?
- Keeps E2E independent from the frontend dependencies to avoid bundling browsers into the frontend build
- Represents cross-service flows (frontend + backend) better than locating under `frontend/`
- Works well with monorepo/CI setups

### Maintenance

#### Regular Tasks
- [ ] Review test execution times
- [ ] Update selectors for UI changes
- [ ] Add tests for new features
- [ ] Monitor flaky test patterns
- [ ] Update browser versions

#### Performance
- Tests run in parallel by default
- Average execution time: ~15-20 minutes
- Consider test sharding for larger suites
