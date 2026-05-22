import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, interval, switchMap, takeWhile } from 'rxjs';

export interface CreatePaymentRequest {
  amount: number;
  currency: string;          // 'SOL' or SPL Token mint address
  splToken: string;          // optional: USDC mint address etc.
  customerEmail: string;
  label: string;
  message?: string;
  expirationMinutes?: number;
}

export interface CreatePaymentResponse {
  reference: string;
  solanaPayUrl: string;
  qrCodeData: string;
  status: string;
  amount: string;
  currency: string;
  recipientWallet: string;
  expiresAt: string;
  createdAt: string;
}

export interface PaymentStatusResponse {
  reference: string;
  status: 'PENDING' | 'CONFIRMED' | 'EXPIRED' | 'FAILED';
  txSignature: string | null;
  paymentId: number | null;
  confirmedAt: string | null;
  expiresAt: string;
}

@Injectable({ providedIn: 'root' })
export class SolanaPayService {

  private readonly baseUrl = '/api/solana';

  constructor(private http: HttpClient) {}

  /**
   * Creates a new payment and returns the Solana Pay URL.
   */
  createPayment(
    merchantId: number,
    request: CreatePaymentRequest
  ): Observable<CreatePaymentResponse> {
    return this.http.post<CreatePaymentResponse>(
      `${this.baseUrl}/payment/${merchantId}`,
      request
    );
  }

  /**
   * Returns the current status of a payment by its reference.
   */
  getStatus(reference: string): Observable<PaymentStatusResponse> {
    return this.http.get<PaymentStatusResponse>(
      `${this.baseUrl}/status/${reference}`
    );
  }

  /**
   * Polls payment status every `intervalMs` milliseconds until
   * the payment reaches a final state (CONFIRMED, EXPIRED or FAILED).
   *
   * Usage:
   * this.solanaPayService.pollUntilFinal(reference).subscribe(status => {
   *   if (status.status === 'CONFIRMED') { ... }
   * });
   */
  pollUntilFinal(
    reference: string,
    intervalMs = 3000
  ): Observable<PaymentStatusResponse> {
    return interval(intervalMs).pipe(
      switchMap(() => this.getStatus(reference)),
      takeWhile(
        (status) => status.status === 'PENDING',
        true   // emit the last value even when the condition becomes false
      )
    );
  }

  /**
   * Opens the Solana Pay URL in the user's wallet via deep link (mobile).
   * On desktop, the user should scan the QR code with their mobile wallet.
   */
  openInWallet(solanaPayUrl: string): void {
    window.location.href = solanaPayUrl;
  }

  /**
   * Returns a user-friendly label for each payment status.
   */
  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING:   'Waiting for payment...',
      CONFIRMED: 'Payment confirmed ✓',
      EXPIRED:   'Payment expired',
      FAILED:    'Payment failed',
    };
    return labels[status] ?? status;
  }
}
