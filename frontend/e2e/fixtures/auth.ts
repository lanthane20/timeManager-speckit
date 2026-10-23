import { Page, TestInfo } from '@playwright/test';
import { createSign } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { environment } from '../../src/environments/environment';

/**
 * Clé privée RSA (PEM) signant les jetons de test. Le backend E2E valide ces jetons avec la clé
 * publique associée (`SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_PUBLIC_KEY_LOCATION`) : aucun
 * jeton Google ni secret n'est nécessaire. La CI génère une paire de clés jetable à chaque run.
 */
const PRIVATE_KEY_PATH = process.env['E2E_JWT_PRIVATE_KEY'] ?? '';

export const E2E_SKIP_REASON =
  'Backend E2E non configuré — définir E2E_JWT_PRIVATE_KEY (voir quickstart.md)';
export const e2eDisabled = !PRIVATE_KEY_PATH;

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
 * Installe une session OIDC simulée dans le navigateur, avec un jeton signé pour l'utilisateur.
 *
 * <p>La connexion Google réelle n'est pas rejouée en E2E : elle dépend d'un fournisseur externe et
 * d'un second facteur. On dépose directement le jeton attendu par `oidc-client-ts` dans le stockage
 * de session, ce qui reproduit fidèlement l'état de l'application après une connexion réussie.
 */
export async function signInAs(page: Page, user: E2EUser): Promise<void> {
  const storageKey = `oidc.user:${environment.oidc.authority}:${environment.oidc.client_id}`;
  const expiresAt = Math.floor(Date.now() / 1000) + 3600;

  await page.addInitScript(
    ([key, value]) => {
      window.sessionStorage.setItem(key as string, value as string);
    },
    [
      storageKey,
      JSON.stringify({
        access_token: signAccessToken(user.sub, expiresAt),
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

/** Signe un JWT RS256 minimal : le backend n'utilise que le `sub` pour retrouver l'utilisateur. */
function signAccessToken(sub: string, expiresAt: number): string {
  const encode = (part: object) => Buffer.from(JSON.stringify(part)).toString('base64url');
  const payload = { sub, iat: Math.floor(Date.now() / 1000), exp: expiresAt };
  const unsigned = `${encode({ alg: 'RS256', typ: 'JWT' })}.${encode(payload)}`;
  const signature = createSign('RSA-SHA256')
    .update(unsigned)
    .sign(readFileSync(PRIVATE_KEY_PATH), 'base64url');
  return `${unsigned}.${signature}`;
}

/**
 * Les projets (bureau, mobile) s'exécutent l'un après l'autre sur la même base : chacun travaille
 * sur son propre mois pour ne pas heurter les absences ou clôtures créées par l'autre.
 */
export function monthFor(testInfo: TestInfo): number {
  return 6 + testInfo.config.projects.findIndex(p => p.name === testInfo.project.name);
}
