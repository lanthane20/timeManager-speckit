import { expect, test } from '@playwright/test';
import { E2E_SKIP_REASON, USERS, e2eDisabled, signInAs } from '../fixtures/auth';

/**
 * T141e — Cloisonnement des rôles : un salarié n'accède ni à l'administration, ni à la file de
 * validation, ni aux écrans RH (Constitution Principe IV, EF-035).
 */
test.describe('Guards d\'accès par rôle', () => {
  test.skip(e2eDisabled, E2E_SKIP_REASON);

  test.beforeEach(async ({ page }) => {
    await signInAs(page, USERS['salarie']);
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
