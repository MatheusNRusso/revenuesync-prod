export interface MerchantProfile {
  id: number;
  name: string;
  slug: string;
  description: string | null;
  avatarUrl: string | null;
  walletAddress: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMerchantRequest {
  name: string;
  slug: string;
  description?: string;
  walletAddress?: string;
}

export interface UpdateWalletRequest {
  walletAddress: string;
}
