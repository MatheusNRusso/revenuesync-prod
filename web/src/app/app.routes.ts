import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';
import { registrationGuard } from './core/guards/registration.guard';
import { OAuth2CallbackComponent } from './pages/oauth2/callback/oauth2-callback.component';
import { PayComponent } from './pages/pay/pay.component';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/landing/landing').then(m => m.LandingComponent)
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./pages/login/login').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./pages/register/register').then(m => m.RegisterComponent),
    canMatch: [registrationGuard]
  },

  // ── Onboarding — new users choose buyer or merchant intent ─────────────────
  {
    path: 'onboarding',
    loadComponent: () =>
      import('./pages/onboarding/onboarding.component')
        .then(m => m.OnboardingComponent),
    canActivate: [authGuard]
  },

  // ── Buyer history — purchase extrato for non-merchant users ────────────────
  {
    path: 'buyer/history',
    loadComponent: () =>
      import('./pages/buyer/history/buyer-history.component')
        .then(m => m.BuyerHistoryComponent),
    canActivate: [authGuard]
  },

  // ── Buyer dashboard ────────────────────────────────────────────────────────
  {
    path: 'buyer/dashboard',
    loadComponent: () =>
      import('./pages/buyer/dashboard/buyer-dashboard')
        .then(m => m.BuyerDashboard),
    canActivate: [authGuard]
  },

  // ── Admin ──────────────────────────────────────────────────────────────────
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./pages/dashboard/dashboard').then(m => m.Dashboard),
    canActivate: [authGuard, roleGuard('ADMIN')]
  },
  {
    path: 'admin/conversions',
    loadComponent: () =>
      import('./pages/admin/conversions/admin-conversions')
        .then(m => m.AdminConversions),
    canActivate: [authGuard, roleGuard('ADMIN')]
  },
  {
    path: 'admin/payments',
    loadComponent: () =>
      import('./pages/admin/payments/admin-payments')
        .then(m => m.AdminPayments),
    canActivate: [authGuard, roleGuard('ADMIN')]
  },
  {
    path: 'admin/leads',
    loadComponent: () =>
      import('./pages/admin/leads/admin-leads').then(m => m.AdminLeads),
    canActivate: [authGuard, roleGuard('ADMIN')]
  },
  {
    path: 'admin/merchants',
    loadComponent: () =>
      import('./pages/admin/merchants/admin-merchants')
        .then(m => m.AdminMerchants),
    canActivate: [authGuard, roleGuard('ADMIN')]
  },

  // ── Merchant ───────────────────────────────────────────────────────────────
  {
    path: 'merchant/dashboard',
    loadComponent: () =>
      import('./pages/merchant/dashboard/merchant-dashboard')
        .then(m => m.MerchantDashboard),
    canActivate: [authGuard, roleGuard('USER')]
  },
  {
    path: 'merchant/payments',
    loadComponent: () =>
      import('./pages/merchant/payments/merchant-payments')
        .then(m => m.MerchantPayments),
    canActivate: [authGuard, roleGuard('USER')]
  },

  // ── Checkout ───────────────────────────────────────────────────────────────
  {
    path: 'solana/checkout',
    loadComponent: () =>
      import('./pages/solana/checkout/solana-checkout.component')
        .then(m => m.SolanaCheckoutComponent),
    canActivate: [authGuard]
  },

  // ── Public ─────────────────────────────────────────────────────────────────
  { path: 'pay/:slug', component: PayComponent },
  { path: 'oauth2/callback', component: OAuth2CallbackComponent },
  {
    path: 'u/:slug',
    loadComponent: () =>
      import('./pages/builder/builder').then(m => m.BuilderComponent)
  },
  {
    path: 'profile',
    loadComponent: () =>
      import('./pages/profile/profile').then(m => m.ProfileComponent),
    canActivate: [authGuard]
  },
  {
    path: 'discover',
    loadComponent: () =>
      import('./pages/discover/discover.component')
        .then(m => m.DiscoverComponent)
  },

  {
    path: 'merchant/:id',
    loadComponent: () =>
      import('./pages/merchant-detail/merchant-detail').then(m => m.MerchantDetailComponent),
    canActivate: [authGuard]
  },

  {
    path: 'admin/users',
    loadComponent: () =>
      import('./pages/admin/users/admin-users')
        .then(m => m.AdminUsers),
    canActivate: [authGuard, roleGuard('ADMIN')]
  },
  { path: '**', redirectTo: '' }
];
