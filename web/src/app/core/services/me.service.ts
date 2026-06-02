import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export type UserRole = 'ADMIN' | 'USER';

export interface MerchantProfile {
  id: number;
  name: string;
  email: string;
  slug: string;
  description: string | null;
  avatarUrl: string | null;
  walletAddress: string | null;
  active: boolean;
}

export interface MeProfileResponse {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  hasMerchants: boolean;
  merchants: MerchantProfile[];
}

export interface MerchantDashboardSummary {
  id: number;
  name: string;
  email: string;
  slug: string;
  walletAddress: string | null;
  active: boolean;
  totalPayments: number;
  totalRevenue: number;
  totalRevenueSol: number;
}

export interface MeDashboard {
  userId: number;
  name: string;
  email: string;
  hasMerchants: boolean;
  totalMerchants: number;
  totalPayments: number;
  totalRevenue: number;
  totalRevenueSol: number;
  merchants: MerchantDashboardSummary[];
}

export interface PaymentResponse {
  id: number;
  externalPaymentId: string;
  amount: number;
  currency: string;
  status: string;
  customerEmail: string | null;
  customerName: string | null;
  createdAt: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface CreateMerchantProfileRequest {
  name: string;
  email: string;
  description?: string | null;
  avatarUrl?: string | null;
  walletAddress: string;
  defaultAmountSol?: number | null;
}

export interface UpdateMerchantWalletRequest {
  walletAddress: string;
}

@Injectable({ providedIn: 'root' })
export class MeService {
  constructor(private http: HttpClient) {}

  getProfile(): Observable<MeProfileResponse> {
    return this.http.get<MeProfileResponse>('/api/me/dashboard');
  }

  getDashboard(): Observable<MeDashboard> {
    return this.http.get<MeDashboard>('/api/me/dashboard');
  }

  getPayments(page = 0, size = 20): Observable<PageResponse<PaymentResponse>> {
    return this.http.get<PageResponse<PaymentResponse>>(
      `/api/me/payments?page=${page}&size=${size}`
    );
  }

  getMerchantPayments(
    merchantId: number,
    page = 0,
    size = 20
  ): Observable<PageResponse<PaymentResponse>> {
    return this.http.get<PageResponse<PaymentResponse>>(
      `/api/me/merchants/${merchantId}/payments?page=${page}&size=${size}`
    );
  }

  createMerchantProfile(
    request: CreateMerchantProfileRequest
  ): Observable<MerchantProfile> {
    return this.http.post<MerchantProfile>(
      '/api/me/merchant-profile',
      request
    );
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>("/api/me/account");
  }

  deactivateMerchant(merchantId: number): Observable<void> {
    return this.http.delete<void>(`/api/me/merchants/${merchantId}`);
  }

  updateMerchantWallet(
    merchantId: number,
    walletAddress: string
  ): Observable<MerchantProfile> {
    return this.http.put<MerchantProfile>(
      `/api/me/merchants/${merchantId}/wallet`,
      { walletAddress }
    );
  }
}
