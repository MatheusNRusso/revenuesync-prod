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

  showArchived = false;
  archivedConversations: ConversationResponse[] = [];
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

  toggleArchivedView(): void {
    this.showArchived = !this.showArchived;
    this.selectedConversation = null;
    if (this.showArchived) this.loadArchivedConversations();
    this.cdr.detectChanges();
  }

  loadArchivedConversations(): void {
    this.chatService.getArchivedConversations().subscribe({
      next: (convs) => {
        this.archivedConversations = convs.filter(c => c.messageCount > 0);
        this.cdr.detectChanges();
      },
      error: () => this.cdr.detectChanges()
    });
  }

  unarchiveConversation(conv: ConversationResponse, event?: Event): void {
    event?.stopPropagation();
    this.chatService.unarchiveConversation(conv.id).subscribe({
      next: () => {
        this.archivedConversations = this.archivedConversations.filter(c => c.id !== conv.id);
        if (this.selectedConversation?.id === conv.id) this.selectedConversation = null;
        this.loadConversations();
        this.cdr.detectChanges();
      },
      error: () => this.cdr.detectChanges()
    });
  }

  selectConversation(conv: ConversationResponse): void {
    this.selectedConversation =
      this.selectedConversation?.id === conv.id ? null : conv;
    this.cdr.detectChanges();
  }

  archiveConversation(conv: ConversationResponse, event?: Event): void {
    event?.stopPropagation();
    this.chatService.archiveConversation(conv.id).subscribe({
      next: () => {
        this.conversations = this.conversations.filter(c => c.id !== conv.id);
        if (this.selectedConversation?.id === conv.id) this.selectedConversation = null;
        this.cdr.detectChanges();
      },
      error: () => this.cdr.detectChanges()
    });
  }

  deleteConversation(conv: ConversationResponse, event?: Event): void {
    event?.stopPropagation();
    if (!confirm(`Delete conversation with ${conv.merchantName}?`)) return;
    this.chatService.deleteConversation(conv.id).subscribe({
      next: () => {
        this.conversations = this.conversations.filter(c => c.id !== conv.id);
        if (this.selectedConversation?.id === conv.id) this.selectedConversation = null;
        this.cdr.detectChanges();
      },
      error: () => this.cdr.detectChanges()
    });
  }


  goTo(path: string): void {
    this.router.navigate([path]);
  }

  logout(): void {
    this.authService.logout();
  }
}
