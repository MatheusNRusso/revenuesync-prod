package com.mtnrs.revenuesync;

import com.mtnrs.revenuesync.domain.Conversation;
import com.mtnrs.revenuesync.domain.Merchant;
import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.domain.enums.UserRole;
import com.mtnrs.revenuesync.repository.ChatMessageRepository;
import com.mtnrs.revenuesync.repository.ConversationRepository;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import com.mtnrs.revenuesync.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @InjectMocks
    private ChatService chatService;

    private User buyer;
    private User merchant;
    private Merchant merchantEntity;
    private Conversation conversation;

    @BeforeEach
    void setUp() throws Exception {
        buyer = User.of("Test Buyer", "buyer@example.com", "encoded-password", UserRole.USER);
        ReflectionTestUtils.setField(buyer, "id", 2L);

        merchant = User.of("Test Merchant User", "merchant@example.com", "encoded-password", UserRole.USER);
        ReflectionTestUtils.setField(merchant, "id", 1L);

        java.lang.reflect.Constructor<Merchant> merchantConstructor = Merchant.class.getDeclaredConstructor();
        merchantConstructor.setAccessible(true);
        merchantEntity = merchantConstructor.newInstance();
        ReflectionTestUtils.setField(merchantEntity, "id", 1L);
        ReflectionTestUtils.setField(merchantEntity, "name", "Test Merchant");
        ReflectionTestUtils.setField(merchantEntity, "email", "merchant-listing@example.com");
        ReflectionTestUtils.setField(merchantEntity, "user", merchant);

        conversation = Conversation.start(merchantEntity, buyer);
        ReflectionTestUtils.setField(conversation, "id", 1L);
    }

    @Test
    void testStartConversationThrowsExceptionForSelfChat() {
        when(merchantRepository.findById(1L)).thenReturn(Optional.of(merchantEntity));

        assertThrows(IllegalArgumentException.class, () -> {
            chatService.startOrResumeConversation(1L, merchant);
        });
    }

    @Test
    void testArchiveConversationForBuyer() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        chatService.archiveConversation(1L, buyer);

        verify(conversationRepository).findById(1L);
        assertTrue(conversation.isArchivedFor(buyer));
    }

    @Test
    void testDeleteConversationForBuyer() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        chatService.deleteConversation(1L, buyer);

        assertFalse(conversation.isVisibleFor(buyer));
    }

    @Test
    void testArchiveDoesNotAffectMerchantView() {
        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        chatService.archiveConversation(1L, buyer);

        assertTrue(conversation.isVisibleFor(merchant));
        assertFalse(conversation.isArchivedFor(merchant));
    }

    @Test
    void testResurrectConversationOnNewMessage() {
        conversation.archiveFor(buyer);
        conversation.deleteFor(merchant);

        assertTrue(conversation.isArchivedFor(buyer));
        assertFalse(conversation.isVisibleFor(merchant));

        conversation.resurrectOnNewMessage();

        assertFalse(conversation.isArchivedFor(buyer));
        assertTrue(conversation.isVisibleFor(merchant));
    }

    @Test
    void testSendMessageThrowsExceptionForUnauthorizedUser() {
        User unauthorizedUser = User.of("Unauthorized", "unauth@example.com", "pass", UserRole.USER);
        ReflectionTestUtils.setField(unauthorizedUser, "id", 999L);

        when(conversationRepository.findById(1L)).thenReturn(Optional.of(conversation));

        assertThrows(SecurityException.class, () -> {
            chatService.sendMessage(1L, unauthorizedUser, "Unauthorized message");
        });
    }
}