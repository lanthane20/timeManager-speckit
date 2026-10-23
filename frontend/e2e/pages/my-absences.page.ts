import { Locator, Page, expect } from '@playwright/test';

/** Page object — écran « Mes demandes d'absence » (US1). */
export class MyAbsencesPage {
  readonly newRequestButton: Locator;
  readonly absenceTypeSelect: Locator;
  readonly startDate: Locator;
  readonly endDate: Locator;
  readonly submitButton: Locator;
  readonly rows: Locator;

  constructor(private readonly page: Page) {
    this.newRequestButton = page.getByRole('button', { name: 'Nouvelle demande' });
    this.absenceTypeSelect = page.locator('#absenceType');
    this.startDate = page.locator('#startDate');
    this.endDate = page.locator('#endDate');
    this.submitButton = page.getByRole('button', { name: 'Envoyer la demande' });
    this.rows = page.locator('table tbody tr');
  }

  async goto(): Promise<void> {
    await this.page.goto('/my-absences');
    await expect(this.page.getByRole('heading', { name: "Mes demandes d'absence" })).toBeVisible();
  }

  async createRequest(absenceTypeLabel: string, start: string, end: string): Promise<void> {
    await this.newRequestButton.click();
    await this.absenceTypeSelect.click();
    await this.page.getByRole('option', { name: absenceTypeLabel }).click();
    await this.typeDate(this.startDate, start);
    await this.typeDate(this.endDate, end);
    await this.submitButton.click();
  }

  /**
   * Le datepicker PrimeNG n'interprète que la saisie clavier : `fill()` remplit le champ sans mettre
   * à jour le formulaire, qui l'efface à la perte du focus.
   */
  private async typeDate(field: Locator, date: string): Promise<void> {
    await field.pressSequentially(date);
    await field.press('Escape');
  }

  rowFor(start: string): Locator {
    return this.rows.filter({ hasText: start });
  }
}
