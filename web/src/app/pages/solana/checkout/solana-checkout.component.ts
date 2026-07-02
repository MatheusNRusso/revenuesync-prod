import {
  Component, OnInit, OnDestroy,
  ChangeDetectorRef, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Location } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';

import QRCode from 'qrcode';

export type CheckoutContext = 'buyer' | 'merchant';
type PaymentStatus = 'IDLE' | 'CREATING' | 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'FAILED';

interface CreatePaymentResponse {
  reference: string;
  solanaPayUrl: string;
  status: string;
  amount: string;
  currency: string;
  recipientWallet: string;
  expiresAt: string;
}

interface StatusResponse {
  reference: string;
  status: string;
  txSignature: string | null;
  paymentId: number | null;
  confirmedAt: string | null;
  expiresAt: string;
}

interface MerchantInfo {
  id: number;
  name: string;
  slug: string;
  description: string;
  avatarUrl: string;
}

@Component({
  selector: 'app-solana-checkout',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './solana-checkout.component.html',
  styleUrls: ['./solana-checkout.component.scss'],
})
export class SolanaCheckoutComponent implements OnInit, OnDestroy {

  private http = inject(HttpClient);
  private cdr = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private location = inject(Location);

  // ── Query params ───────────────────────────────────────────────────────────
  slug: string | null = null;
  checkoutContext: CheckoutContext = 'merchant';

  // ── Merchant ───────────────────────────────────────────────────────────────
  merchant: MerchantInfo | null = null;
  loadingMerchant = true;
  merchantError = '';

  // ── Payment state ──────────────────────────────────────────────────────────
  status: PaymentStatus = 'IDLE';
  reference = '';
  solanaPayUrl = '';
  amount = '';
  currency = 'SOL';

  /**
   * Optional amount override entered by merchant or admin before generating QR.
   * If set, it is sent to the backend and overrides the merchant's defaultAmountSol.
   * Buyers cannot set this — the field is only shown in merchant context.
   */
  customAmount = '';
  txSignature: string | null = null;
  confirmedAt: string | null = null;
  errorMsg = '';
  timeLeft = 0;
  copied = false;

  private pollSub?: Subscription;
  private timerSub?: Subscription;

  // ─────────────────────────────────────────────────────────────────────────
  // Lifecycle
  // ─────────────────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.slug = this.route.snapshot.queryParamMap.get('slug');
    const rawContext = this.route.snapshot.queryParamMap.get('context');
    this.checkoutContext = rawContext === 'buyer' ? 'buyer' : 'merchant';

    const presetAmount = this.route.snapshot.queryParamMap.get('amount');
    if (presetAmount) {
      this.customAmount = presetAmount;
    }

    if (!this.slug) {
      this.merchantError = 'No merchant slug provided.';
      this.loadingMerchant = false;
      return;
    }
    this.loadMerchant(this.slug);
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
    this.timerSub?.unsubscribe();
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Merchant loading
  // ─────────────────────────────────────────────────────────────────────────

