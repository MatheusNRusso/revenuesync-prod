import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import {
  MeDashboard,
  MerchantDashboardSummary,
  PaymentResponse,
  MeService
} from '../../../core/services/me.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-merchant-payments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './merchant-payments.html',
  styleUrls: ['./merchant-payments.scss']
})
export class MerchantPayments implements OnInit {
  private readonly meService = inject(MeService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  dashboard: MeDashboard | null = null;
  merchants: MerchantDashboardSummary[] = [];

  payments: PaymentResponse[] = [];
  filtered: PaymentResponse[] = [];

  loading = true;
  paymentsLoading = false;
  error: string | null = null;

  selectedMerchantId: number | null = null;
  selectedStatus = 'ALL';
  statuses = ['ALL', 'SUCCEEDED', 'PROCESSING', 'FAILED'];

  sortColumn: keyof PaymentResponse = 'createdAt';
  sortDir: 'asc' | 'desc' = 'desc';

  page = 0;
  size = 20;
  totalElements = 0;
  totalPages = 0;
  numberOfElements = 0;

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;

    this.meService.getDashboard().subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.merchants = dashboard.merchants ?? [];
        this.loadPayments();
      },
      error: () => {
        this.error = 'Failed to load dashboard.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadPayments(): void {
    this.paymentsLoading = true;
    this.error = null;

    const request$ = this.selectedMerchantId === null
      ? this.meService.getPayments(this.page, this.size)
      : this.meService.getMerchantPayments(this.selectedMerchantId, this.page, this.size);

    request$.subscribe({
      next: (pageResponse) => {
        this.payments = pageResponse.content;
        this.totalElements = pageResponse.totalElements;
        this.totalPages = pageResponse.totalPages;
        this.numberOfElements = pageResponse.numberOfElements;

        this.applyFilter();

        this.loading = false;
        this.paymentsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to load payments.';
        this.loading = false;
        this.paymentsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  selectMerchant(merchantId: number | null): void {
    this.selectedMerchantId = merchantId;
    this.page = 0;
    this.loadPayments();
  }

  filterBy(status: string): void {
    this.selectedStatus = status;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.filtered = this.selectedStatus === 'ALL'
      ? [...this.payments]
      : this.payments.filter((payment) => payment.status === this.selectedStatus);

    this.applySort();
  }

  sort(column: keyof PaymentResponse): void {
    this.sortDir = this.sortColumn === column
      ? this.sortDir === 'asc' ? 'desc' : 'asc'
      : 'asc';

    this.sortColumn = column;
    this.applySort();
  }

  private applySort(): void {
    this.filtered.sort((a, b) => {
      const valueA = a[this.sortColumn];
      const valueB = b[this.sortColumn];

      if (valueA == null) return 1;
      if (valueB == null) return -1;

      const direction = this.sortDir === 'asc' ? 1 : -1;

      if (typeof valueA === 'number' && typeof valueB === 'number') {
        return (valueA - valueB) * direction;
      }

      return String(valueA).localeCompare(String(valueB), undefined, { numeric: true }) * direction;
    });
  }

  previousPage(): void {
    if (this.page === 0) {
      return;
    }

    this.page--;
    this.loadPayments();
  }

  nextPage(): void {
    if (this.page + 1 >= this.totalPages) {
      return;
    }

    this.page++;
    this.loadPayments();
  }

  goToSolanaCheckout(merchant: MerchantDashboardSummary): void {
    this.router.navigate(['/solana/checkout'], {
      queryParams: {
        merchantId: merchant.id,
        slug: merchant.slug
      }
    });
  }

  get selectedMerchant(): MerchantDashboardSummary | null {
    if (this.selectedMerchantId === null) {
      return null;
    }

    return this.merchants.find((merchant) => merchant.id === this.selectedMerchantId) ?? null;
  }

  get selectedMerchantLabel(): string {
    return this.selectedMerchant?.name ?? 'All merchants';
  }

  get succeededCount(): number {
    return this.payments.filter((payment) => payment.status === 'SUCCEEDED').length;
  }

  get processingCount(): number {
    return this.payments.filter((payment) => payment.status === 'PROCESSING').length;
  }

  get failedCount(): number {
    return this.payments.filter((payment) => payment.status === 'FAILED').length;
  }

  logout(): void {
    this.authService.logout();
  }

  goTo(path: string): void {
    this.router.navigate([path]);
  }

  isActive(path: string): boolean {
    return this.router.url.startsWith(path);
  }

  statusClass(status: string): string {
    return status.toLowerCase();
  }

  shortExternalId(externalPaymentId: string): string {
    if (!externalPaymentId) {
      return '-';
    }

    if (externalPaymentId.length <= 26) {
      return externalPaymentId;
    }

    return externalPaymentId.slice(0, 14) + '...' + externalPaymentId.slice(-10);
  }
}
