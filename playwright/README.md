# Grepwise E2E Tests (Playwright)

This folder contains end-to-end UI tests for Grepwise using Playwright.

## Why a top-level `playwright/` folder?
- Keeps E2E independent from the frontend dependencies to avoid bundling browsers into the frontend build.
- Represents cross-service flows (frontend + backend) better than locating under `frontend/`.
- Works well with monorepo/CI setups.

If you prefer `e2e/` or placing under `frontend/`, we can move it easily.

## Prerequisites
- Node 18+
- Install deps and browsers:

```bash
cd playwright
npm install
npm run install-browsers  # installs browsers locally without sudo
```

## Running tests
- Start automatically with Vite dev server at http://localhost:3000 (default):

```bash
cd playwright
npm test
```

- Use an existing running app (e.g., docker-compose) and custom base URL:

```bash
cd playwright
PW_NO_SERVER=1 BASE_URL=http://localhost:3000 npm test
```

- Headed and UI mode:

```bash
npm run test:headed
npm run test:ui
```

## Notes
- Traces, screenshots, and videos are retained on failures to aid debugging.
- Consider adding data-testid attributes to stabilize selectors.
- Next candidates to test: entering a search query, executing it, validating results and histogram, pagination, filters, streaming, sorting.
