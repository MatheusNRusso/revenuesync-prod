import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrls: ['./register.scss']
})
export class RegisterComponent {

  name         = '';
  email        = '';
  password     = '';
  showPassword = false;
  loading      = false;
  error: string | null   = null;
  success: string | null = null;

  constructor(private http: HttpClient, private router: Router) {}

  togglePassword(): void { this.showPassword = !this.showPassword; }
  goToLogin():    void { this.router.navigate(['/login']); }
  goToLanding():  void { this.router.navigate(['/']); }

  goToDiscover(): void {
    this.router.navigate(['/discover']);
  }

  onSubmit(): void {
    if (!this.name || !this.email || !this.password) return;
    this.loading = true;
    this.error   = null;

    this.http.post('/auth/register', {
      name: this.name,
      email: this.email,
      password: this.password
    }).subscribe({
      next: () => {
        this.success = 'Account created! Redirecting to login...';
        this.loading = false;
        setTimeout(() => this.router.navigate(['/login']), 2000);
      },
      error: (err) => {
        this.error   = err.status === 409
          ? 'Email already registered.'
          : 'Failed to create account. Try again.';
        this.loading = false;
      }
    });
  }
}
