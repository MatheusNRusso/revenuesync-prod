import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const roleGuard = (requiredRole: string): CanActivateFn => () => {
  const authService = inject(AuthService);
  const router      = inject(Router);

  if (authService.getRole() === requiredRole) return true;

  router.navigate(['/login']);
  return false;
};
