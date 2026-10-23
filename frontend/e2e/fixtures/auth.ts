import { Page } from '@playwright/test';

export type E2ERole = 'SALARIE' | 'MANAGER' | 'RH' | 'ADMIN';

export interface E2EUser {
  sub: string;
  name: string;
  email: string;
  roles: E2ERole[];
}

/** Comptes de démonstration attendus dans le jeu de données E2E. */
export const USERS: Record<string, E2EUser> = {
  salarie: {
    sub: 'e2e-salarie',
    name: 'Léa Martin',
    email: 'lea.martin@timemanager.fr',
    roles: ['SALARIE'],
  },
  manager: {
    sub: 'e2e-manager',
    name: 'Manon Dubois',
    email: 'manon.dubois@timemanager.fr',
    roles: ['SALARIE', 'MANAGER'],
  },
  rh: {
    sub: 'e2e-rh',
    name: 'Rachel Hubert',
    email: 'rachel.hubert@timemanager.fr',
    roles: ['SALARIE', 'RH'],
  },
};

/**
 * Installe une session OIDC simulée dans le navigateur.
 *
 * <p>La connexion Google réelle n'est pas rejouée en E2E : elle dépend d'un fournisseur externe et
 * d'un second facteur. On dépose directement le jeton attendu par `oidc-client-ts` dans le stockage
 * de session, ce qui reproduit fidèlement l'état de l'application après une connexion réussie.
 */
export async function signInAs(page: Page, user: E2EUser, accessToken: string): Promise<void> {
  const storageKey = `oidc.user:https://accounts.google.com:${process.env['E2E_CLIENT_ID'] ?? 'e2e-client'}`;
  const expiresAt = Math.floor(Date.now() / 1000) + 3600;

  await page.addInitScript(
    ([key, value]) => {
      window.sessionStorage.setItem(key as string, value as string);
    },
    [
      storageKey,
      JSON.stringify({
        access_token: accessToken,
        token_type: 'Bearer',
        expires_at: expiresAt,
        profile: {
          sub: user.sub,
          name: user.name,
          email: user.email,
          roles: user.roles,
        },
      }),
    ]
  );
}
