import {
  Component,
  OnInit,
  OnDestroy,
  inject,
  ChangeDetectorRef,
  DestroyRef
} from '@angular/core';

import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

import { interval, Subscription } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../core/services/auth.service';
import { AdminService } from '../../core/services/admin.service';

import {
  AdminDashboard,
  AdminPayment
} from '../../core/models/admin/admin-dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.scss']
})
export class Dashboard implements OnInit, OnDestroy {

  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly destroyRef = inject(DestroyRef);

  dashboard: AdminDashboard | null = null;

  payments: AdminPayment[] = [];

  loading = true;
  error: string | null = null;

  isDark = true;

  sortColumn = 'createdAt';
  sortDir: 'asc' | 'desc' = 'desc';

  paymentsChartLabels: string[] = [];
  paymentsChartData: number[] = [];

  revenueChartLabels: string[] = [];
  revenueChartData: number[] = [];

  private refreshSub?: Subscription;

  ngOnInit(): void {
    this.loadDashboard();

    this.refreshSub = interval(10000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.loading) {
          this.loadDashboard();
        }
      });
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;

    this.adminService.getDashboard().subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;

        this.payments = dashboard.latestPayments || [];

        this.generateCharts();

        this.loading = false;

        this.cdr.detectChanges();
      },

      error: () => {
        this.error = 'Failed to load admin dashboard.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private generateCharts(): void {

    const byDay: Record<string, { count: number; revenue: number }> = {};

    this.payments.forEach(payment => {

      const day = payment.createdAt.substring(0, 10);

      if (!byDay[day]) {
        byDay[day] = {
          count: 0,
          revenue: 0
        };
      }

      byDay[day].count++;

      if (payment.status === 'SUCCEEDED') {
        byDay[day].revenue += payment.amount;
      }
    });

    const sortedDays = Object.keys(byDay).sort();

    this.paymentsChartLabels = sortedDays;

    this.paymentsChartData =
      sortedDays.map(day => byDay[day].count);

    this.revenueChartLabels = sortedDays;

    this.revenueChartData =
      sortedDays.map(day => byDay[day].revenue);
  }

  sort(column: string): void {

    this.sortDir =
      this.sortColumn === column
        ? (this.sortDir === 'asc' ? 'desc' : 'asc')
        : 'asc';

    this.sortColumn = column;
  }

  get sortedPayments(): AdminPayment[] {

    return [...this.payments].sort((a, b) => {

      const va = (a as any)[this.sortColumn];
      const vb = (b as any)[this.sortColumn];

      if (va == null) return 1;
      if (vb == null) return -1;

      const diff = String(va).localeCompare(
        String(vb),
        undefined,
        { numeric: true }
      );

      return this.sortDir === 'asc'
        ? diff
        : -diff;
    });
  }

  logout(): void {
    this.authService.logout();
  }

  goTo(path: string): void {
    this.router.navigate([path]);
  }

  isActive(path: string): boolean {
    return this.router.url === path;
  }

  statusClass(status: string): string {
    return status.toLowerCase();
  }

  get successRate(): string {

    if (!this.dashboard?.totalPayments) {
      return '0%';
    }

    return Math.round(
      (
        this.dashboard.totalConversions /
        this.dashboard.totalPayments
      ) * 100
    ) + '%';
  }

  get maxPayments(): number {
    return Math.max(...this.paymentsChartData, 1);
  }

  getLinePoints(data: number[]): string {

    if (!data.length) {
      return '';
    }

    const max = Math.max(...data, 1);

    const width = 548;
    const height = 174;

    const paddingLeft = 36;
    const paddingTop = 18;

    return data.map((value, index) => {

      const x =
        paddingLeft +
        (index / (data.length - 1)) * width;

      const y =
        paddingTop +
        height -
        (value / max) * height;

      return `${x},${y}`;

    }).join(' ');
  }

  get totalPayments(): number {
    return this.dashboard?.totalPayments ?? 0;
  }

  get totalMerchants(): number {
    return this.dashboard?.totalMerchants ?? 0;
  }

  get totalLeads(): number {
    return this.dashboard?.totalLeads ?? 0;
  }

  get totalConversions(): number {
    return this.dashboard?.totalConversions ?? 0;
  }

  get totalRevenueSol(): number {
    return this.dashboard?.totalRevenueSol ?? 0;
  }

  get metaConversions(): number {
    return this.dashboard?.metaConversions ?? 0;
  }

  get googleConversions(): number {
    return this.dashboard?.googleConversions ?? 0;
  }

  get succeededCount(): number {

    return this.payments.filter(
      payment => payment.status === 'SUCCEEDED'
    ).length;
  }

  get pendingCount(): number {

    return this.payments.filter(
      payment => payment.status === 'PROCESSING'
    ).length;
  }

  get failedCount(): number {

    return this.payments.filter(
      payment => payment.status === 'FAILED'
    ).length;
  }
}
