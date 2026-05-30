import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { AuthService } from '../../../core/services/auth.service';

interface Purchase {
  reference: string;
  merchantId: number;
  merchantName: string;
  amount: string;
  currency: string;
  status: string;
  createdAt: string;
  confirmedAt: string;
}

interface UserProfile {
  hasMerchants?: boolean;
}

@Component({
  selector: 'app-buyer-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './buyer-history.component.html',
  styleUrls: ['./buyer-history.component.scss'],
})
export class BuyerHistoryComponent implements OnInit {

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly authService = inject(AuthService);

  purchases: Purchase[] = [];

  selectedStatus = 'ALL';

  readonly availableStatuses = [
    'ALL',
    'CONFIRMED',
    'PENDING',
    'EXPIRED',
  ];

  loading = true;
  error: string | null = null;

  hasMerchantProfile = false;

  ngOnInit(): void {
    this.loadPurchases();
  }

  get filteredPurchases(): Purchase[] {
    if (this.selectedStatus === 'ALL') {
      return this.purchases;
    }

    return this.purchases.filter(
      purchase => purchase.status?.toUpperCase() === this.selectedStatus
    );
  }

  loadPurchases(): void {
    this.loading = true;
    this.error = null;

    this.http.get<UserProfile>('/api/me/dashboard').subscribe({
      next: (profile) => {
        this.hasMerchantProfile = !!profile?.hasMerchants;
        this.loadPurchaseHistory();
      },
      error: () => {
        this.loading = false;
        this.error = 'Failed to load profile.';
        this.cdr.detectChanges();
      },
    });
  }

  private loadPurchaseHistory(): void {
    this.http.get<Purchase[]>('/api/me/purchases').subscribe({
      next: (data) => {
        this.purchases = [...data].sort((a, b) =>
          new Date(b.createdAt).getTime() -
          new Date(a.createdAt).getTime()
        );

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to load purchase history.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  selectStatus(status: string): void {
    this.selectedStatus = status;
  }

  goToDiscover(): void {
    this.router.navigate(['/discover']);
  }

  goToDashboard(): void {
    if (this.authService.isAdmin()) {
      this.router.navigate(['/dashboard']);
      return;
    }

    this.router.navigate(['/merchant/dashboard']);
  }

  statusClass(status: string): string {
    return status.toLowerCase();
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';

    return new Date(dateStr).toLocaleDateString('pt-BR', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  }
}
