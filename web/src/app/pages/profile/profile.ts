import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { AuthService }          from '../../core/services/auth.service';
import { PublicProfileService }  from '../../core/services/public-profile.service';
import { PublicProfile, ProfileCategory, CATEGORY_LABELS } from '../../core/models/discover/public-profile.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.html',
  styleUrls: ['./profile.scss'],
})
export class ProfileComponent implements OnInit {

  loading = true;
  saving  = false;
  error:   string | null = null;
  success: string | null = null;

  profile: PublicProfile | null = null;

  form = {
    displayName: '',
    headline:    '',
    bio:         '',
    location:    '',
    websiteUrl:  '',
    category:    '' as ProfileCategory | '',
    tags:        '',
    isPublic:    false,
  };

  readonly profileCategories: Array<{ value: ProfileCategory | ''; label: string }> = [
    { value: '',                   label: '— select —'  },
    { value: 'DEVELOPER',          label: 'Developer'   },
    { value: 'BACKEND_DEVELOPER',  label: 'Backend'     },
    { value: 'FRONTEND_DEVELOPER', label: 'Frontend'    },
    { value: 'FULLSTACK_DEVELOPER',label: 'Full Stack'  },
    { value: 'DEVOPS',             label: 'DevOps'      },
    { value: 'BLOCKCHAIN_WEB3',    label: 'Web3'        },
    { value: 'DATA_ML',            label: 'Data / ML'   },
    { value: 'DESIGNER',           label: 'Designer'    },
    { value: 'BUSINESS_FOUNDER',   label: 'Founder'     },
    { value: 'AGENCY_STUDIO',      label: 'Agency'      },
  ];

  readonly CATEGORY_LABELS = CATEGORY_LABELS;

  constructor(
    private readonly router:               Router,
    private readonly authService:          AuthService,
    private readonly publicProfileService: PublicProfileService,
    private readonly cdr:                  ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.loading = true;
    this.error   = null;
    this.publicProfileService.getMyProfile().subscribe({
      next: (profile) => {
        this.profile = profile;
        this.form = {
          displayName: profile.displayName  || '',
          headline:    profile.headline     || '',
          bio:         profile.bio          || '',
          location:    profile.location     || '',
          websiteUrl:  profile.websiteUrl   || '',
          category:    profile.category     || '',
          tags:        profile.tags?.join(', ') || '',
          isPublic:    profile.isPublic,
        };
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error   = 'Failed to load profile.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  save(): void {
    this.saving  = true;
    this.error   = null;
    this.success = null;

    const tags = this.form.tags
      .split(',')
      .map(t => t.trim())
      .filter(t => t.length > 0);

    const payload = {
      displayName: this.form.displayName || undefined,
      headline:    this.form.headline    || undefined,
      bio:         this.form.bio         || undefined,
      location:    this.form.location    || undefined,
      websiteUrl:  this.form.websiteUrl  || undefined,
      category:    (this.form.category || undefined) as ProfileCategory | undefined,
      tags,
      isPublic:    this.form.isPublic,
    };

    this.publicProfileService.upsertMyProfile(payload).subscribe({
      next: (profile) => {
        this.profile = profile;
        this.saving  = false;
        this.success = 'Profile saved successfully.';
        this.cdr.detectChanges();
        setTimeout(() => { this.success = null; this.cdr.detectChanges(); }, 3000);
      },
      error: () => {
        this.error  = 'Failed to save profile.';
        this.saving = false;
        this.cdr.detectChanges();
      },
    });
  }

  connectGitHub(): void {
    window.location.href = '/oauth2/authorization/github';
  }

  goToDiscover(): void { this.router.navigate(['/discover']); }
  isAuthenticated(): boolean { return this.authService.isAuthenticated(); }
}
