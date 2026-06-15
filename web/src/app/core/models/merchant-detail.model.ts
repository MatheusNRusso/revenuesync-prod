export interface MerchantDetail {
  id: number;
  name: string;
  slug: string;
  description: string;
  avatarUrl: string;
  walletAddress: string;
  defaultAmountSol: number;
  userId: number;
  userDisplayName: string;
  userGithubUsername: string;
  userGithubAvatarUrl: string;
  createdAt: string;
}

export interface ConversationResponse {
  id: number;
  merchantId: number;
  merchantName: string;
  merchantSlug: string;
  buyerId: number;
  buyerName: string;
  status: string;
  unreadCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessageResponse {
  id: number;
  senderId: number;
  senderName: string;
  content: string;
  read: boolean;
  createdAt: string;
  messageType: 'TEXT' | 'PAYMENT_REQUEST';
  paymentToken: string | null;
  paymentAmountSol: number | null;
  paymentStatus: string | null;
}
