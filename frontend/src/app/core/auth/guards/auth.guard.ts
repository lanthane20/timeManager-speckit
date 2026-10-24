import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { AuthService } from '../auth.service';

export const authGuard: CanActivateFn = async () => {
  const authService = inject(AuthService);
  await authService.whenSessionRestored();

  if (authService.isAuthenticated()) {
    return true;
  }
  authService.login();
  return false;
};
