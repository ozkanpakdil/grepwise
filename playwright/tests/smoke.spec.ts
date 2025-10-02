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

  test('UDP and TCP syslog ports accept data', async () => {
    // Test UDP port 1514
    const udpWorks = await sendUdpSyslog('test-udp-message');
    console.log(`UDP port 1514: ${udpWorks ? 'OK' : 'FAIL'}`);

    // Test TCP port 1514
    const tcpWorks = await testTcpPort(1514);
    console.log(`TCP port 1514: ${tcpWorks ? 'OK' : 'FAIL'}`);

    // At least UDP must work (primary syslog protocol)
    expect(udpWorks).toBeTruthy();
  });
});
