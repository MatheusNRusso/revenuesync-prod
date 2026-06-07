import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PublicProfileService } from '../../core/services/public-profile.service';
import { ChatApiService } from '../../core/services/chat.service';
import { AuthService } from '../../core/services/auth.service';
import { MerchantDetail, ConversationResponse, ChatMessageResponse } from '../../core/models/merchant-detail.model';

@Component({
  selector: 'app-merchant-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './merchant-detail.html',
  styleUrls: ['./merchant-detail.scss']
})
export class MerchantDetailComponent implements OnInit {

  loading = true;
  error: string | null = null;
  merchant: MerchantDetail | null = null;

  conversation: ConversationResponse | null = null;
  messages: ChatMessageResponse[] = [];
  newMessage = '';
  chatLoading = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly publicProfileService: PublicProfileService,
    private readonly chatService: ChatApiService,
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const merchantId = this.route.snapshot.paramMap.get('id');
    if (!merchantId) {
      this.router.navigate(['/discover']);
      return;
    }

    this.loadMerchantDetail(Number(merchantId));
  }

  private loadMerchantDetail(merchantId: number): void {
    this.publicProfileService.getMerchantDetail(merchantId).subscribe({
      next: (merchant) => {
        this.merchant = merchant;
        this.loading = false;
        this.cdr.detectChanges();
        if (this.authService.isAuthenticated()) {
          this.startChat(merchantId);
        }
      },
      error: () => {
        this.error = 'Merchant not found.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private startChat(merchantId: number): void {
    this.chatService.startConversation(merchantId).subscribe({
      next: (conv) => {
        this.conversation = conv;
        this.loadMessages();
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Failed to start conversation.';
        this.cdr.detectChanges();
      }
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
    if (!this.conversation || !this.newMessage.trim()) return;

    this.chatLoading = true;
    this.chatService.sendMessage(this.conversation.id, this.newMessage).subscribe({
      next: (msg) => {
        this.messages.push(msg);
        this.newMessage = '';
        this.chatLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.chatLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onPayNow(slug: string): void {
    const checkoutPath = `/solana/checkout?slug=${encodeURIComponent(slug)}&context=buyer`;
    this.router.navigateByUrl(checkoutPath);
  }

  goToDiscover(): void {
    this.router.navigate(['/discover']);
  }
}
