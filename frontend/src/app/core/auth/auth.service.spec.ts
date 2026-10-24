import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

const STORAGE_KEY = `oidc.user:${environment.oidc.authority}:${environment.oidc.client_id}`;

describe('AuthService', () => {
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    sessionStorage.clear();
    httpMock.verify();
  });

  function storeSession(profileRoles: string[]): void {
    sessionStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        access_token: 'jeton-de-test',
        token_type: 'Bearer',
        expires_at: Math.floor(Date.now() / 1000) + 3600,
        profile: { sub: 'sub-manager', roles: profileRoles },
      }),
    );
  }

  it('lit les rôles applicatifs depuis /v1/me et non dans le profil du jeton', async () => {
    // Un jeton Google ne porte aucun rôle ; même s'il en portait, la base fait foi.
    storeSession(['ADMIN']);
    const service = TestBed.inject(AuthService);

    const request = await vi.waitFor(() => httpMock.expectOne(`${environment.apiUrl}/v1/me`));
    expect(request.request.headers.get('Authorization')).toBe('Bearer jeton-de-test');
    request.flush({ roles: ['SALARIE', 'MANAGER'] });
    await service.whenSessionRestored();

    expect(service.isAuthenticated()).toBe(true);
    expect(service.getUserRoles()).toEqual(['SALARIE', 'MANAGER']);
  });

  it('n’expose aucun rôle sans session', async () => {
    const service = TestBed.inject(AuthService);
    await service.whenSessionRestored();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.getUserRoles()).toEqual([]);
  });
});
