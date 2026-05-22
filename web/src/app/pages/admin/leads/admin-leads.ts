import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';

import {
  AdminLead
} from '../../../core/models/admin/admin-dashboard.model';

@Component({
  selector: 'app-admin-leads',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-leads.html',
  styleUrls: ['./admin-leads.scss']
})
export class AdminLeads implements OnInit {

  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  leads: AdminLead[] = [];

  loading = true;
  error: string | null = null;

  totalElements = 0;

  page = 0;
  size = 20;
  totalPages = 0;

  sortColumn = 'createdAt';

  sortDir: 'asc' | 'desc' = 'desc';

  ngOnInit(): void {
    this.loadLeads();
  }

  loadLeads(): void {

    this.loading = true;
    this.error = null;

    this.adminService.getLeads().subscribe({

      next: (data) => {

        const sorted = [...(data || [])].sort((a, b) =>
          String(b.createdAt).localeCompare(String(a.createdAt))
        );

        this.totalElements = sorted.length;

        this.totalPages = Math.max(
          Math.ceil(sorted.length / this.size),
          1
        );

        this.leads = sorted.slice(
          this.page * this.size,
          this.page * this.size + this.size
        );

        this.loading = false;

        this.cdr.detectChanges();
      },

      error: () => {

        this.error = 'Failed to load admin leads.';
        this.loading = false;

        this.cdr.detectChanges();
      }
    });
  }

  prevPage(): void {

    if (this.page > 0) {
      this.page--;
      this.loadLeads();
    }
  }

  nextPage(): void {

    if (this.page < this.totalPages - 1) {
      this.page++;
      this.loadLeads();
    }
  }

  sort(column: string): void {

    this.sortDir =
      this.sortColumn === column
        ? (this.sortDir === 'asc' ? 'desc' : 'asc')
        : 'asc';

    this.sortColumn = column;

    this.leads.sort((a, b) => {

      const valueA = (a as any)[column];
      const valueB = (b as any)[column];

      if (valueA == null) return 1;
      if (valueB == null) return -1;

      const diff = String(valueA).localeCompare(
        String(valueB),
        undefined,
        { numeric: true }
      );

      return this.sortDir === 'asc'
        ? diff
        : -diff;
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

  sourceClass(source: string | null): string {

    return source
        ?.toLowerCase()
        .replace('_', '-')
      || 'unknown';
  }
}