  private loadMerchant(slug: string): void {
    this.http.get<MerchantInfo>(`/api/public/merchants/${slug}`).subscribe({
      next: (m) => {
        this.merchant = m;
        this.loadingMerchant = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.merchantError = 'Merchant not found.';
        this.loadingMerchant = false;
        this.cdr.detectChanges();
      }
    });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Payment creation
  // ─────────────────────────────────────────────────────────────────────────

  /**
   * Creates a Solana Pay payment request.
   * Amount comes from the merchant's defaultAmountSol (set on backend).
   * Frontend sends empty body — backend resolves the price.
   */
  createPayment(): void {
    if (!this.slug) return;

    this.status = 'CREATING';
    this.errorMsg = '';

    const body = (this.isMerchantContext && this.customAmount && +this.customAmount > 0)
      ? { amount: +this.customAmount }
      : {};

    this.http.post<CreatePaymentResponse>(
      `/api/public/pay/${this.slug}`, body
    ).subscribe({
      next: (res) => {
        this.reference = res.reference;
        this.solanaPayUrl = res.solanaPayUrl;
        this.amount = res.amount;
        this.currency = res.currency;
        this.status = 'PENDING';
        this.generateQrCode(res.solanaPayUrl);
        this.startCountdown();
        this.startPolling();
        this.cdr.detectChanges();
      },
      error: () => {
        this.status = 'IDLE';
        this.errorMsg = 'Failed to create payment. Try again.';
        this.cdr.detectChanges();
      }
    });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // QR code
  // ─────────────────────────────────────────────────────────────────────────
  private generateQrCode(url: string): void {
    setTimeout(() => {
      const container = document.getElementById('checkout-qr-container');
      if (!container) return;

      container.innerHTML = '';

      const canvas = document.createElement('canvas');

      QRCode.toCanvas(canvas, url, {
        width: 260,
        margin: 1,
        color: {
          dark: '#00d4aa',
          light: '#0d1117'
        }
      }, (error) => {
        if (error) {
          container.innerHTML = `<p style="word-break:break-all;font-size:10px">${url}</p>`;
          return;
        }

        container.appendChild(canvas);
        this.cdr.detectChanges();
      });
    }, 150);
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Polling & countdown
  // ─────────────────────────────────────────────────────────────────────────

  private startPolling(): void {
    this.pollSub = interval(3000).pipe(
      switchMap(() =>
        this.http.get<StatusResponse>(`/api/solana/status/${this.reference}`)
      ),
      takeWhile(res => res.status === 'PENDING', true)
    ).subscribe({
      next: (res) => {
        if (res.status !== 'PENDING') {
          this.status = res.status as PaymentStatus;
          this.txSignature = res.txSignature;
          this.confirmedAt = res.confirmedAt;
          this.pollSub?.unsubscribe();
          this.timerSub?.unsubscribe();
          this.cdr.detectChanges();
        }
      },
      error: () => { }
    });
  }

  private startCountdown(): void {
    this.timeLeft = 5 * 60;
    this.timerSub = interval(1000).subscribe(() => {
      this.timeLeft--;
      if (this.timeLeft <= 0) {
        this.status = 'EXPIRED';
        this.timerSub?.unsubscribe();
        this.pollSub?.unsubscribe();
      }
      this.cdr.detectChanges();
    });
  }

  // ─────────────────────────────────────────────────────────────────────────
  // Actions
  // ─────────────────────────────────────────────────────────────────────────

  copyPaymentUrl(): void {
    navigator.clipboard.writeText(this.solanaPayUrl);
    this.copied = true;
    setTimeout(() => { this.copied = false; this.cdr.detectChanges(); }, 2000);
  }

  openInWallet(): void {
    window.open(this.solanaPayUrl, "_blank");
  }
  reset(): void {
    this.pollSub?.unsubscribe();
    this.timerSub?.unsubscribe();
    this.status = 'IDLE';
    this.reference = '';
    this.solanaPayUrl = '';
    this.txSignature = null;
    this.confirmedAt = null;
    this.errorMsg = '';
    this.amount = '';
    this.customAmount = '';
    this.timeLeft = 0;
    this.copied = false;
    this.cdr.detectChanges();
  }

  cancelPayment(): void {
    this.pollSub?.unsubscribe();
    this.timerSub?.unsubscribe();

    this.status = 'IDLE';
    this.reference = '';
    this.solanaPayUrl = '';
    this.txSignature = null;
    this.confirmedAt = null;
    this.errorMsg = '';
    this.amount = '';
    this.timeLeft = 0;
    this.copied = false;

    const container = document.getElementById('checkout-qr-container');
    if (container) {
      container.innerHTML = '';
    }

    this.goBack();
  }

  goToDiscover(): void { this.router.navigate(['/discover']); }
  goToDashboard(): void { this.router.navigate(['/merchant/dashboard']); }

  // ─────────────────────────────────────────────────────────────────────────
  // Template helpers
  // ─────────────────────────────────────────────────────────────────────────

  get isBuyerContext(): boolean { return this.checkoutContext === 'buyer'; }
  get isMerchantContext(): boolean { return this.checkoutContext === 'merchant'; }

  get merchantInitials(): string {
    const name = this.merchant?.name;
    if (!name) return '??';
    const words = name.trim().split(/\s+/);
    return words.length >= 2
      ? (words[0][0] + words[1][0]).toUpperCase()
      : name.slice(0, 2).toUpperCase();
  }

  get timeLeftFormatted(): string {
    const m = Math.floor(this.timeLeft / 60).toString().padStart(2, '0');
    const s = (this.timeLeft % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }

  get shortTx(): string {
    if (!this.txSignature) return '';
    return this.txSignature.substring(0, 8) + '...' + this.txSignature.slice(-8);
  }

  goBack(): void {
    if (this.isBuyerContext) {
      this.location.back();
      return;
    }

    this.router.navigate(['/merchant/dashboard']);
  }
}
