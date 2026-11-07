package com.techhub.controller.conversation;

import com.techhub.common.ApiResponse;
import com.techhub.common.CursorPageResponse;
import com.techhub.common.PageResponse;
import com.techhub.exception.BusinessException;
import com.techhub.exception.ErrorCode;
import com.techhub.model.dto.request.conversation.CreateConversationRequest;
import com.techhub.model.dto.response.conversation.ConversationResponse;
import com.techhub.model.dto.response.conversation.MessageResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.ConversationType;
import com.techhub.model.enums.Role;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.conversation.ConversationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests for ConversationController (POST /api/conversations)")
class ConversationControllerTest {

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private ConversationController controller;

    private UUID currentUserId;
    private UUID targetUserId;
    private Validator validator;

    @BeforeEach
    void setUp() {
        currentUserId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        User currentUser = new User();
        currentUser.setId(currentUserId);
        currentUser.setEmail("user@techhub.com");
        currentUser.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(currentUser);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Create or Get Conversation - Success (HTTP 200)")
    void createOrGetConversation_success() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest(targetUserId, ConversationType.BUYER_SELLER);
        ConversationResponse mockResponse = new ConversationResponse(
                UUID.randomUUID(),
                ConversationType.BUYER_SELLER,
                targetUserId,
                "Shop A",
                null,
                "SELLER",
                null,
                null,
                0L,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(conversationService.createOrGetConversation(eq(currentUserId), eq(request))).thenReturn(mockResponse);

        // When
        ApiResponse<ConversationResponse> apiResponse = controller.createOrGetConversation(request);

        // Then
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("Conversation retrieved or created successfully", apiResponse.getMessage());
        assertEquals(mockResponse, apiResponse.getData());
        assertNull(apiResponse.getMetadata());

        verify(conversationService, times(1)).createOrGetConversation(currentUserId, request);
    }

    @Test
    @DisplayName("TC-CONV-12: Reject Unauthenticated Request (Throws UNAUTHENTICATED)")
    void createOrGetConversation_unauthenticated_throwsException() {
        // Given
        SecurityContextHolder.clearContext();
        CreateConversationRequest request = new CreateConversationRequest(targetUserId, ConversationType.BUYER_SELLER);

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                controller.createOrGetConversation(request)
        );
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verifyNoInteractions(conversationService);
    }

    @Test
    @DisplayName("TC-CONV-08: Validation Failure on Null Target User ID")
    void tc_conv_08_nullTargetUserId_validationFails() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest(null, ConversationType.BUYER_SELLER);

