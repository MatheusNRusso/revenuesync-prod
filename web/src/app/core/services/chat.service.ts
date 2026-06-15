import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ConversationResponse, ChatMessageResponse } from '../models/merchant-detail.model';

@Injectable({ providedIn: 'root' })
export class ChatApiService {

  private readonly baseUrl = '/api/chats';

  constructor(private readonly http: HttpClient) { }

  startConversation(merchantId: number): Observable<ConversationResponse> {
    return this.http.post<ConversationResponse>(`${this.baseUrl}/start`, { merchantId });
  }

  getMyConversations(): Observable<ConversationResponse[]> {
    return this.http.get<ConversationResponse[]>(this.baseUrl);
  }

  getMessages(conversationId: number): Observable<ChatMessageResponse[]> {
    return this.http.get<ChatMessageResponse[]>(`${this.baseUrl}/${conversationId}/messages`);
  }

  sendMessage(conversationId: number, content: string): Observable<ChatMessageResponse> {
    return this.http.post<ChatMessageResponse>(
      `${this.baseUrl}/${conversationId}/messages`, { content }
    );
  }

  markAsRead(conversationId: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/${conversationId}/read`, {});
  }

  closeConversation(conversationId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${conversationId}`);
  }

  sendPaymentRequest(conversationId: number, amountSol: number): Observable<ChatMessageResponse> {
    return this.http.post<ChatMessageResponse>(
      `${this.baseUrl}/${conversationId}/payment-request`,
      { amountSol }
    );
  }
}
