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
    this.route.queryParams.subscribe(params => {
      const token = params['token'];
      const error = params['error'];

      if (error) {
        this.router.navigate(['/login'], { queryParams: { error: 'oauth2' } });
        return;
      }

      if (token) {
        this.authService.saveTokenAndRedirect(token);
      } else {
        this.router.navigate(['/login']);
      }
    });
  }
}
