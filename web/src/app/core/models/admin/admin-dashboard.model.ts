export type AdminPaymentStatus =
  | 'PENDING'
  | 'SUCCEEDED'
  | 'FAILED'
  | 'REFUNDED'
  | 'PROCESSING'
  | 'UNKNOWN';

export type AdminConversionPlatform = 'META' | 'GOOGLE';

export interface AdminPayment {
  id: number;
  merchantId: number | null;
  merchantName: string | null;
  leadId: number | null;
  customerEmail: string | null;
  externalPaymentId: string;
  amount: number;
  currency: string;
  status: AdminPaymentStatus;
  createdAt: string;
}

export interface AdminLead {
  id: number;
  email: string;
  name: string | null;
  source: string | null;
  userId: number | null;
  walletAddress: string | null;
  createdAt: string;
}

export interface AdminMerchant {
  id: number;
  userId: number | null;
  name: string;
  slug: string;
  email: string;
  walletAddress: string | null;
  active: boolean;
}

export interface AdminConversion {
  id: number;
  paymentId: number | null;
  merchantId: number | null;
  leadId: number | null;
  platform: AdminConversionPlatform;
  value: number;
  currency: string;
  createdAt: string;
}

export interface AdminDashboard {
  totalUsers: number;
  totalMerchants: number;
  totalLeads: number;
  totalPayments: number;
  totalRevenueSol: number;
  totalConversions: number;
  metaConversions: number;
  googleConversions: number;
  latestPayments: AdminPayment[];
  latestLeads: AdminLead[];
  latestMerchants: AdminMerchant[];
  latestConversions: AdminConversion[];
}


export interface AdminUser {
  id: number;
  name: string;
  email: string;
  role: string;
  active: boolean;
  githubUser: boolean;
  createdAt: string;
}