import { defineConfig, devices } from '@playwright/test';

const BASE_URL = process.env['E2E_BASE_URL'] ?? 'http://localhost:4200';

/**
 * T141b — Configuration Playwright des tests end-to-end.
 *
 * Les scénarios se connectent réellement avec les comptes de démonstration d'un Keycloak de test,
 * qui remplace Google (voir `fixtures/auth.ts` et `keycloak/timeplanning-realm.json`).
 */
export default defineConfig({
  testDir: './tests',
  outputDir: './.results',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 2 : 0,
  workers: process.env['CI'] ? 1 : undefined,
  reporter: process.env['CI'] ? [['github'], ['html', { open: 'never' }]] : [['list']],
  timeout: 30_000,
  expect: { timeout: 10_000 },

  use: {
    baseURL: BASE_URL,
    locale: 'fr-FR',
    timezoneId: 'Europe/Paris',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  projects: [
    { name: 'chromium-desktop', use: { ...devices['Desktop Chrome'] } },
    { name: 'chromium-mobile', use: { ...devices['Pixel 7'] } },
  ],

  // En local, Playwright démarre le serveur de développement ; en CI il est lancé par le workflow.
  webServer: process.env['CI']
    ? undefined
    : {
        command: 'npm run start:e2e',
        url: BASE_URL,
        cwd: '..',
        reuseExistingServer: true,
        timeout: 120_000,
      },
});
