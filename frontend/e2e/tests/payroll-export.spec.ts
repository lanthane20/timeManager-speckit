import { expect, test } from '@playwright/test';
import { E2E_SKIP_REASON, USERS, e2eDisabled, monthFor, signInAs } from '../fixtures/auth';
import { PayrollPage } from '../pages/payroll.page';

/**
 * T141d — Flux US5 : la RH clôture un mois puis déclenche l'export annuel ; le téléchargement
 * s'amorce (EF-024, EF-027).
 */
test.describe('Clôture mensuelle et export paie', () => {
  test.skip(e2eDisabled, E2E_SKIP_REASON);

  test.beforeEach(async ({ page }) => {
    await signInAs(page, USERS['rh']);
  });

  test('la RH clôture une période en une seule action', async ({ page }, testInfo) => {
    const payroll = new PayrollPage(page);
    await payroll.goto();

    await payroll.closureYear.fill('2026');
    await payroll.selectClosureMonth(['juin', 'juillet'][monthFor(testInfo) - 6]);
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
