import { expect, test } from '@playwright/test';
import { E2E_SKIP_REASON, USERS, e2eDisabled, monthFor, signInAs } from '../fixtures/auth';
import { MyAbsencesPage } from '../pages/my-absences.page';
import { ValidationPage } from '../pages/validation.page';

const date = (day: number, month: number) => `${day}/${String(month).padStart(2, '0')}/2026`;

/**
 * T141c — Flux US1 → US2 : un salarié dépose une demande, son manager la valide, et le statut
 * « Validée » apparaît dans les deux vues.
 */
test.describe('Flux dépôt puis validation', () => {
  test.skip(e2eDisabled, E2E_SKIP_REASON);

  test('le salarié dépose une demande et le manager la valide', async ({ browser }, testInfo) => {
    const start = date(15, monthFor(testInfo));
    const end = date(16, monthFor(testInfo));
    const employeeContext = await browser.newContext();
    const employeePage = await employeeContext.newPage();
    await signInAs(employeePage, USERS['salarie']);

    const myAbsences = new MyAbsencesPage(employeePage);
    await myAbsences.goto();
    await myAbsences.createRequest('Congés payés', start, end);

    await expect(myAbsences.rowFor(start)).toContainText('En attente');

    const managerContext = await browser.newContext();
    const managerPage = await managerContext.newPage();
    await signInAs(managerPage, USERS['manager']);

    const validation = new ValidationPage(managerPage);
    await validation.goto();
    await expect(validation.rowFor(start)).toBeVisible();
    await validation.validate(start);
    await expect(validation.rowFor(start)).toHaveCount(0);

    await myAbsences.goto();
    await expect(myAbsences.rowFor(start)).toContainText('Validée');

    await employeeContext.close();
    await managerContext.close();
  });

  test('un refus exige un motif (EF-011)', async ({ browser }, testInfo) => {
    const start = date(22, monthFor(testInfo));
    const end = date(23, monthFor(testInfo));
    const employeeContext = await browser.newContext();
    const employeePage = await employeeContext.newPage();
    await signInAs(employeePage, USERS['salarie']);

    const myAbsences = new MyAbsencesPage(employeePage);
    await myAbsences.goto();
    await myAbsences.createRequest('Congés payés', start, end);

    const managerContext = await browser.newContext();
    const managerPage = await managerContext.newPage();
    await signInAs(managerPage, USERS['manager']);

    const validation = new ValidationPage(managerPage);
    await validation.goto();
    await validation.rowFor(start).getByRole('button', { name: 'Refuser' }).click();

    // Sans motif saisi, la confirmation reste inaccessible.
    await expect(managerPage.getByRole('button', { name: 'Confirmer le refus' })).toBeDisabled();

    await employeeContext.close();
    await managerContext.close();
  });
});
