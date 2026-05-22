import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { AdminConversion } from '../../../core/models/admin/admin-dashboard.model';

@Component({
  selector: 'app-admin-conversions',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-conversions.html',
  styleUrls: ['./admin-conversions.scss']
})
export class AdminConversions implements OnInit {

  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  conversions: AdminConversion[] = [];
  filtered: AdminConversion[] = [];

  loading = true;
  error: string | null = null;

  selectedPlatform = 'ALL';
  platforms = ['ALL', 'META', 'GOOGLE'];

  sortColumn = 'createdAt';
  sortDir: 'asc' | 'desc' = 'desc';

  expandedId: number | null = null;

  ngOnInit(): void {
    this.loadConversions();
  }

  loadConversions(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getConversions().subscribe({
      next: (data) => {
        this.conversions = data || [];
        this.applyFilter();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to load admin conversions.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  filterBy(platform: string): void {
    this.selectedPlatform = platform;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.filtered = this.selectedPlatform === 'ALL'
      ? [...this.conversions]
      : this.conversions.filter(conversion => conversion.platform === this.selectedPlatform);

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

  togglePayload(id: number): void {
    this.expandedId = this.expandedId === id ? null : id;
  }

  formatJson(payload: string | null): string {
    if (!payload) return 'Payload was removed from admin DTO.';

    try {
      return JSON.stringify(JSON.parse(payload), null, 2);
    } catch {
      return payload;
    }
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

  platformClass(platform: string): string {
    return platform.toLowerCase();
  }

  get metaCount(): number {
    return this.conversions.filter(conversion => conversion.platform === 'META').length;
  }

  get googleCount(): number {
    return this.conversions.filter(conversion => conversion.platform === 'GOOGLE').length;
  }
}
