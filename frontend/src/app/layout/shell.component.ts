import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../core/auth/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <a class="skip-link" href="#contenu">Aller au contenu principal</a>
    <header class="app-header">
      <div class="header-brand">
        <span class="brand-name">TimePlanning</span>
      </div>
      <nav class="header-nav" aria-label="Navigation principale">
        <a routerLink="/my-absences" routerLinkActive="active">Mes absences</a>
        <a routerLink="/counters" routerLinkActive="active">Mes compteurs</a>
        @if (authService.getUserRoles().includes('MANAGER')) {
          <a routerLink="/validation" routerLinkActive="active">Validation</a>
          <a routerLink="/planning" routerLinkActive="active">Planning</a>
        }
        @if (authService.getUserRoles().includes('RH')) {
          <a routerLink="/payroll" routerLinkActive="active">RH / Paie</a>
        }
        <a routerLink="/recurrences" routerLinkActive="active">Récurrences</a>
        @if (authService.getUserRoles().includes('ADMIN')) {
          <a routerLink="/admin" routerLinkActive="active">Administration</a>
        }
      </nav>
      <div class="header-user">
        <span>{{ authService.currentUser()?.profile?.name }}</span>
        <button type="button" (click)="logout()">Déconnexion</button>
      </div>
    </header>
    <main class="app-content" id="contenu" tabindex="-1">
      <router-outlet />
    </main>
  `,
  styles: [`
    .app-header {
      display: flex;
      align-items: center;
      padding: 0 1.5rem;
      height: 60px;
      background: #0d47a1;
      color: white;
      gap: 2rem;
    }
    .brand-name { font-size: 1.2rem; font-weight: bold; }
    .header-nav { display: flex; gap: 1rem; flex: 1; }
    .header-nav a { color: white; text-decoration: none; padding: 0.5rem; border-radius: 4px; }
    .header-nav a.active {
      background: rgba(255,255,255,0.2);
      text-decoration: underline;
      text-underline-offset: 4px;
    }
    .header-nav a:focus-visible,
    .header-user button:focus-visible,
    .skip-link:focus {
      outline: 3px solid #ffffff;
      outline-offset: 2px;
    }
    .skip-link {
      position: absolute;
      left: -9999px;
      background: #ffffff;
      color: #0d47a1;
      padding: 0.5rem 1rem;
      z-index: 10;
    }
    .skip-link:focus {
      left: 0.5rem;
      top: 0.5rem;
    }
    .app-content { padding: 1.5rem; }
    button { background: transparent; border: 1px solid white; color: white;
             padding: 0.25rem 0.75rem; border-radius: 4px; cursor: pointer; }
  `],
})
export class ShellComponent {
  authService = inject(AuthService);

  logout(): void {
    this.authService.logout();
  }
}
