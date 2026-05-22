import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { AdminMerchant } from '../../../core/models/admin/admin-dashboard.model';

@Component({
  selector: 'app-admin-merchants',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-merchants.html',
  styleUrls: ['./admin-merchants.scss']
})
export class AdminMerchants implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  merchants: AdminMerchant[] = [];
  filtered: AdminMerchant[] = [];

  loading = true;
  error: string | null = null;

  selectedStatus = 'ALL';
  statuses = ['ALL', 'ACTIVE', 'INACTIVE'];

  sortColumn = 'id';
  sortDir: 'asc' | 'desc' = 'desc';

  ngOnInit(): void {
    this.loadMerchants();
  }

  loadMerchants(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getMerchants().subscribe({
      next: (data) => {
        this.merchants = data || [];
        this.applyFilter();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to load admin merchants.';
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
    if (this.selectedStatus === 'ACTIVE') {
      this.filtered = this.merchants.filter((merchant) => merchant.active);
    } else if (this.selectedStatus === 'INACTIVE') {
      this.filtered = this.merchants.filter((merchant) => !merchant.active);
    } else {
      this.filtered = [...this.merchants];
    }

    this.applySort();
  }

  sort(column: string): void {
    this.sortDir =
      this.sortColumn === column
        ? this.sortDir === 'asc' ? 'desc' : 'asc'
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

  logout(): void {
    this.authService.logout();
  }

  goTo(path: string): void {
    this.router.navigate([path]);
  }

  statusClass(active: boolean): string {
    return active ? 'succeeded' : 'failed';
  }

  get activeCount(): number {
    return this.merchants.filter((merchant) => merchant.active).length;
  }

  get inactiveCount(): number {
    return this.merchants.filter((merchant) => !merchant.active).length;
  }
}
