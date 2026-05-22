import {
  Component, OnInit, OnDestroy,
  ChangeDetectorRef, inject
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { interval, Subscription } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import { PublicMerchant } from '../../core/models/pay.model';

import QRCode from 'qrcode';

type PaymentStatus = 'IDLE' | 'CREATING' | 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'FAILED';

interface CreatePaymentResponse {
  reference:       string;
  solanaPayUrl:    string;
  status:          string;
  amount:          string;
  currency:        string;
  recipientWallet: string;
  expiresAt:       string;
}

interface StatusResponse {
  reference:   string;
  status:      string;
  txSignature: string | null;
  paymentId:   number | null;
  confirmedAt: string | null;
  expiresAt:   string;
}

@Component({
  selector: 'app-pay',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pay.component.html',
  styleUrls: ['./pay.component.scss']
})
export class PayComponent implements OnInit, OnDestroy {

  private http  = inject(HttpClient);
  private cdr   = inject(ChangeDetectorRef);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  merchant: PublicMerchant | null = null;
  loadingMerchant = true;
  merchantError   = '';

  amount   = '';
  currency = 'SOL';
  email    = '';

  status: PaymentStatus      = 'IDLE';
  reference                  = '';
  solanaPayUrl               = '';
  txSignature: string | null = null;
  confirmedAt: string | null = null;
  errorMsg                   = '';
  timeLeft                   = 0;
  copied                     = false;

  private pollSub?:  Subscription;
  private timerSub?: Subscription;

  ngOnInit(): void {
    const slug = this.route.snapshot.paramMap.get('slug');
    if (!slug) { this.router.navigate(['/']); return; }
    this.loadMerchant(slug);
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
    this.timerSub?.unsubscribe();
  }

  private loadMerchant(slug: string): void {
    this.http.get<PublicMerchant>(`/api/public/merchants/${slug}`).subscribe({
      next: (m) => {
        this.merchant       = m;
        this.loadingMerchant = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.merchantError   = 'Merchant not found.';
        this.loadingMerchant = false;
        this.cdr.detectChanges();
      }
    });
  }

  createPayment(): void {
    if (!this.email || !this.email.includes('@')) {
      this.errorMsg = 'Enter a valid email.';
      return;
    }
    if (!this.amount || isNaN(+this.amount) || +this.amount <= 0) {
      this.errorMsg = 'Enter a valid amount.';
      return;
    }

    this.status   = 'CREATING';
    this.errorMsg = '';

    this.http.post<CreatePaymentResponse>(
      `/api/public/pay/${this.merchant!.slug}`,
      {
        amount:            +this.amount,
        currency:          this.currency,
        customerEmail:     this.email,
        label:             `Pay ${this.merchant!.name}`,
        message:           `Pay ${this.amount} SOL to ${this.merchant!.name}`,
        expirationMinutes: 15
      }
    ).subscribe({
      next: (res) => {
        this.reference    = res.reference;
        this.solanaPayUrl = res.solanaPayUrl;
        this.status       = 'PENDING';
        this.generateQrCode(res.solanaPayUrl);
        this.startCountdown();
        this.startPolling();
        this.cdr.detectChanges();
      },
      error: () => {
        this.status   = 'IDLE';
        this.errorMsg = 'Failed to create payment. Try again.';
        this.cdr.detectChanges();
      }
    });
  }

  private generateQrCode(url: string): void {
    setTimeout(() => {
      const container = document.getElementById('pay-qr-container');
      if (!container) return;

      container.innerHTML = '';

      const canvas = document.createElement('canvas');

      QRCode.toCanvas(canvas, url, {
        width: 280,
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
    }, 100);
  }
  private startPolling(): void {
    this.pollSub = interval(3000).pipe(
      switchMap(() =>
        this.http.get<StatusResponse>(`/api/solana/status/${this.reference}`)
      ),
      takeWhile(res => res.status === 'PENDING', true)
    ).subscribe({
      next: (res) => {
        if (res.status !== 'PENDING') {
          this.status      = res.status as PaymentStatus;
          this.txSignature = res.txSignature;
          this.confirmedAt = res.confirmedAt;
          this.pollSub?.unsubscribe();
          this.timerSub?.unsubscribe();
          this.cdr.detectChanges();
        }
      },
      error: () => {}
    });
  }

  private startCountdown(): void {
    this.timeLeft = 15 * 60;
    this.timerSub = interval(1000).subscribe(() => {
      this.timeLeft--;
      if (this.timeLeft <= 0) this.timerSub?.unsubscribe();
      this.cdr.detectChanges();
    });
  }

  get timeLeftFormatted(): string {
    const m = Math.floor(this.timeLeft / 60).toString().padStart(2, '0');
    const s = (this.timeLeft % 60).toString().padStart(2, '0');
    return `${m}:${s}`;
  }

  copyPaymentUrl(): void {
    navigator.clipboard.writeText(this.solanaPayUrl);
    this.copied = true;
    setTimeout(() => this.copied = false, 2000);
  }

  openInWallet(): void { window.location.href = this.solanaPayUrl; }

  reset(): void {
    this.pollSub?.unsubscribe();
    this.timerSub?.unsubscribe();
    this.status       = 'IDLE';
    this.reference    = '';
    this.solanaPayUrl = '';
    this.txSignature  = null;
    this.confirmedAt  = null;
    this.errorMsg     = '';
    this.amount       = '';
    this.timeLeft     = 0;
    this.copied       = false;
    this.cdr.detectChanges();
  }

  get shortTx(): string {
    if (!this.txSignature) return '';
    return this.txSignature.substring(0, 8) + '...' + this.txSignature.slice(-8);
  }

  goToLanding(): void { this.router.navigate(['/']); }
}
