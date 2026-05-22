export type Conversion = {
  id: number;
  paymentId: number;
  platform: 'META' | 'GOOGLE';
  value: number;
  currency: string;
  requestPayload: string;
  responsePayload: string;
  createdAt: string;
}


