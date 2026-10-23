import { Injectable, inject, signal } from '@angular/core';
import { HttpBackend, HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { UserManager, UserManagerSettings, User } from 'oidc-client-ts';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly router = inject(Router);
  // HttpBackend contourne les intercepteurs : l'intercepteur d'authentification dépend de ce service.
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly userManager = new UserManager(environment.oidc as UserManagerSettings);

  currentUser = signal<User | null>(null);
  // Les rôles applicatifs sont en base (le jeton du fournisseur d'identité n'en porte pas).
  private readonly roles = signal<string[]>([]);

  // oidc-client-ts relit la session de façon asynchrone : les guards doivent l'attendre, sinon
  // chaque chargement de page renverrait un utilisateur connecté vers la page de connexion.
  private readonly sessionRestored: Promise<void>;

  constructor() {
    this.sessionRestored = this.userManager.getUser().then(user => this.applySession(user));
  }

  whenSessionRestored(): Promise<void> {
    return this.sessionRestored;
  }

  login(): Promise<void> {
    return this.userManager.signinRedirect();
  }

  async handleCallback(): Promise<void> {
    const user = await this.userManager.signinRedirectCallback();
    await this.applySession(user);
    await this.router.navigate(['/']);
  }

  logout(): Promise<void> {
    return this.userManager.signoutRedirect();
  }

  async getAccessToken(): Promise<string | null> {
    const user = await this.userManager.getUser();
    return user?.access_token ?? null;
  }

  isAuthenticated(): boolean {
    return this.currentUser() !== null && !this.currentUser()?.expired;
  }

  getUserRoles(): string[] {
    return this.roles();
  }

  private async applySession(user: User | null): Promise<void> {
    this.currentUser.set(user);
    this.roles.set(user && !user.expired ? await this.loadRoles(user.access_token) : []);
  }

  private async loadRoles(accessToken: string): Promise<string[]> {
    try {
      const me = await firstValueFrom(
        this.http.get<{ roles: string[] }>(`${environment.apiUrl}/v1/me`, {
          headers: { Authorization: `Bearer ${accessToken}` },
        })
      );
      return me.roles;
    } catch {
      // Profil indisponible : aucun rôle, les écrans réservés restent fermés.
      return [];
    }
  }
}
