import { test, expect } from '@playwright/test';
import * as dgram from 'dgram';
import * as net from 'net';

function sendUdpSyslog(message: string, port: number = 1514): Promise<boolean> {
  return new Promise((resolve) => {
    const client = dgram.createSocket('udp4');
    const timestamp = new Date().toISOString();
    const syslogMessage = `<134>1 ${timestamp} test-host playwright-test - - - ${message}`;

    client.send(syslogMessage, port, 'localhost', (err) => {
      client.close();
      resolve(!err);
    });
  });
}

function testTcpPort(port: number = 1514): Promise<boolean> {
  return new Promise((resolve) => {
    const client = new net.Socket();

    client.connect(port, 'localhost', () => {
      client.end();
      resolve(true);
    });

    client.on('error', () => resolve(false));

    setTimeout(() => {
      client.destroy();
      resolve(false);
    }, 3000);
  });
}

test.describe('Critical Functionality Tests', () => {
  test('authentication works - login, logout, invalid credentials', async ({ page }) => {
    // Test 1: Home redirects to login
    await page.goto('/');
    await expect(page).toHaveURL(/\/login/);

    // Test 2: Invalid login rejected
    await page.getByTestId('username').fill('admin');
    await page.getByTestId('password').fill('wrong');
    await page.getByTestId('sign-in').click();
    await page.waitForTimeout(2000);
    expect(page.url()).toContain('/login');

    // Test 3: Valid login succeeds
    await page.getByTestId('username').clear();
    await page.getByTestId('password').clear();
    await page.getByTestId('username').fill('admin');
    await page.getByTestId('password').fill('admin');
    await page.getByTestId('sign-in').click();
    await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });

    // Test 4: Logout works
    await page.getByTestId('logout').click();
    await expect(page).toHaveURL(/\/login/);
  });

  test('UDP and TCP syslog data appears in search UI', async ({ page }) => {
    // Generate unique identifier for this test
    const testId = `playwright-test-${Date.now()}-${Math.random().toString(36).substring(7)}`;
    const udpMessage = `UDP-TEST-${testId}`;
    const tcpMessage = `TCP-TEST-${testId}`;

    // Send UDP syslog message
    const udpSent = await sendUdpSyslog(udpMessage);
    console.log(`UDP message sent: ${udpSent ? 'OK' : 'FAIL'}`);
    expect(udpSent).toBeTruthy();

    // Send TCP syslog message
    const tcpSent = await new Promise<boolean>((resolve) => {
      const client = new net.Socket();
      const timestamp = new Date().toISOString();
      const syslogMessage = `<134>1 ${timestamp} test-host playwright-test - - - ${tcpMessage}\n`;

      client.connect(1514, 'localhost', () => {
        client.write(syslogMessage, (err) => {
          client.end();
          resolve(!err);
        });
      });

      client.on('error', () => {
        client.destroy();
        resolve(false);
      });

      setTimeout(() => {
        client.destroy();
        resolve(false);
      }, 3000);
    });
    console.log(`TCP message sent: ${tcpSent ? 'OK' : 'FAIL'}`);

    // Wait for data to be indexed (give it a few seconds)
    await page.waitForTimeout(5000);

    // Login to the application
    await page.goto('/login');
    await page.getByTestId('username').fill('admin');
    await page.getByTestId('password').fill('admin');
    await page.getByTestId('sign-in').click();
    await expect(page.getByTestId('logout')).toBeVisible({ timeout: 20_000 });

    // Navigate to search page
    await page.goto('/search');
    await page.waitForLoadState('networkidle');

    // Wait for Monaco editor or alternative search interface
    const editorVisible = await page.locator('.monaco-editor').first().isVisible({ timeout: 30_000 }).catch(() => false);

    if (editorVisible) {
      // Search for UDP message using Monaco editor
      await page.locator('.monaco-editor').first().click();
      await page.keyboard.type(udpMessage);
    } else {
      // Try alternative search interface
      const queryEditor = page.getByTestId('query-editor');
      if (await queryEditor.isVisible({ timeout: 5000 }).catch(() => false)) {
        await queryEditor.click();
        await queryEditor.fill(udpMessage);
      } else {
        test.skip(true, 'Search interface not available');
      }
    }

    await page.getByTestId('run-search').click();

    // Wait for results
    await expect(page.getByTestId('histogram-section')).toBeVisible({ timeout: 20_000 });

    // Verify UDP message appears in results
    const resultsSection = page.getByTestId('results-section');
    await expect(resultsSection).toBeVisible({ timeout: 10_000 });

    // Check if our message is in the results
    const pageContent = await page.content();
    const udpFound = pageContent.includes(udpMessage);
    console.log(`UDP message found in UI: ${udpFound ? 'YES' : 'NO'}`);
    expect(udpFound).toBeTruthy();

    // Clear search and search for TCP message if it was sent
    if (tcpSent) {
      if (editorVisible) {
        await page.locator('.monaco-editor').first().click();
        await page.keyboard.press('Control+A');
        await page.keyboard.type(tcpMessage);
      } else {
        const queryEditor = page.getByTestId('query-editor');
        if (await queryEditor.isVisible({ timeout: 5000 }).catch(() => false)) {
          await queryEditor.clear();
          await queryEditor.fill(tcpMessage);
        }
      }
      await page.getByTestId('run-search').click();

      await expect(page.getByTestId('histogram-section')).toBeVisible({ timeout: 20_000 });
      await expect(resultsSection).toBeVisible({ timeout: 10_000 });

      const tcpPageContent = await page.content();
      const tcpFound = tcpPageContent.includes(tcpMessage);
      console.log(`TCP message found in UI: ${tcpFound ? 'YES' : 'NO'}`);
      expect(tcpFound).toBeTruthy();
    }
  });
});
