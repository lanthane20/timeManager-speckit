import { Locator, Page, expect } from '@playwright/test';

/** Page object — tableau de bord RH et paie (US5). */
export class PayrollPage {
  readonly closureYear: Locator;
  readonly closeButton: Locator;
  readonly exportButton: Locator;

  constructor(private readonly page: Page) {
    this.closureYear = page.locator('#closureYear input');
    this.closeButton = page.getByRole('button', { name: 'Clôturer la période' });
    this.exportButton = page.getByRole('button', { name: "Télécharger l'export Excel" });
  }

  async goto(): Promise<void> {
    await this.page.goto('/payroll');
    await expect(this.page.getByRole('heading', { name: 'RH et paie' })).toBeVisible();
  }
}
