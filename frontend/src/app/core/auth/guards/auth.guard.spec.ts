import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../auth.service';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
  let authenticated: boolean;
  let restoreSession: () => void;
  const login = vi.fn();

  beforeEach(() => {
    authenticated = false;
    login.mockReset();
    const sessionRestored = new Promise<void>((resolve) => (restoreSession = resolve));
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: {
            whenSessionRestored: () => sessionRestored,
            isAuthenticated: () => authenticated,
            login,
          },
        },
      ],
    });
  });

  const runGuard = () =>
    TestBed.runInInjectionContext(() =>
      authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
    ) as Promise<boolean>;

  it('attend la relecture de la session avant de décider', async () => {
    const decision = runGuard();
    // La session stockée n'est connue qu'une fois relue : rien ne doit partir vers la connexion.
    authenticated = true;
    restoreSession();

    expect(await decision).toBe(true);
    expect(login).not.toHaveBeenCalled();
  });

  it('redirige vers la connexion quand aucune session n’est restaurée', async () => {
    const decision = runGuard();
    restoreSession();

    expect(await decision).toBe(false);
    expect(login).toHaveBeenCalledOnce();
  });
});
