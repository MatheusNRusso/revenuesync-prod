import { Component, Input, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { ChatApiService } from '../../../core/services/chat.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { ChatMessageResponse, ConversationResponse } from '../../../core/models/merchant-detail.model';

@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat.component.html',
  styleUrls: ['./chat.component.scss']
})
export class ChatComponent implements OnInit, OnDestroy {

  @Input() conversation: ConversationResponse | null = null;
  @Input() buyerId: number | null = null;

  messages: ChatMessageResponse[] = [];
  newMessage = '';
  chatLoading = false;
  connected = false;

  private messageSubscription: Subscription | null = null;
  private destroy$ = new Subject<void>();

  constructor(
    private readonly chatService: ChatApiService,
    private readonly wsService: WebSocketService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (!this.conversation) return;

    this.loadMessages();
    this.connectWebSocket();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    if (this.messageSubscription) {
      this.messageSubscription.unsubscribe();
    }
  }

  private connectWebSocket(): void {
    this.wsService.connect().subscribe((connected) => {
      this.connected = connected;
      if (connected && this.conversation) {
        this.subscribeToMessages();
      }
      this.cdr.detectChanges();
    });
  }

  private subscribeToMessages(): void {
    if (!this.conversation) return;

    this.wsService.subscribe(this.conversation.id);
    this.messageSubscription = this.wsService.getMessages().subscribe((message) => {
      this.messages.push(message);
      this.cdr.detectChanges();
    });
  }

  private loadMessages(): void {
    if (!this.conversation) return;

    this.chatService.getMessages(this.conversation.id).subscribe({
      next: (msgs) => {
        this.messages = msgs;
        this.cdr.detectChanges();
      }
    });
  }

  sendMessage(): void {
    if (!this.conversation || !this.newMessage.trim() || !this.connected) return;

    this.chatLoading = true;
    const content = this.newMessage.trim();
    this.newMessage = '';

    this.wsService.sendMessage(this.conversation.id, content);
    this.chatLoading = false;
    this.cdr.detectChanges();
  }
}
