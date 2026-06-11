import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, switchMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

interface MeProfile {
  id:   number;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly TOKEN_KEY = 'token';

  constructor(private http: HttpClient, private router: Router) {}

  // ── Login ──────────────────────────────────────────────────────────────────

  login(email: string, password: string) {
    return this.http.post<{ token: string }>('/auth/login', { email, password })
      .pipe(tap(res => localStorage.setItem(this.TOKEN_KEY, res.token)));
  }

  /**
   * Logs in and redirects.
   *
   * If redirectUrl is a valid internal path (starts with '/'), the user lands
   * there — used by the Discover auth gate so buyers return to checkout.
   *
   * Otherwise:
   *   ADMIN → /dashboard
   *   USER  → /discover
   */
  loginAndRedirect(
    email: string,
    password: string,
    redirectUrl?: string
  ): Observable<any> {
    return this.login(email, password).pipe(
      switchMap(() => {
        if (redirectUrl && redirectUrl.startsWith('/')) {
          this.router.navigateByUrl(redirectUrl);
          return of(null);
        }
        this.redirectByRole();
        return of(null);
      })
    );
  }

  // ── OAuth2 callback ────────────────────────────────────────────────────────

  /**
   * Called by OAuth2CallbackComponent after receiving the token.
   * ADMIN → /dashboard | USER → /discover
   */
  saveTokenAndRedirect(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    this.redirectByRole();
  }

  // ── Role-based redirect ────────────────────────────────────────────────────

  redirectByRole(): void {
    if (this.getRole() === 'ADMIN') {
      this.router.navigate(['/dashboard']);
    } else {
      this.router.navigate(['/discover']);
    }
  }

  // ── Auth state ─────────────────────────────────────────────────────────────

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem(this.TOKEN_KEY);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  getRole(): string | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.role ?? null;
    } catch {
      return null;
    }
  }

  isAdmin(): boolean { return this.getRole() === 'ADMIN'; }
  isUser():  boolean { return this.getRole() === 'USER';  }

  getCurrentUserId(): number | null {
    const token = this.getToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.userId ? Number(payload.userId) : null;
    } catch {
      return null;
    }
  }
}
