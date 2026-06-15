import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap, switchMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';

interface MeProfile {
  id: number;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly TOKEN_KEY = 'token';

  constructor(private http: HttpClient, private router: Router) { }

  login(email: string, password: string) {
    return this.http.post<{ token: string }>('/auth/login', { email, password })
      .pipe(tap(res => localStorage.setItem(this.TOKEN_KEY, res.token)));
  }

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

  saveTokenAndRedirect(token: string): void {
    localStorage.setItem(this.TOKEN_KEY, token);
    this.redirectByRole();
  }

  redirectByRole(): void {
    if (this.getRole() === 'ADMIN') {
      this.router.navigate(['/dashboard']);
      return;
    }
    const hasMerchants = this.getHasMerchants();
    if (hasMerchants) {
      this.router.navigate(['/merchant/dashboard']);
    } else {
      this.router.navigate(['/buyer/dashboard']);
    }
  }

  getHasMerchants(): boolean {
    const token = this.getToken();
    if (!token) return false;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.hasMerchants === true;
    } catch {
      return false;
    }
  }

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
  isUser(): boolean { return this.getRole() === 'USER'; }

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