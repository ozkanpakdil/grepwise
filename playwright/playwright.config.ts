import {defineConfig, devices} from '@playwright/test';
import path from 'path';

const baseURL = process.env.BASE_URL || 'http://localhost:3000';

export default defineConfig({
    testDir: './tests',
    timeout: 30_000,
    expect: {timeout: 5_000},
    retries: process.env.CI ? 2 : 0,
    reporter: process.env.CI ? [['github']] : [['html', {open: 'never'}], ['list']],
    use: {
        baseURL,
        trace: 'retain-on-failure',
        screenshot: 'only-on-failure',
        video: 'retain-on-failure',
    },
    projects: [
        {name: 'chromium', use: {...devices['Desktop Chrome']}},
        {name: 'firefox', use: {...devices['Desktop Firefox']}},
        {name: 'webkit', use: {...devices['Desktop Safari']}},
    ],
    // Start frontend dev server only when not running with PW_NO_SERVER
    // TODO there should be some control for webserver but it is not decided yet.
    /*webServer: process.env.PW_NO_SERVER ? undefined : {
        command: 'npm run dev',
        cwd: path.resolve(__dirname, '../frontend'),
        url: baseURL,
        reuseExistingServer: true,
        stdout: 'pipe', // Changed from 'ignore' to see errors
        stderr: 'pipe', // Changed from 'ignore' to see errors
        timeout: 120_000,
    },*/
});
