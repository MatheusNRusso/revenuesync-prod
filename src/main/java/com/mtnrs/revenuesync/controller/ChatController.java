package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.dto.chat.ChatMessageResponse;
import com.mtnrs.revenuesync.dto.chat.ConversationResponse;
import com.mtnrs.revenuesync.dto.chat.SendMessageRequest;
import com.mtnrs.revenuesync.dto.chat.StartConversationRequest;
import com.mtnrs.revenuesync.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/start")
    public ResponseEntity<ConversationResponse> startConversation(
            @RequestBody StartConversationRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.startOrResumeConversation(request.merchantId(), user));
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> getMyConversations(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getConversationsForUser(user));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getMessages(conversationId, user));
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.sendMessage(conversationId, user, request.content()));
    }

    @PostMapping("/{conversationId}/payment-request")
    public ResponseEntity<ChatMessageResponse> sendPaymentRequest(
            @PathVariable Long conversationId,
            @RequestBody java.util.Map<String, Object> body,
            @AuthenticationPrincipal User user) {
        java.math.BigDecimal amount = new java.math.BigDecimal(body.get("amountSol").toString());
        return ResponseEntity.ok(chatService.sendPaymentRequest(conversationId, user, amount));
    }

    @PutMapping("/{conversationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {
        chatService.markAsRead(conversationId, user);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/archived")
    public ResponseEntity<List<ConversationResponse>> getArchivedConversations(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(chatService.getArchivedConversationsForUser(user));
    }

    @PostMapping("/{conversationId}/archive")
    public ResponseEntity<Void> archiveConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {
        chatService.archiveConversation(conversationId, user);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{conversationId}/unarchive")
    public ResponseEntity<Void> unarchiveConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {
        chatService.unarchiveConversation(conversationId, user);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conversationId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User user) {
        chatService.deleteConversation(conversationId, user);
        return ResponseEntity.noContent().build();
    }

}
