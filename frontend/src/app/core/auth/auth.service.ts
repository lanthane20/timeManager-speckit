import { Injectable, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { UserManager, UserManagerSettings, User } from 'oidc-client-ts';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly router = inject(Router);
  private readonly userManager = new UserManager(environment.oidc as UserManagerSettings);

  currentUser = signal<User | null>(null);

  constructor() {
    this.userManager.getUser().then(user => this.currentUser.set(user));
  }

  login(): Promise<void> {
    return this.userManager.signinRedirect();
  }

  async handleCallback(): Promise<void> {
    const user = await this.userManager.signinRedirectCallback();
    this.currentUser.set(user);
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
    const profile = this.currentUser()?.profile;
    if (!profile) return [];
    const roles = (profile as Record<string, unknown>)['roles'];
    if (Array.isArray(roles)) return roles as string[];
    return [];
  }
}
