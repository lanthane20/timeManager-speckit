import { expect, test } from '@playwright/test';
import { USERS, signInAs } from '../fixtures/auth';
import { PayrollPage } from '../pages/payroll.page';

const RH_TOKEN = process.env['E2E_TOKEN_RH'] ?? '';

/**
 * T141d — Flux US5 : la RH clôture un mois puis déclenche l'export annuel ; le téléchargement
 * s'amorce (EF-024, EF-027).
 */
test.describe('Clôture mensuelle et export paie', () => {
  test.skip(!RH_TOKEN, 'Jeton E2E absent — définir E2E_TOKEN_RH');

  test.beforeEach(async ({ page }) => {
    await signInAs(page, USERS['rh'], RH_TOKEN);
  });

  test('la RH clôture une période en une seule action', async ({ page }) => {
    const payroll = new PayrollPage(page);
    await payroll.goto();

    await payroll.closureYear.fill('2026');
    await payroll.closeButton.click();

    await expect(page.getByText(/Période .* clôturée/)).toBeVisible();
  });

  test("l'export annuel déclenche un téléchargement", async ({ page }) => {
    const payroll = new PayrollPage(page);
    await payroll.goto();

    const downloadPromise = page.waitForEvent('download');
    await payroll.exportButton.click();
    const download = await downloadPromise;

    expect(download.suggestedFilename()).toMatch(/absences-\d{4}\.xlsx/);
  });
});
