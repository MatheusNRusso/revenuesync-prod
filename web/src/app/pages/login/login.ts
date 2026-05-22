import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.scss']
})
export class LoginComponent implements OnInit, OnDestroy {

  email        = '';
  password     = '';
  loading      = false;
  error: string | null = null;
  showPassword = false;

  comingSoonMsg: string | null = null;

  /**
   * Stores the post-login redirect path read from the ?redirect= query param.
   * Validated to start with '/' to prevent open-redirect attacks.
   * Null if absent or invalid.
   */
  private redirectUrl: string | null = null;

  comingSoon(): void {
    this.comingSoonMsg = 'Social login coming soon. Please use email and password.';
    setTimeout(() => this.comingSoonMsg = null, 3000);
  }

  slides = [
    {
      quote: 'Integrated Solana Pay in minutes. On-chain payments confirmed and conversions firing to Meta automatically.',
      author: 'Ana K.',
      role: 'Growth Lead, Web3 merchant'
    },
    {
      quote: 'Finally a dashboard that shows real on-chain revenue in real-time. The lead capture alone paid for itself.',
      author: 'Marcus R.',
      role: 'Founder, SaaS startup'
    },
    {
      quote: 'Data isolation per merchant is exactly what we needed. Our clients trust us more with Solana Pay.',
      author: 'Julia L.',
      role: 'Agency owner, 12 merchants'
    },
    {
      quote: 'The blockchain-to-conversion pipeline works flawlessly. Google Ads ROAS improved 31% in 2 weeks.',
      author: 'Pedro C.',
      role: 'Performance marketer'
    },
  ];

  activeSlide = 0;
  private slideInterval: any;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Read and validate the post-login redirect path from the query string.
    // Angular automatically URL-decodes query params, so
    // ?redirect=%2Fsolana%2Fcheckout%3Fslug%3Dmy-store
    // arrives here as '/solana/checkout?slug=my-store'.
    const raw = this.route.snapshot.queryParamMap.get('redirect');
    if (raw && raw.startsWith('/')) {
      this.redirectUrl = raw;
    }

    this.slideInterval = setInterval(() => {
      this.activeSlide = (this.activeSlide + 1) % this.slides.length;
      this.cdr.detectChanges();
    }, 4000);
  }

  ngOnDestroy(): void { clearInterval(this.slideInterval); }

  togglePassword(): void { this.showPassword = !this.showPassword; }
  goToRegister():   void { this.router.navigate(['/register']); }
  goToLanding():    void { this.router.navigate(['/']); }

  goToDiscover(): void {
    this.router.navigate(['/discover']);
  }

  onSubmit(): void {
    if (!this.email || !this.password) return;
    this.loading = true;
    this.error   = null;

    // Pass the validated redirect path (or undefined to fall back to role default).
    this.authService.loginAndRedirect(
      this.email,
      this.password,
      this.redirectUrl ?? undefined
    ).subscribe({
      error: () => {
        this.error   = 'Invalid email or password.';
        this.loading = false;
      }
    });
  }
}
