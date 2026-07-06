import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { Location } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PublicProfileService } from '../../core/services/public-profile.service';
import { ChatApiService } from '../../core/services/chat.service';
import { AuthService } from '../../core/services/auth.service';
import { MerchantDetail } from '../../core/models/merchant-detail.model';

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
  currentUserId: number | null = null;
  isOwnMerchant = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly publicProfileService: PublicProfileService,
    private readonly chatService: ChatApiService,
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef,
    private readonly location: Location
  ) { }

  ngOnInit(): void {
    const merchantId = this.route.snapshot.paramMap.get('id');
    if (!merchantId) {
      this.router.navigate(['/discover']);
      return;
    }
    this.currentUserId = this.authService.getCurrentUserId();
    this.loadMerchantDetail(Number(merchantId));
  }

  private loadMerchantDetail(merchantId: number): void {
    this.publicProfileService.getMerchantDetail(merchantId).subscribe({
      next: (merchant) => {
        this.merchant = merchant;
        this.loading = false;
        // Check if current user owns this merchant
        this.isOwnMerchant = this.currentUserId !== null && merchant.userId === this.currentUserId;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'Merchant not found.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  contactMerchant(): void {
    if (this.isOwnMerchant || !this.merchant) return;

    this.chatService.startConversation(this.merchant.id).subscribe({
      next: (conv) => {
        // Chat lives in the buyer dashboard — redirect there with the conversation
        this.router.navigate(['/buyer/dashboard'], {
          queryParams: { conversationId: conv.id }
        });
      },
      error: () => {
        this.error = 'Failed to start conversation.';
        this.cdr.detectChanges();
      }
    });
  }

  onPayNow(slug: string): void {
    const checkoutPath = `/solana/checkout?slug=${encodeURIComponent(slug)}&context=buyer`;
    this.router.navigateByUrl(checkoutPath);
  }

  goToDiscover(): void {
    this.location.back();
  }
}
