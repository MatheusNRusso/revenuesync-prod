import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { AdminUser } from '../../../core/models/admin/admin-dashboard.model';

@Component({
  selector: 'app-admin-users',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './admin-users.html',
  styleUrls: ['./admin-users.scss']
})
export class AdminUsers implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  users: AdminUser[] = [];
  filtered: AdminUser[] = [];
  loading = true;
  error: string | null = null;
  selectedRole = 'ALL';
  roles = ['ALL', 'USER', 'ADMIN'];

  ngOnInit(): void { this.loadUsers(); }

  loadUsers(): void {
    this.loading = true;
    this.error = null;
    this.adminService.getUsers().subscribe({
      next: (data) => {
        this.users = data || [];
        this.applyFilter();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to load users.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  filterBy(role: string): void {
    this.selectedRole = role;
    this.applyFilter();
  }

  private applyFilter(): void {
    this.filtered = this.selectedRole === 'ALL'
      ? [...this.users]
      : this.users.filter(u => u.role === this.selectedRole);
  }

  deleteUser(user: AdminUser): void {
    if (!confirm(`Delete user "${user.name}" (${user.email})? This cannot be undone.`)) return;
    this.adminService.deleteUser(user.id).subscribe({
      next: () => {
        this.users = this.users.filter(u => u.id !== user.id);
        this.applyFilter();
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to delete user.';
        this.cdr.detectChanges();
      }
    });
  }

  toggleUser(user: AdminUser): void {
    const action = user.active ? 'deactivate' : 'activate';
    if (!confirm(`${action} user "${user.name}"?`)) return;
    this.adminService.toggleUser(user.id, !user.active).subscribe({
      next: () => this.loadUsers(),
      error: () => { this.error = `Failed to ${action} user.`; this.cdr.detectChanges(); }
    });
  }

  logout(): void { this.authService.logout(); }
  goTo(path: string): void { this.router.navigate([path]); }

  get userCount(): number { return this.users.filter(u => u.role === 'USER').length; }
  get adminCount(): number { return this.users.filter(u => u.role === 'ADMIN').length; }
  get githubCount(): number { return this.users.filter(u => u.githubUser).length; }
}
