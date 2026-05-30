import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';

import { PublicProfileService }  from '../../core/services/public-profile.service';
import { AuthService }           from '../../core/services/auth.service';
import { PublicProfile, CATEGORY_LABELS } from '../../core/models/discover/public-profile.model';

@Component({
  selector: 'app-builder',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './builder.html',
  styleUrls: ['./builder.scss'],
})
export class BuilderComponent implements OnInit {

  loading = true;
  error:   string | null = null;
  profile: PublicProfile | null = null;

  readonly CATEGORY_LABELS = CATEGORY_LABELS;

  constructor(
    private readonly route:                ActivatedRoute,
    private readonly router:               Router,
    private readonly publicProfileService: PublicProfileService,
    private readonly authService:          AuthService,
    private readonly cdr:                  ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) { this.router.navigate(['/discover']); return; }

    this.publicProfileService.getProfileBySlug(slug).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error   = 'Profile not found.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  onPayNow(slug: string): void {
    const checkoutPath = `/solana/checkout?slug=${encodeURIComponent(slug)}&context=buyer`;
    if (this.authService.isAuthenticated()) {
      this.router.navigateByUrl(checkoutPath);
    } else {
      this.router.navigate(['/login'], { queryParams: { redirect: checkoutPath } });
    }
  }

  getCategoryLabel(category: string): string {
    return CATEGORY_LABELS[category as keyof typeof CATEGORY_LABELS] ?? category;
  }

  goToDiscover(): void { this.router.navigate(['/discover']); }
}
