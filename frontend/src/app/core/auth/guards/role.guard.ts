import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth.service';

export const roleGuard: CanActivateFn = async (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const requiredRoles: string[] = route.data['roles'] ?? [];
  await authService.whenSessionRestored();

  if (!authService.isAuthenticated()) {
    authService.login();
    return false;
  }

  const userRoles = authService.getUserRoles();
  const hasRole = requiredRoles.some(role => userRoles.includes(role));
  if (!hasRole) {
    router.navigate(['/forbidden']);
    return false;
  }
  return true;
};
