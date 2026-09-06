import { Component, OnInit, inject, signal } from '@angular/core';
import { AuthService } from './auth.service';

/** Point de retour OIDC — échange le code d'autorisation puis redirige vers l'accueil. */
@Component({
  selector: 'app-callback',
  standalone: true,
  template: `
    <p role="status">
      {{ error() ?? 'Connexion en cours…' }}
    </p>
  `,
})
export class CallbackComponent implements OnInit {
  private readonly authService = inject(AuthService);

  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.authService
      .handleCallback()
      .catch(() => this.error.set('La connexion a échoué. Merci de réessayer.'));
  }
}
