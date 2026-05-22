import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { AdminPayment } from '../../../core/models/admin/admin-dashboard.model';

@Component({
  selector: 'app-admin-payments',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-payments.html',
  styleUrls: ['./admin-payments.scss']
})
export class AdminPayments implements OnInit {

  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  payments: AdminPayment[] = [];
  filtered: AdminPayment[] = [];

  loading = true;
  error: string | null = null;

  sortColumn = 'createdAt';
  sortDir: 'asc' | 'desc' = 'desc';

  selectedStatus = 'ALL';
  statuses = ['ALL', 'SUCCEEDED', 'PROCESSING', 'FAILED'];

  ngOnInit(): void {
    this.loadPayments();
  }

  loadPayments(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getPayments().subscribe({
      next: (data) => {
        this.payments = data || [];
        this.applyFilter();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to load admin payments.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  filterBy(status: string): void {
    this.selectedStatus = status;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.filtered = this.selectedStatus === 'ALL'
      ? [...this.payments]
      : this.payments.filter(payment => payment.status === this.selectedStatus);

    this.applySort();
  }

  sort(column: string): void {
    this.sortDir =
      this.sortColumn === column
        ? (this.sortDir === 'asc' ? 'desc' : 'asc')
        : 'asc';

    this.sortColumn = column;
    this.applySort();
  }

  private applySort(): void {
    this.filtered.sort((a, b) => {
      const valueA = (a as any)[this.sortColumn];
      const valueB = (b as any)[this.sortColumn];

      if (valueA == null) return 1;
      if (valueB == null) return -1;

      const diff = String(valueA).localeCompare(
        String(valueB),
        undefined,
        { numeric: true }
      );

      return this.sortDir === 'asc' ? diff : -diff;
    });
  }

  exportCsv(): void {
    this.error = 'Admin CSV export will be implemented in a later block.';
  }

  logout(): void {
    this.authService.logout();
  }

  goTo(path: string): void {
    this.router.navigate([path]);
  }

  statusClass(status: string): string {
    return status.toLowerCase();
  }

  get succeededCount(): number {
    return this.payments.filter(payment => payment.status === 'SUCCEEDED').length;
  }

  get processingCount(): number {
    return this.payments.filter(payment => payment.status === 'PROCESSING').length;
  }

  get failedCount(): number {
    return this.payments.filter(payment => payment.status === 'FAILED').length;
  }
}
