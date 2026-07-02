import { Component, Input, OnInit, OnDestroy, ChangeDetectorRef, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { interval, Subject, Subscription } from 'rxjs';
import { takeUntil, switchMap } from 'rxjs/operators';
import { ChatApiService } from '../../../core/services/chat.service';
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

  @ViewChild('messagesArea') private messagesArea!: ElementRef;

  messages: ChatMessageResponse[] = [];
  newMessage = '';
  chatLoading = false;
  connected = true;

  private destroy$ = new Subject<void>();
  private pollSub: Subscription | null = null;
  private lastMessageCount = 0;

  constructor(
    private readonly chatService: ChatApiService,
    private readonly cdr: ChangeDetectorRef,
    private readonly router: Router
  ) { }

  ngOnInit(): void {
    if (!this.conversation) return;
    this.loadMessages();
    this.startPolling();
  }

  onPaymentRequest(msg: ChatMessageResponse): void {
    this.router.navigate(['/solana/checkout'], {
      queryParams: {
        slug: this.conversation?.merchantSlug,
        context: 'buyer',
        amount: msg.paymentAmountSol
      }
    });
  }

  get hasConfirmedPayment(): boolean {
    return this.messages.some(m => m.messageType === 'PAYMENT_CONFIRMED');
  }

  isRequestExpired(msg: ChatMessageResponse): boolean {
    if (!msg.createdAt) return false;
    const created = new Date(msg.createdAt).getTime();
    const expiresMs = 5 * 60 * 1000; // 5 minutes, same as backend
    return Date.now() - created > expiresMs;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.messagesArea) {
        const el = this.messagesArea.nativeElement;
        el.scrollTop = el.scrollHeight;
      }
    }, 50);
  }

  private loadMessages(): void {
    if (!this.conversation) return;
    this.chatService.getMessages(this.conversation.id).subscribe({
      next: (msgs) => {
        this.messages = msgs;
        this.lastMessageCount = msgs.length;
        this.cdr.detectChanges();
        this.scrollToBottom();
      }
    });
  }

  private startPolling(): void {
    if (!this.conversation) return;
    const convId = this.conversation.id;

    this.pollSub = interval(3000).pipe(
      takeUntil(this.destroy$),
      switchMap(() => this.chatService.getMessages(convId))
    ).subscribe({
      next: (msgs) => {
        if (msgs.length !== this.lastMessageCount) {
          this.messages = msgs;
          this.lastMessageCount = msgs.length;
          this.cdr.detectChanges();
          this.scrollToBottom();
        }
      },
      error: () => { }
    });
  }

  sendMessage(): void {
    if (!this.conversation || !this.newMessage.trim()) return;

    this.chatLoading = true;
    const content = this.newMessage.trim();
    this.newMessage = '';

    this.chatService.sendMessage(this.conversation.id, content).subscribe({
      next: (msg) => {
        this.messages.push(msg);
        this.lastMessageCount = this.messages.length;
        this.chatLoading = false;
        this.cdr.detectChanges();
        this.scrollToBottom();
      },
      error: () => {
        this.chatLoading = false;
        this.cdr.detectChanges();
      }
    });
  }
}
