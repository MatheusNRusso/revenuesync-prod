package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.dto.chat.ChatMessageResponse;
import com.mtnrs.revenuesync.dto.chat.SendMessageRequest;
import com.mtnrs.revenuesync.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.send/{conversationId}")
    @SendTo("/topic/conversation/{conversationId}")
    public ChatMessageResponse sendMessage(
            @DestinationVariable Long conversationId,
            SendMessageRequest request,
            @AuthenticationPrincipal User user) {
        return chatService.sendMessage(conversationId, user, request.content());
    }
}
