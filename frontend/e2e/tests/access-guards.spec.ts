import { expect, test } from '@playwright/test';
import { USERS, signInAs } from '../fixtures/auth';

const EMPLOYEE_TOKEN = process.env['E2E_TOKEN_SALARIE'] ?? '';

/**
 * T141e — Cloisonnement des rôles : un salarié n'accède ni à l'administration, ni à la file de
 * validation, ni aux écrans RH (Constitution Principe IV, EF-035).
 */
test.describe('Guards d\'accès par rôle', () => {
  test.skip(!EMPLOYEE_TOKEN, 'Jeton E2E absent — définir E2E_TOKEN_SALARIE');

  test.beforeEach(async ({ page }) => {
    await signInAs(page, USERS['salarie'], EMPLOYEE_TOKEN);
  });

  for (const path of ['/admin', '/validation', '/payroll', '/planning']) {
    test(`un salarié ne peut pas ouvrir ${path}`, async ({ page }) => {
      await page.goto(path);

      // Le guard de rôle redirige : l'écran demandé ne doit jamais s'afficher.
      await expect(page).not.toHaveURL(new RegExp(`${path}$`));
      await expect(page.getByRole('heading', { name: 'Administration technique' })).toHaveCount(0);
      await expect(page.getByRole('heading', { name: 'Demandes à valider' })).toHaveCount(0);
      await expect(page.getByRole('heading', { name: 'RH et paie' })).toHaveCount(0);
    });
  }

  test('la navigation ne propose pas les entrées réservées aux autres rôles', async ({ page }) => {
    await page.goto('/my-absences');

    await expect(page.getByRole('link', { name: 'Administration' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'Validation' })).toHaveCount(0);
    await expect(page.getByRole('link', { name: 'RH / Paie' })).toHaveCount(0);
  });
});
