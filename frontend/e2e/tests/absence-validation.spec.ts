import { expect, test } from '@playwright/test';
import { USERS, signInAs } from '../fixtures/auth';
import { MyAbsencesPage } from '../pages/my-absences.page';
import { ValidationPage } from '../pages/validation.page';

const EMPLOYEE_TOKEN = process.env['E2E_TOKEN_SALARIE'] ?? '';
const MANAGER_TOKEN = process.env['E2E_TOKEN_MANAGER'] ?? '';

const START = '15/06/2026';
const END = '16/06/2026';

/**
 * T141c — Flux US1 → US2 : un salarié dépose une demande, son manager la valide, et le statut
 * « Validée » apparaît dans les deux vues.
 */
test.describe('Flux dépôt puis validation', () => {
  test.skip(
    !EMPLOYEE_TOKEN || !MANAGER_TOKEN,
    'Jetons E2E absents — définir E2E_TOKEN_SALARIE et E2E_TOKEN_MANAGER'
  );

  test('le salarié dépose une demande et le manager la valide', async ({ browser }) => {
    const employeeContext = await browser.newContext();
    const employeePage = await employeeContext.newPage();
    await signInAs(employeePage, USERS['salarie'], EMPLOYEE_TOKEN);

    const myAbsences = new MyAbsencesPage(employeePage);
    await myAbsences.goto();
    await myAbsences.createRequest('Congés payés', START, END);

    await expect(myAbsences.rowFor(START)).toContainText('En attente');

    const managerContext = await browser.newContext();
    const managerPage = await managerContext.newPage();
    await signInAs(managerPage, USERS['manager'], MANAGER_TOKEN);

    const validation = new ValidationPage(managerPage);
    await validation.goto();
    await expect(validation.rowFor(START)).toBeVisible();
    await validation.validate(START);
    await expect(validation.rowFor(START)).toHaveCount(0);

    await myAbsences.goto();
    await expect(myAbsences.rowFor(START)).toContainText('Validée');

    await employeeContext.close();
    await managerContext.close();
  });

  test('un refus exige un motif (EF-011)', async ({ browser }) => {
    const employeeContext = await browser.newContext();
    const employeePage = await employeeContext.newPage();
    await signInAs(employeePage, USERS['salarie'], EMPLOYEE_TOKEN);

    const myAbsences = new MyAbsencesPage(employeePage);
    await myAbsences.goto();
    await myAbsences.createRequest('Congés payés', '22/06/2026', '23/06/2026');

    const managerContext = await browser.newContext();
    const managerPage = await managerContext.newPage();
    await signInAs(managerPage, USERS['manager'], MANAGER_TOKEN);

    const validation = new ValidationPage(managerPage);
    await validation.goto();
    await validation.rowFor('22/06/2026').getByRole('button', { name: 'Refuser' }).click();

    // Sans motif saisi, la confirmation reste inaccessible.
    await expect(managerPage.getByRole('button', { name: 'Confirmer le refus' })).toBeDisabled();

    await employeeContext.close();
    await managerContext.close();
  });
});
