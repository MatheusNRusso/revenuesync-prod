import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-onboarding',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './onboarding.component.html',
  styleUrls: ['./onboarding.component.scss'],
})
export class OnboardingComponent {

  loading = false;
  error: string | null = null;

  constructor(
    private readonly http:        HttpClient,
    private readonly router:      Router,
    private readonly authService: AuthService,
  ) {}

  /**
   * User chose to consume services (buyer).
   * Marks onboarding complete on the backend then goes to /buyer/history.
   */
  chooseBuyer(): void {
    this.loading = true;
    this.error   = null;

    this.http.patch<{ onboardingCompleted: boolean }>('/api/me/onboarding', {})
      .subscribe({
        next: () => {
          this.loading = false;
          this.router.navigate(['/buyer/history']);
        },
        error: () => {
          this.loading = false;
          this.error   = 'Something went wrong. Please try again.';
        },
      });
  }

  /**
   * User chose to offer their service (merchant).
   * Goes directly to merchant dashboard which auto-opens the create form
   * when hasMerchants = false.
   * Onboarding is marked complete after merchant creation.
   */
  chooseMerchant(): void {
    this.router.navigate(['/merchant/dashboard']);
  }

  logout(): void {
    this.authService.logout();
  }
}
