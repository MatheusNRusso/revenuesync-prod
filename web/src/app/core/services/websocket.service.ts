import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { Client, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';


@Injectable({ providedIn: 'root' })
export class WebSocketService {

  private client: Client | null = null;
  private connected$ = new BehaviorSubject<boolean>(false);
  private messageSubject$ = new Subject<any>();

  constructor() {}

  connect(): Observable<boolean> {
    if (this.client?.active) {
      return this.connected$.asObservable();
    }

    this.client = new Client({
      webSocketFactory: () => new SockJS(`/ws/chat`),
      onConnect: () => {
        this.connected$.next(true);
      },
      onDisconnect: () => {
        this.connected$.next(false);
      },
      onStompError: (error) => {
        console.error('STOMP error:', error);
        this.connected$.next(false);
      }
    });

    this.client.activate();
    return this.connected$.asObservable();
  }

  disconnect(): void {
    if (this.client?.active) {
      this.client.deactivate();
    }
  }

  isConnected(): boolean {
    return this.client?.active ?? false;
  }

  subscribe(conversationId: number): StompSubscription | null {
    if (!this.client?.active) return null;

    return this.client.subscribe(
      `/topic/conversation/${conversationId}`,
      (message) => {
        this.messageSubject$.next(JSON.parse(message.body));
      }
    );
  }

  getMessages(): Observable<any> {
    return this.messageSubject$.asObservable();
  }

  sendMessage(conversationId: number, content: string): void {
    if (!this.client?.active) return;

    this.client.publish({
      destination: `/app/chat.send/${conversationId}`,
      body: JSON.stringify({ content })
    });
  }
}
