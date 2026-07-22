import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-oauth2-callback',
  standalone: true,
  template: `
    <div style="display:flex;align-items:center;justify-content:center;height:100vh;background:#0c1220;color:#a0aec0;font-family:monospace">
      <span>Authenticating...</span>
    </div>
  `
})
export class OAuth2CallbackComponent implements OnInit {

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // JWT arrives in the URL fragment (#token=...): never sent to the server,
    // never logged, never leaked via the Referer header.
    const token = this.extractTokenFromFragment();
    this.clearFragment(); // strip it from the address bar / history

    if (token) {
      this.authService.saveTokenAndRedirect(token);
      return;
    }

    // Defensive fallback for the current backend (query param) and error flows.
    this.route.queryParams.subscribe(params => {
      if (params['error']) {
        this.router.navigate(['/login'], { queryParams: { error: 'oauth2' } });
        return;
      }
      if (params['token']) {
        this.authService.saveTokenAndRedirect(params['token']);
        return;
      }
      this.router.navigate(['/login']);
    });
  }

  private extractTokenFromFragment(): string | null {
    const hash = window.location.hash.replace(/^#/, '');
    return hash ? new URLSearchParams(hash).get('token') : null;
  }

  private clearFragment(): void {
    if (window.location.hash) {
      history.replaceState(null, '', window.location.pathname + window.location.search);
    }
  }
}