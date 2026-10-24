import { Page, TestInfo, expect } from '@playwright/test';

export interface E2EUser {
  username: string;
  password: string;
  name: string;
}

/**
 * Comptes de démonstration du Keycloak de test (`keycloak/timeplanning-realm.json`). Leurs rôles
 * applicatifs sont en base (`fixtures/seed.sql`), comme pour un compte Google en production.
 * Ces mots de passe n'existent que dans ce Keycloak jetable : ce ne sont pas des secrets.
 */
export const USERS: Record<string, E2EUser> = {
  salarie: { username: 'lea.martin', password: 'demo-salarie', name: 'Léa Martin' },
  manager: { username: 'manon.dubois', password: 'demo-manager', name: 'Manon Dubois' },
  rh: { username: 'rachel.hubert', password: 'demo-rh', name: 'Rachel Hubert' },
};

const KEYCLOAK_URL = process.env['E2E_KEYCLOAK_URL'] ?? '';

export const E2E_SKIP_REASON =
  'Keycloak de test non configuré — définir E2E_KEYCLOAK_URL (voir quickstart.md)';
export const e2eDisabled = !KEYCLOAK_URL;

/**
 * Connexion réelle : l'application redirige vers Keycloak, l'utilisateur saisit ses identifiants,
 * puis revient par `/callback` (code d'autorisation + PKCE), comme avec Google en production.
 */
export async function signInAs(page: Page, user: E2EUser): Promise<void> {
  await page.goto('/');
  await page.waitForURL(url => url.href.startsWith(KEYCLOAK_URL));
  await page.locator('#username').fill(user.username);
  await page.locator('#password').fill(user.password);
  await page.locator('#kc-login').click();

  await page.waitForURL('**/my-absences');
  await expect(page.getByText(user.name)).toBeVisible();
}

/**
 * Les projets (bureau, mobile) s'exécutent l'un après l'autre sur la même base : chacun travaille
 * sur son propre mois pour ne pas heurter les absences ou clôtures créées par l'autre.
 */
export function monthFor(testInfo: TestInfo): number {
  return 6 + testInfo.config.projects.findIndex(p => p.name === testInfo.project.name);
}
