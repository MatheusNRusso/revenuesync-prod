export type UserRole = 'ADMIN' | 'USER';

export interface MerchantSummary {
  id: number;
  name: string;
  slug: string;
  walletAddress: string | null;
  active: boolean;
}

export interface UserProfile {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  hasMerchants: boolean;
  merchants: MerchantSummary[];
}
