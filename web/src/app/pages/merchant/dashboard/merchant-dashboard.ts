import { Component, OnInit, inject, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

import { ChatApiService } from '../../../core/services/chat.service';
import { ChatComponent } from '../../../shared/components/chat/chat.component';
import { ConversationResponse } from '../../../core/models/merchant-detail.model';

import {
  CreateMerchantProfileRequest,
  MeDashboard,
  MeProfileResponse,
  MerchantDashboardSummary,
  PaymentResponse,
  MeService
} from '../../../core/services/me.service';
import { AuthService } from '../../../core/services/auth.service';
import { ChartCardComponent } from '../../../shared/components/chart-card/chart-card.component';

@Component({
  selector: 'app-merchant-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, ChartCardComponent, ChatComponent],
  templateUrl: './merchant-dashboard.html',
  styleUrls: ['./merchant-dashboard.scss']
})
export class MerchantDashboard implements OnInit, OnDestroy {
  private readonly meService = inject(MeService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly chatService = inject(ChatApiService);
  private readonly http = inject(HttpClient);

  profile: MeProfileResponse | null = null;
  dashboard: MeDashboard | null = null;

  merchants: MerchantDashboardSummary[] = [];
  selectedMerchantId: number | null = null;
  recentPayments: PaymentResponse[] = [];

  visibleWalletMerchantIds = new Set<number>();
  copiedWalletMerchantId: number | null = null;

  visibleEmailMerchantIds = new Set<number>();

  merchantForm: CreateMerchantProfileRequest = this.createEmptyMerchantForm();

  loading = true;
  paymentsLoading = false;
  creatingMerchant = false;
  showCreateMerchantForm = false;

  editingWalletMerchantId: number | null = null;
  walletEditValue = '';
  updatingWallet = false;
  walletEditError: string | null = null;
  walletEditSuccess: string | null = null;

  activeSection: 'dashboard' | 'inbox' = 'dashboard';
  conversations: ConversationResponse[] = [];
  selectedConversation: ConversationResponse | null = null;
  conversationsLoading = false;

  showArchived = false;
  archivedConversations: ConversationResponse[] = [];
  currentUserId: number | null = null;

  error: string | null = null;
  merchantCreateError: string | null = null;
  merchantCreateSuccess: string | null = null;

  isDark = true;

  paymentsChartLabels: string[] = [];
  paymentsChartData: number[] = [];
  revenueChartLabels: string[] = [];
  revenueChartData: number[] = [];

  showPaymentModal = false;
  paymentRequestAmount: number | null = null;
  paymentRequestLoading = false;

  ngOnInit(): void {
    this.loadProfile();
    this.currentUserId = this.authService.getCurrentUserId();
  }

  ngOnDestroy(): void { }

  loadProfile(): void {
    this.loading = true;
    this.error = null;

    this.meService.getProfile().subscribe({
      next: (profile) => {
        this.profile = profile;

        if (!profile.hasMerchants) {
          this.dashboard = null;
          this.merchants = [];
          this.recentPayments = [];
          this.selectedMerchantId = null;
          this.showCreateMerchantForm = true;
          this.resetCharts();
          this.loading = false;
          this.cdr.detectChanges();
          return;
        }

        this.showCreateMerchantForm = false;
        this.loadDashboard();
      },
      error: () => {
        this.error = 'Failed to load profile.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  loadDashboard(): void {
    this.loading = true;
    this.error = null;

    this.meService.getDashboard().subscribe({
      next: (dashboard) => {
        this.dashboard = dashboard;
        this.merchants = dashboard.merchants ?? [];

        if (this.selectedMerchantId !== null) {
          const selectedStillExists = this.merchants.some(
            (merchant) => merchant.id === this.selectedMerchantId
          );
          if (!selectedStillExists) this.selectedMerchantId = null;
        }

        this.loadPayments();
      },
      error: () => {
        this.error = 'Failed to load dashboard.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  selectMerchant(merchantId: number | null): void {
    this.selectedMerchantId = merchantId;
    this.loadPayments();
  }

  private loadPayments(): void {
    this.paymentsLoading = true;

    const paymentsRequest = this.selectedMerchantId === null
      ? this.meService.getPayments(0, 10)
      : this.meService.getMerchantPayments(this.selectedMerchantId, 0, 10);

    paymentsRequest.subscribe({
      next: (paymentsPage) => {
        this.recentPayments = paymentsPage.content;
        this.generateCharts(this.recentPayments);
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

  toggleCreateMerchantForm(): void {
    this.showCreateMerchantForm = !this.showCreateMerchantForm;
    this.merchantCreateError = null;
    this.merchantCreateSuccess = null;

    if (!this.showCreateMerchantForm && this.profile?.hasMerchants) {
      this.resetMerchantForm();
    }

    this.cdr.detectChanges();
  }

  createMerchant(): void {
    const name = this.merchantForm.name.trim();
    const email = this.merchantForm.email.trim();
    const walletAddress = this.merchantForm.walletAddress.trim();
    const description = this.merchantForm.description?.trim() || null;
    const avatarUrl = this.merchantForm.avatarUrl?.trim() || null;

    if (!name || !email || !walletAddress) {
      this.merchantCreateError = 'Name, email and wallet address are required.';
      this.merchantCreateSuccess = null;
      return;
    }

    // Validate Solana wallet address (base58, 32-44 chars)
    const solanaWalletRegex = /^[1-9A-HJ-NP-Za-km-z]{32,44}$/;
    if (!solanaWalletRegex.test(walletAddress)) {
      this.merchantCreateError = 'Invalid Solana wallet address. Must be base58, 32-44 characters.';
      this.merchantCreateSuccess = null;
      return;
    }

    const request: CreateMerchantProfileRequest = {
      name, email, walletAddress, description, avatarUrl,
      defaultAmountSol: this.merchantForm.defaultAmountSol
        ? Number(this.merchantForm.defaultAmountSol)
        : null
    };

    this.creatingMerchant = true;
    this.merchantCreateError = null;
    this.merchantCreateSuccess = null;

    this.meService.createMerchantProfile(request).subscribe({
      next: () => {
        this.merchantCreateSuccess = 'Merchant profile created successfully.';
        this.creatingMerchant = false;
        this.showCreateMerchantForm = false;
        this.resetMerchantForm();

        // Refresh JWT so hasMerchants claim is updated
        this.http.post<{ token: string }>('/api/me/refresh-token', {}).subscribe({
          next: (res) => {
            localStorage.setItem('token', res.token);
            this.router.navigate(['/merchant/dashboard']).then(() => {
              window.location.reload();
            });
          },
          error: () => this.loadProfile()
        });
      },
      error: (err) => {
        this.merchantCreateError = err?.error?.error || 'Failed to create merchant profile.';
        this.creatingMerchant = false;
        this.cdr.detectChanges();
      }
    });
  }

  startWalletEdit(merchant: MerchantDashboardSummary): void {
    this.editingWalletMerchantId = merchant.id;
    this.walletEditValue = merchant.walletAddress ?? '';
    this.walletEditError = null;
    this.walletEditSuccess = null;
    this.cdr.detectChanges();
  }

  cancelWalletEdit(): void {
    this.editingWalletMerchantId = null;
    this.walletEditValue = '';
    this.walletEditError = null;
    this.cdr.detectChanges();
  }

  updateMerchantWallet(): void {
    if (this.editingWalletMerchantId === null) return;

    const walletAddress = this.walletEditValue.trim();

    if (!walletAddress) {
      this.walletEditError = 'Wallet address is required.';
      this.walletEditSuccess = null;
      return;
    }

    this.updatingWallet = true;
    this.walletEditError = null;
    this.walletEditSuccess = null;

    this.meService.updateMerchantWallet(this.editingWalletMerchantId, walletAddress).subscribe({
      next: () => {
        this.walletEditSuccess = 'Wallet updated successfully.';
        this.updatingWallet = false;
        this.editingWalletMerchantId = null;
        this.walletEditValue = '';
        this.loadProfile();
      },
      error: (err) => {
        this.walletEditError = err?.error?.error || 'Failed to update wallet.';
        this.updatingWallet = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleWalletVisibility(merchantId: number, event?: Event): void {
    event?.stopPropagation();
    if (this.visibleWalletMerchantIds.has(merchantId)) {
      this.visibleWalletMerchantIds.delete(merchantId);
    } else {
      this.visibleWalletMerchantIds.add(merchantId);
    }
    this.cdr.detectChanges();
  }

  isWalletVisible(merchantId: number): boolean {
    return this.visibleWalletMerchantIds.has(merchantId);
  }

  formatWallet(walletAddress: string | null | undefined, merchantId: number): string {
    if (!walletAddress) return 'No wallet configured';
    if (this.isWalletVisible(merchantId)) return walletAddress;
    if (walletAddress.length <= 12) return walletAddress;
    return `${walletAddress.slice(0, 4)}...${walletAddress.slice(-4)}`;
  }

  toggleEmailVisibility(merchantId: number, event?: Event): void {
    event?.stopPropagation();
    if (this.visibleEmailMerchantIds.has(merchantId)) {
      this.visibleEmailMerchantIds.delete(merchantId);
    } else {
      this.visibleEmailMerchantIds.add(merchantId);
    }
    this.cdr.detectChanges();
  }

  isEmailVisible(merchantId: number): boolean {
    return this.visibleEmailMerchantIds.has(merchantId);
  }

  formatEmail(email: string | null | undefined, merchantId: number): string {
    if (!email) return 'No email';
    if (this.isEmailVisible(merchantId)) return email;
    const [user, domain] = email.split('@');
    if (!domain) return email;
    const maskedUser = user.length > 2 ? `${user.slice(0, 2)}***` : `${user}***`;
    return `${maskedUser}@${domain}`;
  }

  copyWallet(walletAddress: string | null | undefined, merchantId: number, event?: Event): void {
    event?.stopPropagation();
    if (!walletAddress) return;
    navigator.clipboard.writeText(walletAddress);
    this.copiedWalletMerchantId = merchantId;
    setTimeout(() => {
      this.copiedWalletMerchantId = null;
      this.cdr.detectChanges();
    }, 1800);
  }

  goToSolanaCheckout(merchant: MerchantDashboardSummary): void {
    this.router.navigate(['/solana/checkout'], {
      queryParams: { slug: merchant.slug, context: 'merchant' }
    });
  }

  private generateCharts(payments: PaymentResponse[]): void {
    const byDay: Record<string, { count: number; revenue: number }> = {};
    payments.forEach((payment) => {
      const day = payment.createdAt.substring(0, 10);
      if (!byDay[day]) byDay[day] = { count: 0, revenue: 0 };
      byDay[day].count++;
      byDay[day].revenue += payment.amount;
    });
    const sortedDays = Object.keys(byDay).sort();
    this.paymentsChartLabels = sortedDays;
    this.paymentsChartData = sortedDays.map((day) => byDay[day].count);
    this.revenueChartLabels = sortedDays;
    this.revenueChartData = sortedDays.map((day) => byDay[day].revenue);
  }

  private resetCharts(): void {
    this.paymentsChartLabels = [];
    this.paymentsChartData = [];
    this.revenueChartLabels = [];
    this.revenueChartData = [];
  }

  private resetMerchantForm(): void {
    this.merchantForm = this.createEmptyMerchantForm();
  }

  private createEmptyMerchantForm(): CreateMerchantProfileRequest {
    return { name: '', email: '', description: '', avatarUrl: '', walletAddress: '', defaultAmountSol: null };
  }

  get selectedMerchant(): MerchantDashboardSummary | null {
    if (this.selectedMerchantId === null) return null;
    return this.merchants.find((merchant) => merchant.id === this.selectedMerchantId) ?? null;
  }

  get selectedMerchantLabel(): string {
    return this.selectedMerchant?.name ?? 'All merchants';
  }

  isActive(path: string): boolean { return this.router.url.startsWith(path); }
  goTo(path: string): void { this.router.navigate([path]); }

  showInbox(): void {
    this.activeSection = 'inbox';
    this.loadConversations();
  }

  showDashboard(): void {
    this.activeSection = 'dashboard';
    this.selectedConversation = null;
  }

  toggleArchivedView(): void {
    this.showArchived = !this.showArchived;
    this.selectedConversation = null;
    if (this.showArchived) this.loadArchivedConversations();
    this.cdr.detectChanges();
  }

  loadArchivedConversations(): void {
    this.chatService.getArchivedConversations().subscribe({
      next: (convs) => {
        this.archivedConversations = convs.filter(c => c.messageCount > 0);
        this.cdr.detectChanges();
      },
      error: () => this.cdr.detectChanges()
    });
  }

  unarchiveConversation(conv: ConversationResponse, event?: Event): void {
    event?.stopPropagation();
    this.chatService.unarchiveConversation(conv.id).subscribe({
      next: () => {
        this.archivedConversations = this.archivedConversations.filter(c => c.id !== conv.id);
        if (this.selectedConversation?.id === conv.id) this.selectedConversation = null;
        this.loadConversations();
        this.cdr.detectChanges();
      },
      error: () => this.cdr.detectChanges()
    });
  }

  loadConversations(): void {
    this.conversationsLoading = true;
    this.chatService.getMyConversations().subscribe({
      next: (convs) => {
        this.conversations = convs.filter(c => c.messageCount > 0);
        this.conversationsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.conversationsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  selectConversation(conv: ConversationResponse): void {
    this.selectedConversation =
      this.selectedConversation?.id === conv.id ? null : conv;
    this.cdr.detectChanges();
  }

  get unreadCount(): number {
    return this.conversations.reduce((sum, c) => sum + (c.unreadCount || 0), 0);
  }

  archiveConversation(conv: ConversationResponse, event?: Event): void {
    event?.stopPropagation();
    this.chatService.archiveConversation(conv.id).subscribe({
      next: () => {
        this.conversations = this.conversations.filter(c => c.id !== conv.id);
        if (this.selectedConversation?.id === conv.id) this.selectedConversation = null;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to archive conversation.';
        this.cdr.detectChanges();
      }
    });
  }

  deleteConversation(conv: ConversationResponse, event?: Event): void {
    event?.stopPropagation();
    if (!confirm(`Delete conversation with ${conv.buyerName}?`)) return;
    this.chatService.deleteConversation(conv.id).subscribe({
      next: () => {
        this.conversations = this.conversations.filter(c => c.id !== conv.id);
        if (this.selectedConversation?.id === conv.id) this.selectedConversation = null;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to delete conversation.';
        this.cdr.detectChanges();
      }
    });
  }

  openPaymentModal(): void {
    this.showPaymentModal = true;
    this.paymentRequestAmount = null;
  }

  closePaymentModal(): void {
    this.showPaymentModal = false;
    this.paymentRequestAmount = null;
  }

  sendPaymentRequest(): void {
    if (!this.selectedConversation || !this.paymentRequestAmount) return;
    this.paymentRequestLoading = true;
    this.chatService.sendPaymentRequest(this.selectedConversation.id, this.paymentRequestAmount).subscribe({
      next: () => {
        this.paymentRequestLoading = false;
        this.showPaymentModal = false;
        this.paymentRequestAmount = null;
        this.cdr.detectChanges();
      },
      error: () => {
        this.paymentRequestLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  logout(): void { this.authService.logout(); }

  deleteAccount(): void {
    if (!confirm("Deactivate your account? You will be logged out immediately.")) return;
    this.meService.deleteAccount().subscribe({
      next: () => this.authService.logout(),
      error: () => { this.error = "Failed to deactivate account."; this.cdr.detectChanges(); }
    });
  }

  activateMerchant(merchant: MerchantDashboardSummary, event: Event): void {
    event.stopPropagation();
    this.meService.activateMerchant(merchant.id).subscribe({
      next: () => this.loadProfile(),
      error: () => { this.error = "Failed to activate merchant."; this.cdr.detectChanges(); }
    });
  }

  deactivateMerchant(merchant: MerchantDashboardSummary, event: Event): void {
    event.stopPropagation();
    if (!confirm(`Deactivate merchant "${merchant.name}"?`)) return;
    this.meService.deactivateMerchant(merchant.id).subscribe({
      next: () => this.loadProfile(),
      error: () => { this.error = "Failed to deactivate merchant."; this.cdr.detectChanges(); }
    });
  }

  deleteMerchant(merchant: MerchantDashboardSummary, event: Event): void {
    event.stopPropagation();
    if (!confirm(`Delete merchant "${merchant.name}"? This action cannot be undone.`)) return;
    this.meService.deleteMerchant(merchant.id).subscribe({
      next: () => this.loadProfile(),
      error: () => { this.error = "Failed to delete merchant."; this.cdr.detectChanges(); }
    });
  }

  statusClass(status: string): string { return status.toLowerCase(); }

  setTheme(dark: boolean): void {
    this.isDark = dark;
    document.body.style.background = dark ? '#080c14' : '#f1f5f9';
    this.cdr.detectChanges();
  }

  formatAmount(amount: number, currency: string): string {
    if (currency === 'SOL') return `◎ ${amount.toFixed(6)}`;
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: currency === 'BRL' ? 'BRL' : 'USD'
    }).format(amount);
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(amount);
  }
}