        // When
        Set<ConstraintViolation<CreateConversationRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Target user ID must not be null")));
    }

    @Test
    @DisplayName("TC-CONV-09: Validation Failure on Null Conversation Type")
    void tc_conv_09_nullConversationType_validationFails() {
        // Given
        CreateConversationRequest request = new CreateConversationRequest(targetUserId, null);

        // When
        Set<ConstraintViolation<CreateConversationRequest>> violations = validator.validate(request);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Conversation type must not be null")));
    }

    @Test
    @DisplayName("Verify @PreAuthorize('isAuthenticated()') on createOrGetConversation")
    void verifyPreAuthorizeAnnotation() throws NoSuchMethodException {
        Method method = ConversationController.class.getMethod("createOrGetConversation", CreateConversationRequest.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertNotNull(annotation, "Method must have @PreAuthorize");
        assertEquals("isAuthenticated()", annotation.value());
    }

    @Test
    @DisplayName("Verify @Valid annotation on CreateConversationRequest parameter")
    void verifyValidAnnotation() throws NoSuchMethodException {
        Method method = ConversationController.class.getMethod("createOrGetConversation", CreateConversationRequest.class);
        Parameter[] parameters = method.getParameters();

        assertEquals(1, parameters.length);
        assertNotNull(parameters[0].getAnnotation(Valid.class), "Parameter must have @Valid annotation");
    }

    @Test
    @DisplayName("TC-INBOX-01: Get User Conversations - Success (HTTP 200)")
    void getUserConversations_success() {
        // Given
        PageResponse<ConversationResponse> mockPage = new PageResponse<>(
                java.util.List.of(), 0, 20, 0, 0, true
        );
        when(conversationService.getUserConversations(eq(currentUserId), eq(null), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        ApiResponse<PageResponse<ConversationResponse>> apiResponse = controller.getUserConversations(null, 0, 20);

        // Then
        assertNotNull(apiResponse);
        assertTrue(apiResponse.isSuccess());
        assertEquals("User conversations retrieved successfully", apiResponse.getMessage());
        assertEquals(mockPage, apiResponse.getData());
        assertNull(apiResponse.getMetadata());

        verify(conversationService, times(1)).getUserConversations(eq(currentUserId), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("TC-INBOX-13: Reject Unauthenticated Request on getUserConversations")
    void getUserConversations_unauthenticated_throwsException() {
        // Given
        SecurityContextHolder.clearContext();

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                controller.getUserConversations(null, 0, 20)
        );
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verifyNoInteractions(conversationService);
    }

    @Test
    @DisplayName("Verify @PreAuthorize('isAuthenticated()') on getUserConversations")
    void verifyPreAuthorizeAnnotationOnGetUserConversations() throws NoSuchMethodException {
        Method method = ConversationController.class.getMethod(
                "getUserConversations", ConversationType.class, int.class, int.class
        );
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertNotNull(annotation, "getUserConversations must have @PreAuthorize");
        assertEquals("isAuthenticated()", annotation.value());
    }

    @Test
    @DisplayName("Verify Pagination Constraints on getUserConversations parameters")
    void verifyPaginationConstraints() throws NoSuchMethodException {
        Method method = ConversationController.class.getMethod(
                "getUserConversations", ConversationType.class, int.class, int.class
        );
        Parameter[] parameters = method.getParameters();

        // Parameter 1: page
        Parameter pageParam = parameters[1];
        Min minPage = pageParam.getAnnotation(Min.class);
        assertNotNull(minPage, "page must have @Min annotation");
        assertEquals(0, minPage.value());
        assertEquals("Page index must not be negative", minPage.message());

        // Parameter 2: size
        Parameter sizeParam = parameters[2];
        Min minSize = sizeParam.getAnnotation(Min.class);
        assertNotNull(minSize, "size must have @Min annotation");
        assertEquals(1, minSize.value());

        Max maxSize = sizeParam.getAnnotation(Max.class);
        assertNotNull(maxSize, "size must have @Max annotation");
        assertEquals(100, maxSize.value());
        assertEquals("Page size must not exceed 100", maxSize.message());
    }

    @Test
    @DisplayName("TC-MSG-10: Controller HTTP 200 Delegation & ApiResponse Format for getMessageHistory")
    void getMessageHistory_success() {
        // Given
        UUID conversationId = UUID.randomUUID();
        MessageResponse msgResponse = new MessageResponse(
                UUID.randomUUID(),
                conversationId,
                currentUserId,
                "Current User",
                null,
                "Hello World",
                LocalDateTime.now(),
                true
        );
        CursorPageResponse<MessageResponse> mockPage = new CursorPageResponse<>(
                java.util.List.of(msgResponse),
                msgResponse.createdAt(),
                false,
                30
        );

        when(conversationService.getMessageHistory(eq(currentUserId), eq(conversationId), eq(null), eq(30)))
                .thenReturn(mockPage);

        // When
        ApiResponse<CursorPageResponse<MessageResponse>> response =
                controller.getMessageHistory(conversationId, null, 30);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Message history retrieved successfully", response.getMessage());
        assertEquals(1, response.getData().content().size());
        assertEquals("Hello World", response.getData().content().get(0).content());
        assertFalse(response.getData().hasMore());
        verify(conversationService, times(1))
                .getMessageHistory(eq(currentUserId), eq(conversationId), eq(null), eq(30));
    }

    @Test
    @DisplayName("TC-MSG-11: Verify @PreAuthorize('isAuthenticated()') on getMessageHistory")
    void verifyPreAuthorizeAnnotationOnGetMessageHistory() throws NoSuchMethodException {
        Method method = ConversationController.class.getMethod(
                "getMessageHistory", UUID.class, LocalDateTime.class, int.class
        );
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertNotNull(annotation, "getMessageHistory must have @PreAuthorize");
        assertEquals("isAuthenticated()", annotation.value());
    }

    @Test
    @DisplayName("TC-MSG-12: Verify Validation Annotations on getMessageHistory parameters (@Min(1) and @Max(100))")
    void verifyValidationAnnotationsOnGetMessageHistory() throws NoSuchMethodException {
        Method method = ConversationController.class.getMethod(
                "getMessageHistory", UUID.class, LocalDateTime.class, int.class
        );
        Parameter[] parameters = method.getParameters();

        // Parameter 2: limit
        Parameter limitParam = parameters[2];
        Min minLimit = limitParam.getAnnotation(Min.class);
        assertNotNull(minLimit, "limit must have @Min annotation");
        assertEquals(1, minLimit.value());
        assertEquals("Limit must be at least 1", minLimit.message());

        Max maxLimit = limitParam.getAnnotation(Max.class);
        assertNotNull(maxLimit, "limit must have @Max annotation");
        assertEquals(100, maxLimit.value());
        assertEquals("Limit must not exceed 100", maxLimit.message());
    }

    @Test
    @DisplayName("Reject Unauthenticated Request on getMessageHistory")
    void getMessageHistory_unauthenticated_throwsException() {
        // Given
        SecurityContextHolder.clearContext();
        UUID conversationId = UUID.randomUUID();

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                controller.getMessageHistory(conversationId, null, 30)
        );
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verifyNoInteractions(conversationService);
    }

    @Test
    @DisplayName("TC-DEL-05: Controller HTTP 200 Delegation & ApiResponse Format for deleteConversation")
    void deleteConversation_success() {
        // Given
        UUID conversationId = UUID.randomUUID();
        doNothing().when(conversationService).deleteConversation(eq(currentUserId), eq(conversationId));

        // When
        ApiResponse<Void> response = controller.deleteConversation(conversationId);

        // Then
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Conversation deleted successfully", response.getMessage());
        assertNull(response.getData());
        verify(conversationService, times(1)).deleteConversation(eq(currentUserId), eq(conversationId));
    }

    @Test
    @DisplayName("TC-DEL-06: Reject Unauthenticated Request on deleteConversation")
    void deleteConversation_unauthenticated_throwsException() {
        // Given
        SecurityContextHolder.clearContext();
        UUID conversationId = UUID.randomUUID();

        // When & Then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                controller.deleteConversation(conversationId)
        );
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
        verifyNoInteractions(conversationService);
    }

    @Test
    @DisplayName("TC-DEL-07: Verify @PreAuthorize('isAuthenticated()') on deleteConversation")
    void verifyPreAuthorizeAnnotationOnDeleteConversation() throws NoSuchMethodException {
        Method method = ConversationController.class.getMethod("deleteConversation", UUID.class);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);

        assertNotNull(annotation, "deleteConversation must have @PreAuthorize");
        assertEquals("isAuthenticated()", annotation.value());
    }
}
