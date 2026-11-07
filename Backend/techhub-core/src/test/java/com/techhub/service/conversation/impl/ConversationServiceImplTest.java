package com.techhub.service.conversation.impl;

import com.techhub.common.CursorPageResponse;
import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.conversation.CreateConversationRequest;
import com.techhub.model.dto.response.conversation.ConversationResponse;
import com.techhub.model.dto.response.conversation.MessageResponse;
import com.techhub.model.entity.Conversation;
import com.techhub.model.entity.ConversationParticipant;
import com.techhub.model.entity.Message;
import com.techhub.model.entity.User;
import com.techhub.model.enums.ConversationType;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.repository.ConversationParticipantRepository;
import com.techhub.repository.ConversationRepository;
import com.techhub.repository.MessageRepository;
import com.techhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for ConversationServiceImpl (POST /api/conversations)")
class ConversationServiceImplTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private ConversationServiceImpl conversationService;

    private UUID buyerId;
    private UUID sellerId;
    private UUID adminId;

    private User buyerUser;
    private User sellerUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        buyerUser = new User();
        buyerUser.setId(buyerId);
        buyerUser.setFirstName("Nguyễn");
        buyerUser.setLastName("Văn A");
        buyerUser.setEmail("buyer@techhub.com");
        buyerUser.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        buyerUser.setStatus(UserStatus.ACTIVE);

        sellerUser = new User();
        sellerUser.setId(sellerId);
        sellerUser.setFirstName("TechZone");
        sellerUser.setLastName("Official Store");
        sellerUser.setEmail("seller@techzone.com");
        sellerUser.setAvatarUrl("https://cdn.techhub.com/avatar.png");
        sellerUser.setRoles(new HashSet<>(Set.of(Role.BUYER, Role.SELLER)));
        sellerUser.setStatus(UserStatus.ACTIVE);

        adminUser = new User();
        adminUser.setId(adminId);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("CSKH");
        adminUser.setEmail("admin@techhub.com");
        adminUser.setRoles(new HashSet<>(Set.of(Role.ADMIN)));
        adminUser.setStatus(UserStatus.ACTIVE);

        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    @DisplayName("TC-CONV-01: Create New BUYER_SELLER Conversation Successfully")
    void tc_conv_01_createNewBuyerSellerConversation_success() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest(sellerId, ConversationType.BUYER_SELLER);

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(sellerUser));
        when(conversationRepository.findConversationBetweenUsers(buyerId, sellerId)).thenReturn(Optional.empty());

        Conversation savedConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .type(ConversationType.BUYER_SELLER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);

        // When
        ConversationResponse response = conversationService.createOrGetConversation(buyerId, request);

        // Then
        assertNotNull(response);
        assertEquals(savedConversation.getId(), response.id());
        assertEquals(ConversationType.BUYER_SELLER, response.type());
        assertEquals(sellerId, response.opponentId());
        assertEquals("TechZone Official Store", response.opponentName());
        assertEquals("https://cdn.techhub.com/avatar.png", response.opponentAvatar());
        assertEquals("SELLER", response.opponentRole());
        assertNull(response.lastMessageSnippet());
        assertNull(response.lastMessageTime());
        assertEquals(0L, response.unreadCount());

        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(conversationParticipantRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("TC-CONV-02: Create New SELLER_ADMIN Conversation Successfully")
    void tc_conv_02_createNewSellerAdminConversation_success() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest(adminId, ConversationType.SELLER_ADMIN);

        when(userRepository.findById(sellerId)).thenReturn(Optional.of(sellerUser));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(conversationRepository.findConversationBetweenUsers(sellerId, adminId)).thenReturn(Optional.empty());

        Conversation savedConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .type(ConversationType.SELLER_ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);

        // When
        ConversationResponse response = conversationService.createOrGetConversation(sellerId, request);

        // Then
        assertNotNull(response);
        assertEquals(savedConversation.getId(), response.id());
        assertEquals(ConversationType.SELLER_ADMIN, response.type());
        assertEquals(adminId, response.opponentId());
        assertEquals("ADMIN", response.opponentRole());
        assertEquals("Admin CSKH", response.opponentName());

        verify(conversationRepository, times(1)).save(any(Conversation.class));
        verify(conversationParticipantRepository, times(1)).saveAll(any());
    }

    @Test
    @DisplayName("TC-CONV-03: Get Existing Conversation (Idempotency)")
    void tc_conv_03_getExistingConversation_success() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest(sellerId, ConversationType.BUYER_SELLER);
        UUID existingConvId = UUID.randomUUID();

        Conversation existingConversation = Conversation.builder()
                .id(existingConvId)
                .type(ConversationType.BUYER_SELLER)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusHours(2))
                .build();

        ConversationParticipant participant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(existingConversation)
                .user(buyerUser)
                .isDeleted(false)
                .build();

        Message latestMessage = Message.builder()
                .id(UUID.randomUUID())
                .conversation(existingConversation)
                .sender(sellerUser)
                .content("Shop còn màu đen bạn nhé!")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(sellerUser));
        when(conversationRepository.findConversationBetweenUsers(buyerId, sellerId)).thenReturn(Optional.of(existingConversation));
        when(conversationParticipantRepository.findByConversationIdAndUserId(existingConvId, buyerId)).thenReturn(Optional.of(participant));
        when(messageRepository.findLatestMessage(existingConvId)).thenReturn(Optional.of(latestMessage));

        // When
        ConversationResponse response = conversationService.createOrGetConversation(buyerId, request);

        // Then
        assertNotNull(response);
        assertEquals(existingConvId, response.id());
        assertEquals("Shop còn màu đen bạn nhé!", response.lastMessageSnippet());
        assertEquals(latestMessage.getCreatedAt(), response.lastMessageTime());

        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(conversationParticipantRepository, never()).saveAll(any());
        verify(conversationParticipantRepository, never()).save(any(ConversationParticipant.class));
    }

    @Test
    @DisplayName("TC-CONV-04: Reactivate Soft-Deleted Conversation")
    void tc_conv_04_reactivateSoftDeletedConversation_success() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest(sellerId, ConversationType.BUYER_SELLER);
        UUID existingConvId = UUID.randomUUID();

        Conversation existingConversation = Conversation.builder()
                .id(existingConvId)
                .type(ConversationType.BUYER_SELLER)
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        ConversationParticipant softDeletedParticipant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(existingConversation)
                .user(buyerUser)
                .isDeleted(true)
                .deletedAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(sellerUser));
        when(conversationRepository.findConversationBetweenUsers(buyerId, sellerId)).thenReturn(Optional.of(existingConversation));
        when(conversationParticipantRepository.findByConversationIdAndUserId(existingConvId, buyerId))
                .thenReturn(Optional.of(softDeletedParticipant));
        when(messageRepository.findLatestMessage(existingConvId)).thenReturn(Optional.empty());

        // When
        ConversationResponse response = conversationService.createOrGetConversation(buyerId, request);

        // Then
        assertNotNull(response);
        assertEquals(existingConvId, response.id());
        assertFalse(softDeletedParticipant.isDeleted());
        assertNull(softDeletedParticipant.getDeletedAt());

        verify(conversationParticipantRepository, times(1)).save(softDeletedParticipant);
    }

    @Test
    @DisplayName("TC-CONV-05: Reject Self-Chat Attempt (CANNOT_CHAT_WITH_SELF)")
    void tc_conv_05_rejectSelfChat() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest(buyerId, ConversationType.BUYER_SELLER);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.createOrGetConversation(buyerId, request)
        );
        assertEquals(ErrorCode.CANNOT_CHAT_WITH_SELF, ex.getErrorCode());
        verifyNoInteractions(userRepository, conversationRepository, conversationParticipantRepository);
    }

    @Test
    @DisplayName("TC-CONV-06: Reject Target User Not Found (USER_NOT_FOUND)")
    void tc_conv_06_rejectTargetUserNotFound() {
        // Given
        UUID nonExistentId = UUID.randomUUID();
        CreateConversationRequest request = new CreateConversationRequest(nonExistentId, ConversationType.BUYER_SELLER);

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.createOrGetConversation(buyerId, request)
        );
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
        verify(conversationRepository, never()).findConversationBetweenUsers(any(), any());
    }

    @Test
    @DisplayName("TC-CONV-07: Reject Target User Inactive / Banned (USER_NOT_ACTIVE)")
    void tc_conv_07_rejectTargetUserInactive() {
        // Given
        sellerUser.setStatus(UserStatus.BANNED);
        CreateConversationRequest request = new CreateConversationRequest(sellerId, ConversationType.BUYER_SELLER);

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(sellerUser));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.createOrGetConversation(buyerId, request)
        );
        assertEquals(ErrorCode.USER_NOT_ACTIVE, ex.getErrorCode());
        verify(conversationRepository, never()).findConversationBetweenUsers(any(), any());
    }

    @Test
    @DisplayName("TC-CONV-07b: Reject Current User Inactive / Banned (USER_NOT_ACTIVE)")
    void tc_conv_07b_rejectCurrentUserInactive() {
        // Given
        buyerUser.setStatus(UserStatus.SUSPENDED);
        CreateConversationRequest request = new CreateConversationRequest(sellerId, ConversationType.BUYER_SELLER);

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.createOrGetConversation(buyerId, request)
        );
        assertEquals(ErrorCode.USER_NOT_ACTIVE, ex.getErrorCode());
        verify(userRepository, never()).findById(sellerId);
    }

    @Test
    @DisplayName("TC-CONV-10: Reject Role Mismatch with Conversation Type (INVALID_CONVERSATION_TYPE)")
    void tc_conv_10_rejectRoleMismatch() {
        // Given: Buyer chats with another Buyer with BUYER_SELLER (neither is Seller)
        User anotherBuyer = new User();
        anotherBuyer.setId(UUID.randomUUID());
        anotherBuyer.setRoles(new HashSet<>(Set.of(Role.BUYER)));
        anotherBuyer.setStatus(UserStatus.ACTIVE);

        CreateConversationRequest request = new CreateConversationRequest(anotherBuyer.getId(), ConversationType.BUYER_SELLER);

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
        when(userRepository.findById(anotherBuyer.getId())).thenReturn(Optional.of(anotherBuyer));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.createOrGetConversation(buyerId, request)
        );
        assertEquals(ErrorCode.INVALID_CONVERSATION_TYPE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-CONV-10b: Reject Non-Admin in SELLER_ADMIN Conversation Type")
    void tc_conv_10b_rejectNonAdminInSellerAdmin() {
        // Given: Buyer and Seller trying to create SELLER_ADMIN (neither is Admin)
        CreateConversationRequest request = new CreateConversationRequest(sellerId, ConversationType.SELLER_ADMIN);

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(sellerUser));

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.createOrGetConversation(buyerId, request)
        );
        assertEquals(ErrorCode.INVALID_CONVERSATION_TYPE, ex.getErrorCode());
    }

    @Test
    @DisplayName("TC-CONV-11: Concurrent Creation Synchronization (Canonical Lock Key)")
    void tc_conv_11_concurrentCreationSynchronization() throws InterruptedException {
        // Given
        CreateConversationRequest request = new CreateConversationRequest(sellerId, ConversationType.BUYER_SELLER);

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(sellerUser));

        UUID convId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(convId)
                .type(ConversationType.BUYER_SELLER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AtomicInteger callCount = new AtomicInteger(0);
        when(conversationRepository.findConversationBetweenUsers(buyerId, sellerId)).thenAnswer(inv -> {
            if (callCount.getAndIncrement() == 0) {
                return Optional.empty();
            }
            return Optional.of(conv);
        });
        when(conversationRepository.save(any(Conversation.class))).thenReturn(conv);

        // When: Run 2 concurrent threads
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    conversationService.createOrGetConversation(buyerId, request);
                } catch (Exception ignored) {
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // Then: Only 1 insert occurred
        verify(conversationRepository, times(1)).save(any(Conversation.class));
    }

    @Test
    @DisplayName("TC-CONV-14: Truncate long message snippet to 100 characters with ellipsis")
    void tc_conv_14_truncateLongSnippet() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest(sellerId, ConversationType.BUYER_SELLER);
        UUID existingConvId = UUID.randomUUID();

        Conversation existingConversation = Conversation.builder()
                .id(existingConvId)
                .type(ConversationType.BUYER_SELLER)
                .build();

        String longContent = "A".repeat(150);
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .conversation(existingConversation)
                .sender(sellerUser)
                .content(longContent)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(sellerUser));
        when(conversationRepository.findConversationBetweenUsers(buyerId, sellerId)).thenReturn(Optional.of(existingConversation));
        when(conversationParticipantRepository.findByConversationIdAndUserId(existingConvId, buyerId)).thenReturn(Optional.empty());
        when(messageRepository.findLatestMessage(existingConvId)).thenReturn(Optional.of(message));

        // When
        ConversationResponse response = conversationService.createOrGetConversation(buyerId, request);

        // Then
        assertNotNull(response.lastMessageSnippet());
        assertEquals(100, response.lastMessageSnippet().length());
        assertTrue(response.lastMessageSnippet().endsWith("..."));
    }

    @Test
    @DisplayName("Fallback opponent name to email when first and last name are empty")
    void fallbackOpponentNameToEmail() {
        // Given
        sellerUser.setFirstName(null);
        sellerUser.setLastName("");
        CreateConversationRequest request = new CreateConversationRequest(sellerId, ConversationType.BUYER_SELLER);

        when(userRepository.findById(buyerId)).thenReturn(Optional.of(buyerUser));
        when(userRepository.findById(sellerId)).thenReturn(Optional.of(sellerUser));
        when(conversationRepository.findConversationBetweenUsers(buyerId, sellerId)).thenReturn(Optional.empty());

        Conversation savedConversation = Conversation.builder()
                .id(UUID.randomUUID())
                .type(ConversationType.BUYER_SELLER)
                .build();
        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);

        // When
        ConversationResponse response = conversationService.createOrGetConversation(buyerId, request);

        // Then
        assertEquals("seller@techzone.com", response.opponentName());
    }

    @Test
    @DisplayName("TC-INBOX-01: Get Active Conversations Successfully (Happy Path)")
    void tc_inbox_01_getActiveConversations_success() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        UUID convId1 = UUID.randomUUID();
        UUID convId2 = UUID.randomUUID();

        Conversation conv1 = Conversation.builder()
                .id(convId1)
                .type(ConversationType.BUYER_SELLER)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();

        Conversation conv2 = Conversation.builder()
                .id(convId2)
                .type(ConversationType.BUYER_SELLER)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusHours(3))
                .build();

        Page<Conversation> page = new PageImpl<>(List.of(conv1, conv2), pageable, 2);
        when(conversationRepository.findUserConversations(buyerId, null, pageable)).thenReturn(page);

        // Mock Opponents
        ConversationParticipant part1 = ConversationParticipant.builder()
                .conversation(conv1)
                .user(sellerUser)
                .build();
        when(conversationParticipantRepository.findOpponentsInConversations(any(), eq(buyerId)))
                .thenReturn(List.of(part1));

        // Mock Latest Messages
        Message msg1 = Message.builder()
                .conversation(conv1)
                .content("Chào bạn, shop còn màu đen!")
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
        when(messageRepository.findLatestMessagesByConversationIds(any()))
                .thenReturn(List.of(msg1));

        // Mock Unread Counts
        when(messageRepository.countUnreadMessagesByConversationIds(any(), eq(buyerId)))
                .thenReturn(Collections.singletonList(new Object[]{convId1, 2L}));

        // When
        PageResponse<ConversationResponse> response = conversationService.getUserConversations(buyerId, null, pageable);

        // Then
        assertNotNull(response);
        assertEquals(2, response.totalElements());
        assertEquals(2, response.content().size());

        ConversationResponse item1 = response.content().get(0);
        assertEquals(convId1, item1.id());
        assertEquals(sellerId, item1.opponentId());
        assertEquals("TechZone Official Store", item1.opponentName());
        assertEquals("Chào bạn, shop còn màu đen!", item1.lastMessageSnippet());
        assertEquals(2L, item1.unreadCount());

        ConversationResponse item2 = response.content().get(1);
        assertEquals(convId2, item2.id());
        assertEquals(0L, item2.unreadCount());
    }

    @Test
    @DisplayName("TC-INBOX-02: Filter Conversations by Type (BUYER_SELLER)")
    void tc_inbox_02_filterConversationsByType_buyerSeller() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        UUID convId1 = UUID.randomUUID();
        Conversation conv1 = Conversation.builder()
                .id(convId1)
                .type(ConversationType.BUYER_SELLER)
                .build();

        Page<Conversation> page = new PageImpl<>(List.of(conv1), pageable, 1);
        when(conversationRepository.findUserConversations(buyerId, ConversationType.BUYER_SELLER, pageable))
                .thenReturn(page);
        when(conversationParticipantRepository.findOpponentsInConversations(any(), eq(buyerId)))
                .thenReturn(List.of(ConversationParticipant.builder().conversation(conv1).user(sellerUser).build()));
        when(messageRepository.findLatestMessagesByConversationIds(any())).thenReturn(List.of());
        when(messageRepository.countUnreadMessagesByConversationIds(any(), eq(buyerId))).thenReturn(List.of());

        // When
        PageResponse<ConversationResponse> response =
                conversationService.getUserConversations(buyerId, ConversationType.BUYER_SELLER, pageable);

        // Then
        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals(ConversationType.BUYER_SELLER, response.content().get(0).type());
    }

    @Test
    @DisplayName("TC-INBOX-03: Filter Conversations by Type (SELLER_ADMIN)")
    void tc_inbox_03_filterConversationsByType_sellerAdmin() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        UUID convId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(convId)
                .type(ConversationType.SELLER_ADMIN)
                .build();

        Page<Conversation> page = new PageImpl<>(List.of(conv), pageable, 1);
        when(conversationRepository.findUserConversations(sellerId, ConversationType.SELLER_ADMIN, pageable))
                .thenReturn(page);
        when(conversationParticipantRepository.findOpponentsInConversations(any(), eq(sellerId)))
                .thenReturn(List.of(ConversationParticipant.builder().conversation(conv).user(adminUser).build()));
        when(messageRepository.findLatestMessagesByConversationIds(any())).thenReturn(List.of());
        when(messageRepository.countUnreadMessagesByConversationIds(any(), eq(sellerId))).thenReturn(List.of());

        // When
        PageResponse<ConversationResponse> response =
                conversationService.getUserConversations(sellerId, ConversationType.SELLER_ADMIN, pageable);

        // Then
        assertNotNull(response);
        assertEquals(1, response.totalElements());
        assertEquals(ConversationType.SELLER_ADMIN, response.content().get(0).type());
        assertEquals("ADMIN", response.content().get(0).opponentRole());
    }

    @Test
    @DisplayName("TC-INBOX-05: Empty Inbox for New User")
    void tc_inbox_05_emptyInboxForNewUser() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<Conversation> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        when(conversationRepository.findUserConversations(buyerId, null, pageable)).thenReturn(emptyPage);

        // When
        PageResponse<ConversationResponse> response = conversationService.getUserConversations(buyerId, null, pageable);

        // Then
        assertNotNull(response);
        assertEquals(0, response.totalElements());
        assertTrue(response.content().isEmpty());
        verifyNoInteractions(conversationParticipantRepository, messageRepository);
    }

    @Test
    @DisplayName("TC-INBOX-07: Truncate Long Message Snippet in List")
    void tc_inbox_07_truncateLongSnippetInList() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        UUID convId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(convId)
                .type(ConversationType.BUYER_SELLER)
                .build();

        Page<Conversation> page = new PageImpl<>(List.of(conv), pageable, 1);
        when(conversationRepository.findUserConversations(buyerId, null, pageable)).thenReturn(page);
        when(conversationParticipantRepository.findOpponentsInConversations(any(), eq(buyerId)))
                .thenReturn(List.of(ConversationParticipant.builder().conversation(conv).user(sellerUser).build()));

        Message longMsg = Message.builder()
                .conversation(conv)
                .content("B".repeat(150))
                .createdAt(LocalDateTime.now())
                .build();
        when(messageRepository.findLatestMessagesByConversationIds(any())).thenReturn(List.of(longMsg));
        when(messageRepository.countUnreadMessagesByConversationIds(any(), eq(buyerId))).thenReturn(List.of());

        // When
        PageResponse<ConversationResponse> response = conversationService.getUserConversations(buyerId, null, pageable);

        // Then
        assertEquals(100, response.content().get(0).lastMessageSnippet().length());
        assertTrue(response.content().get(0).lastMessageSnippet().endsWith("..."));
    }

    @Test
    @DisplayName("TC-INBOX-08: Fallback Opponent Name to Email in List")
    void tc_inbox_08_fallbackOpponentNameToEmailInList() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        UUID convId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(convId)
                .type(ConversationType.BUYER_SELLER)
                .build();

        User noNameUser = new User();
        noNameUser.setId(UUID.randomUUID());
        noNameUser.setEmail("nameless@shop.com");
        noNameUser.setRoles(Set.of(Role.SELLER));

        Page<Conversation> page = new PageImpl<>(List.of(conv), pageable, 1);
        when(conversationRepository.findUserConversations(buyerId, null, pageable)).thenReturn(page);
        when(conversationParticipantRepository.findOpponentsInConversations(any(), eq(buyerId)))
                .thenReturn(List.of(ConversationParticipant.builder().conversation(conv).user(noNameUser).build()));
        when(messageRepository.findLatestMessagesByConversationIds(any())).thenReturn(List.of());
        when(messageRepository.countUnreadMessagesByConversationIds(any(), eq(buyerId))).thenReturn(List.of());

        // When
        PageResponse<ConversationResponse> response = conversationService.getUserConversations(buyerId, null, pageable);

        // Then
        assertEquals("nameless@shop.com", response.content().get(0).opponentName());
    }

    @Test
    @DisplayName("TC-INBOX-09: Correct Unread Count Calculation in List")
    void tc_inbox_09_correctUnreadCountCalculation() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        UUID convId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(convId)
                .type(ConversationType.BUYER_SELLER)
                .build();

        Page<Conversation> page = new PageImpl<>(List.of(conv), pageable, 1);
        when(conversationRepository.findUserConversations(buyerId, null, pageable)).thenReturn(page);
        when(conversationParticipantRepository.findOpponentsInConversations(any(), eq(buyerId)))
                .thenReturn(List.of(ConversationParticipant.builder().conversation(conv).user(sellerUser).build()));
        when(messageRepository.findLatestMessagesByConversationIds(any())).thenReturn(List.of());
        when(messageRepository.countUnreadMessagesByConversationIds(any(), eq(buyerId)))
                .thenReturn(Collections.singletonList(new Object[]{convId, 7L}));

        // When
        PageResponse<ConversationResponse> response = conversationService.getUserConversations(buyerId, null, pageable);

        // Then
        assertEquals(7L, response.content().get(0).unreadCount());
    }

    @Test
    @DisplayName("TC-INBOX-14: Verify Read-Only Transaction Annotation on getUserConversations")
    void tc_inbox_14_verifyReadOnlyTransactionAnnotation() throws NoSuchMethodException {
        java.lang.reflect.Method method = ConversationServiceImpl.class.getMethod(
                "getUserConversations", UUID.class, ConversationType.class, Pageable.class
        );
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertNotNull(annotation, "getUserConversations must be annotated with @Transactional");
        assertTrue(annotation.readOnly(), "readOnly must be true");
    }

    @Test
    @DisplayName("Fallback to default TechHub User when opponent is missing")
    void fallbackToDefaultUserWhenOpponentMissing() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        UUID convId = UUID.randomUUID();
        Conversation conv = Conversation.builder()
                .id(convId)
                .type(ConversationType.BUYER_SELLER)
                .build();

        Page<Conversation> page = new PageImpl<>(List.of(conv), pageable, 1);
        when(conversationRepository.findUserConversations(buyerId, null, pageable)).thenReturn(page);
        when(conversationParticipantRepository.findOpponentsInConversations(any(), eq(buyerId)))
                .thenReturn(List.of()); // Opponent missing
        when(messageRepository.findLatestMessagesByConversationIds(any())).thenReturn(List.of());
        when(messageRepository.countUnreadMessagesByConversationIds(any(), eq(buyerId))).thenReturn(List.of());

        // When
        PageResponse<ConversationResponse> response = conversationService.getUserConversations(buyerId, null, pageable);

        // Then
        assertEquals("Người dùng TechHub", response.content().get(0).opponentName());
    }

    // =========================================================================
    // TESTS FOR GET /api/conversations/{id}/messages
    // =========================================================================

    @Test
    @DisplayName("TC-MSG-01: Get Initial Message History Successfully (Happy Path)")
    void tc_msg_01_getMessageHistory_happyPath() {
        // Given
        UUID convId = UUID.randomUUID();
        ConversationParticipant participant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .user(buyerUser)
                .build();

        when(conversationParticipantRepository.findByConversationIdAndUserId(convId, buyerId))
                .thenReturn(Optional.of(participant));

        Message msg1 = Message.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .sender(buyerUser)
                .content("Cảm ơn shop nhiều!")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build();

        Message msg2 = Message.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .sender(sellerUser)
                .content("Dạ shop đã gửi hàng ạ!")
                .createdAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(messageRepository.findMessagesBefore(eq(convId), eq(null), eq(PageRequest.of(0, 31))))
                .thenReturn(List.of(msg1, msg2));

        // When
        CursorPageResponse<MessageResponse> response = conversationService.getMessageHistory(buyerId, convId, null, 30);

        // Then
        assertNotNull(response);
        assertEquals(2, response.content().size());
        assertFalse(response.hasMore());
        assertEquals(msg2.getCreatedAt(), response.nextCursor());
        assertEquals(30, response.limit());

        MessageResponse item1 = response.content().get(0);
        assertEquals(msg1.getId(), item1.id());
        assertTrue(item1.isMine(), "Message 1 sent by buyer should be isMine = true");
        assertEquals("Nguyễn Văn A", item1.senderName());

        MessageResponse item2 = response.content().get(1);
        assertEquals(msg2.getId(), item2.id());
        assertFalse(item2.isMine(), "Message 2 sent by seller should be isMine = false");
        assertEquals("TechZone Official Store", item2.senderName());

        // Verify lastReadAt is updated on initial load (beforeTime == null)
        assertNotNull(participant.getLastReadAt());
        verify(conversationParticipantRepository, times(1)).save(participant);
    }

    @Test
    @DisplayName("TC-MSG-02: Cursor Pagination with hasMore = true")
    void tc_msg_02_getMessageHistory_hasMoreTrue() {
        // Given
        UUID convId = UUID.randomUUID();
        ConversationParticipant participant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .user(buyerUser)
                .build();

        when(conversationParticipantRepository.findByConversationIdAndUserId(convId, buyerId))
                .thenReturn(Optional.of(participant));

        // Create 31 mock messages (limit 30 + 1)
        java.util.List<Message> mockMessages = new java.util.ArrayList<>();
        for (int i = 0; i < 31; i++) {
            mockMessages.add(Message.builder()
                    .id(UUID.randomUUID())
                    .conversation(Conversation.builder().id(convId).build())
                    .sender(buyerUser)
                    .content("Message #" + i)
                    .createdAt(LocalDateTime.now().minusMinutes(i))
                    .build());
        }

        when(messageRepository.findMessagesBefore(eq(convId), eq(null), eq(PageRequest.of(0, 31))))
                .thenReturn(mockMessages);

        // When
        CursorPageResponse<MessageResponse> response = conversationService.getMessageHistory(buyerId, convId, null, 30);

        // Then
        assertNotNull(response);
        assertEquals(30, response.content().size(), "Page content should be trimmed to exactly limit (30)");
        assertTrue(response.hasMore(), "hasMore should be true when 31 messages returned");
        assertEquals(mockMessages.get(29).getCreatedAt(), response.nextCursor());
    }

    @Test
    @DisplayName("TC-MSG-03: Cursor Pagination Scroll Up with beforeTime (Skip update lastReadAt)")
    void tc_msg_03_getMessageHistory_withBeforeTime() {
        // Given
        UUID convId = UUID.randomUUID();
        LocalDateTime beforeTime = LocalDateTime.now().minusHours(2);
        ConversationParticipant participant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .user(buyerUser)
                .build();

        when(conversationParticipantRepository.findByConversationIdAndUserId(convId, buyerId))
                .thenReturn(Optional.of(participant));

        Message msg1 = Message.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .sender(sellerUser)
                .content("Tin nhắn cũ hơn")
                .createdAt(beforeTime.minusMinutes(10))
                .build();

        when(messageRepository.findMessagesBefore(eq(convId), eq(beforeTime), eq(PageRequest.of(0, 31))))
                .thenReturn(List.of(msg1));

        // When
        CursorPageResponse<MessageResponse> response = conversationService.getMessageHistory(buyerId, convId, beforeTime, 30);

        // Then
        assertNotNull(response);
        assertEquals(1, response.content().size());
        assertFalse(response.hasMore());
        assertEquals(msg1.getCreatedAt(), response.nextCursor());

        // Verify lastReadAt is NOT updated when scrolling up (beforeTime != null)
        verify(conversationParticipantRepository, never()).save(participant);
    }

    @Test
    @DisplayName("TC-MSG-04: Reject Unauthorized User (Anti-IDOR Protection)")
    void tc_msg_04_getMessageHistory_unauthorizedParticipant_throwsForbidden() {
        // Given
        UUID convId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        when(conversationParticipantRepository.findByConversationIdAndUserId(convId, strangerId))
                .thenReturn(Optional.empty());
        when(conversationRepository.existsById(convId)).thenReturn(true);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.getMessageHistory(strangerId, convId, null, 30)
        );
        assertEquals(ErrorCode.NOT_CONVERSATION_PARTICIPANT, ex.getErrorCode());
        verifyNoInteractions(messageRepository);
    }

    @Test
    @DisplayName("TC-MSG-05: Reject Non-Existent Conversation ID")
    void tc_msg_05_getMessageHistory_conversationNotFound_throwsNotFound() {
        // Given
        UUID nonExistentConvId = UUID.randomUUID();

        when(conversationParticipantRepository.findByConversationIdAndUserId(nonExistentConvId, buyerId))
                .thenReturn(Optional.empty());
        when(conversationRepository.existsById(nonExistentConvId)).thenReturn(false);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.getMessageHistory(buyerId, nonExistentConvId, null, 30)
        );
        assertEquals(ErrorCode.CONVERSATION_NOT_FOUND, ex.getErrorCode());
        verifyNoInteractions(messageRepository);
    }

    @Test
    @DisplayName("TC-MSG-06: Empty Message History for Fresh Conversation")
    void tc_msg_06_getMessageHistory_emptyConversation() {
        // Given
        UUID convId = UUID.randomUUID();
        ConversationParticipant participant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .user(buyerUser)
                .build();

        when(conversationParticipantRepository.findByConversationIdAndUserId(convId, buyerId))
                .thenReturn(Optional.of(participant));
        when(messageRepository.findMessagesBefore(eq(convId), eq(null), eq(PageRequest.of(0, 31))))
                .thenReturn(Collections.emptyList());

        // When
        CursorPageResponse<MessageResponse> response = conversationService.getMessageHistory(buyerId, convId, null, 30);

        // Then
        assertNotNull(response);
        assertTrue(response.content().isEmpty());
        assertNull(response.nextCursor());
        assertFalse(response.hasMore());
    }

    @Test
    @DisplayName("TC-MSG-09: Fallback Sender Name to Email when First/Last Name Empty")
    void tc_msg_09_getMessageHistory_fallbackSenderNameToEmail() {
        // Given
        UUID convId = UUID.randomUUID();
        ConversationParticipant participant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .user(buyerUser)
                .build();

        when(conversationParticipantRepository.findByConversationIdAndUserId(convId, buyerId))
                .thenReturn(Optional.of(participant));

        User namelessUser = new User();
        namelessUser.setId(UUID.randomUUID());
        namelessUser.setEmail("anonymous@techhub.com");
        namelessUser.setFirstName(null);
        namelessUser.setLastName("");

        Message msg = Message.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .sender(namelessUser)
                .content("Tin nhắn từ người dùng chưa đặt tên")
                .createdAt(LocalDateTime.now())
                .build();

        when(messageRepository.findMessagesBefore(eq(convId), eq(null), eq(PageRequest.of(0, 31))))
                .thenReturn(List.of(msg));

        // When
        CursorPageResponse<MessageResponse> response = conversationService.getMessageHistory(buyerId, convId, null, 30);

        // Then
        assertNotNull(response);
        assertEquals("anonymous@techhub.com", response.content().get(0).senderName());
    }

    @Test
    @DisplayName("Verify Transactional Annotation on getMessageHistory")
    void verifyTransactionalAnnotationOnGetMessageHistory() throws NoSuchMethodException {
        java.lang.reflect.Method method = ConversationServiceImpl.class.getMethod(
                "getMessageHistory", UUID.class, UUID.class, LocalDateTime.class, int.class
        );
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertNotNull(annotation, "getMessageHistory must be annotated with @Transactional");
    }

    // =========================================================================
    // TESTS FOR DELETE /api/conversations/{id}
    // =========================================================================

    @Test
    @DisplayName("TC-DEL-01: Soft Delete Conversation Successfully (Happy Path)")
    void tc_del_01_deleteConversation_happyPath() {
        // Given
        UUID convId = UUID.randomUUID();
        ConversationParticipant participant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .user(buyerUser)
                .isDeleted(false)
                .deletedAt(null)
                .build();

        when(conversationParticipantRepository.findByConversationIdAndUserId(convId, buyerId))
                .thenReturn(Optional.of(participant));

        // When
        conversationService.deleteConversation(buyerId, convId);

        // Then
        assertTrue(participant.isDeleted(), "isDeleted must be set to true");
        assertNotNull(participant.getDeletedAt(), "deletedAt must be recorded");
        verify(conversationParticipantRepository, times(1)).save(participant);
    }

    @Test
    @DisplayName("TC-DEL-02: Idempotent Delete when Already Soft-Deleted")
    void tc_del_02_deleteConversation_alreadyDeleted_idempotent() {
        // Given
        UUID convId = UUID.randomUUID();
        LocalDateTime originalDeletedAt = LocalDateTime.now().minusDays(1);
        ConversationParticipant participant = ConversationParticipant.builder()
                .id(UUID.randomUUID())
                .conversation(Conversation.builder().id(convId).build())
                .user(buyerUser)
                .isDeleted(true)
                .deletedAt(originalDeletedAt)
                .build();

        when(conversationParticipantRepository.findByConversationIdAndUserId(convId, buyerId))
                .thenReturn(Optional.of(participant));

        // When
        conversationService.deleteConversation(buyerId, convId);

        // Then - Should return cleanly without redundant save
        assertTrue(participant.isDeleted());
        assertEquals(originalDeletedAt, participant.getDeletedAt());
        verify(conversationParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-DEL-03: Reject Unauthorized User on deleteConversation (Anti-IDOR Protection)")
    void tc_del_03_deleteConversation_unauthorizedParticipant_throwsForbidden() {
        // Given
        UUID convId = UUID.randomUUID();
        UUID strangerId = UUID.randomUUID();

        when(conversationParticipantRepository.findByConversationIdAndUserId(convId, strangerId))
                .thenReturn(Optional.empty());
        when(conversationRepository.existsById(convId)).thenReturn(true);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.deleteConversation(strangerId, convId)
        );
        assertEquals(ErrorCode.NOT_CONVERSATION_PARTICIPANT, ex.getErrorCode());
        verify(conversationParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-DEL-04: Reject Non-Existent Conversation ID on deleteConversation")
    void tc_del_04_deleteConversation_conversationNotFound_throwsNotFound() {
        // Given
        UUID nonExistentConvId = UUID.randomUUID();

        when(conversationParticipantRepository.findByConversationIdAndUserId(nonExistentConvId, buyerId))
                .thenReturn(Optional.empty());
        when(conversationRepository.existsById(nonExistentConvId)).thenReturn(false);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                conversationService.deleteConversation(buyerId, nonExistentConvId)
        );
        assertEquals(ErrorCode.CONVERSATION_NOT_FOUND, ex.getErrorCode());
        verify(conversationParticipantRepository, never()).save(any());
    }

    @Test
    @DisplayName("TC-DEL-08: Verify @Transactional Annotation on Service deleteConversation method")
    void tc_del_08_verifyTransactionalAnnotationOnDeleteConversation() throws NoSuchMethodException {
        java.lang.reflect.Method method = ConversationServiceImpl.class.getMethod(
                "deleteConversation", UUID.class, UUID.class
        );
        Transactional annotation = method.getAnnotation(Transactional.class);

        assertNotNull(annotation, "deleteConversation must be annotated with @Transactional");
    }
}
