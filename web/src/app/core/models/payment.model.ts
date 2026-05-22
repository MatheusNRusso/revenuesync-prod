export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'REFUNDED' | 'PROCESSING';

export type Payment = {
  id: number;
  externalPaymentId: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  customerName: string | null;
  customerEmail: string | null;
  merchantId: number | null;
  leadId: number | null;
  createdAt: string;
};

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
