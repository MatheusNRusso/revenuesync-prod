import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { environment } from '../../../environments/environment';

/**
 * Gates the direct email/password registration route.
 * Mirrors the backend flag app.auth.allow-direct-registration:
 * in production registration is GitHub-first, so /register redirects to /login.
 * The RegisterComponent itself is untouched and reusable when registration reopens.
 */
export const registrationGuard: CanMatchFn = () => {
  if (environment.allowDirectRegistration) {
    return true;
  }
  return inject(Router).createUrlTree(['/login']);
};
