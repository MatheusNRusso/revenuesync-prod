import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { HttpClient } from '@angular/common/http';
import { catchError, map, of } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router      = inject(Router);
  const http        = inject(HttpClient);

  if (!authService.isAuthenticated()) {
    router.navigate(['/login']);
    return false;
  }

  return http.get('/auth/validate').pipe(
    map(() => true),
    catchError(() => {
      localStorage.removeItem('token');
      router.navigate(['/login']);
      return of(false);
    })
  );
};
