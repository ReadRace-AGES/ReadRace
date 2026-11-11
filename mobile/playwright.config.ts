import { defineConfig } from '@playwright/test';

const presentation = process.env.E2E_MODE === 'presentation';
export default defineConfig({
  testDir: './e2e',
  testMatch: presentation
    ? 'presentation/**/*.spec.ts'
    : ['smoke/**/*.spec.ts', 'regression/**/*.spec.ts'],
  fullyParallel: false,
  workers: 1,
  forbidOnly: !!process.env.CI,
  retries: presentation ? 0 : process.env.CI ? 1 : 0,
  timeout: 120_000,
  expect: { timeout: 15_000 },
  outputDir: 'test-results',
  reporter: [
    ['list'],
    ['html', { open: 'never' }],
    ['junit', { outputFile: 'e2e-artifacts/results.xml' }],
  ],
  use: {
    baseURL: `http://127.0.0.1:${process.env.E2E_WEB_PORT ?? '4173'}`,
    viewport: { width: 390, height: 844 },
    locale: 'pt-BR',
    timezoneId: 'America/Sao_Paulo',
    browserName: 'chromium',
    trace: presentation ? 'off' : 'on-first-retry',
    screenshot: 'only-on-failure',
    video: presentation ? 'on' : 'retain-on-failure',
  },
  webServer: {
    command: `node node_modules/serve/build/main.js -s dist -l tcp://127.0.0.1:${process.env.E2E_WEB_PORT ?? '4173'}`,
    url: `http://127.0.0.1:${process.env.E2E_WEB_PORT ?? '4173'}`,
    reuseExistingServer: false,
    timeout: 30_000,
  },
});
