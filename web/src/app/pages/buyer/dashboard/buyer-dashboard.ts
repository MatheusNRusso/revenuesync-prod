import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { ChatApiService } from '../../../core/services/chat.service';
import { AuthService } from '../../../core/services/auth.service';
import { ConversationResponse } from '../../../core/models/merchant-detail.model';
import { ChatComponent } from '../../../shared/components/chat/chat.component';

@Component({
  selector: 'app-buyer-dashboard',
  standalone: true,
  imports: [CommonModule, ChatComponent],
  templateUrl: './buyer-dashboard.html',
  styleUrls: ['./buyer-dashboard.scss']
})
export class BuyerDashboard implements OnInit {
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly chatService = inject(ChatApiService);
  private readonly authService = inject(AuthService);
  private readonly cdr = inject(ChangeDetectorRef);

  conversations: ConversationResponse[] = [];
  selectedConversation: ConversationResponse | null = null;
  currentUserId: number | null = null;
  loading = true;

  ngOnInit(): void {
    this.currentUserId = this.authService.getCurrentUserId();
    this.loadConversations();
  }

  loadConversations(): void {
    this.chatService.getMyConversations().subscribe({
      next: (convs) => {
        this.conversations = convs;
        this.loading = false;

        // Auto-select conversation if coming from Discover/merchant-detail
        const targetId = this.route.snapshot.queryParamMap.get('conversationId');
        if (targetId) {
          const match = convs.find(c => c.id === Number(targetId));
          if (match) {
            this.selectedConversation = match;
          }
        }

        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  selectConversation(conv: ConversationResponse): void {
    this.selectedConversation = conv;
    this.cdr.detectChanges();
  }

  goTo(path: string): void {
    this.router.navigate([path]);
  }

  logout(): void {
    this.authService.logout();
  }
}
