import { expect, test } from '@playwright/test';
import { E2E_SKIP_REASON, USERS, e2eDisabled, signInAs } from '../fixtures/auth';

/**
 * EF-017 — Planning mensuel filtrable par équipe et sous-équipe, selon le périmètre : le manager
 * voit ses équipes et leurs sous-équipes, la RH toutes les équipes.
 */
test.describe('Planning mensuel — filtre par équipe', () => {
  test.skip(e2eDisabled, E2E_SKIP_REASON);

  test('le manager filtre sur une sous-équipe de son périmètre', async ({ page }) => {
    await signInAs(page, USERS['manager']);
    await page.goto('/planning');
    await expect(page.getByRole('heading', { name: 'Planning mensuel' })).toBeVisible();

    await page.locator('#teamFilter').click();
    await expect(page.getByRole('option', { name: 'Équipe E2E', exact: true })).toBeVisible();
    const filteredRequest = page.waitForRequest(
      request => request.url().includes('/api/v1/planning?') && request.url().includes('teamId=')
    );
    await page.getByRole('option', { name: '— Support E2E' }).click();

    expect((await filteredRequest).url()).toContain('teamId=e2e00000-0000-0000-0000-0000000000a2');
  });

  test('la RH accède au planning de toutes les équipes depuis le menu', async ({ page }) => {
    await signInAs(page, USERS['rh']);
    await page.getByRole('link', { name: 'Planning' }).click();
    await expect(page.getByRole('heading', { name: 'Planning mensuel' })).toBeVisible();

    await page.locator('#teamFilter').click();
    await expect(page.getByRole('option', { name: 'Équipe E2E', exact: true })).toBeVisible();
    await expect(page.getByRole('option', { name: '— Support E2E' })).toBeVisible();
  });
});
