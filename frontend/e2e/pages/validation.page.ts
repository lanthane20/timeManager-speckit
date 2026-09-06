import { Locator, Page, expect } from '@playwright/test';

/** Page object — file de validation du manager (US2, US3). */
export class ValidationPage {
  readonly rows: Locator;

  constructor(private readonly page: Page) {
    this.rows = page.locator('table tbody tr');
  }

  async goto(): Promise<void> {
    await this.page.goto('/validation');
    await expect(this.page.getByRole('heading', { name: 'Demandes à valider' })).toBeVisible();
  }

  rowFor(text: string): Locator {
    return this.rows.filter({ hasText: text });
  }

  async validate(text: string): Promise<void> {
    await this.rowFor(text).getByRole('button', { name: 'Valider' }).click();
  }

  async refuse(text: string, reason: string): Promise<void> {
    await this.rowFor(text).getByRole('button', { name: 'Refuser' }).click();
    await this.page.locator('#refusalReason').fill(reason);
    await this.page.getByRole('button', { name: 'Confirmer le refus' }).click();
  }
}
