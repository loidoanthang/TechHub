package com.techhub.controller.review;

import com.techhub.common.ApiResponse;
import com.techhub.common.PageResponse;
import com.techhub.model.dto.request.review.SellerResponseRequest;
import com.techhub.model.dto.response.review.SellerReviewResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.review.SellerReviewService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho SellerReviewController (GET /api/seller/reviews)")
class SellerReviewControllerTest {

    @Mock
    private SellerReviewService sellerReviewService;

    @InjectMocks
    private SellerReviewController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-60: controller_getShopReviews_PreAuthorize_RequiresRoleSeller - Bắt buộc quyền ROLE_SELLER")
    void controller_getShopReviews_PreAuthorize_RequiresRoleSeller() throws NoSuchMethodException {
        Method method = SellerReviewController.class.getMethod("getShopReviews",
                Integer.class, Boolean.class, Boolean.class, UUID.class, String.class,
                LocalDate.class, LocalDate.class, int.class, int.class, String.class, String.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method getShopReviews phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('SELLER')"),
                "@PreAuthorize phải yêu cầu hasRole('SELLER')");

        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        assertNotNull(getMapping, "Method getShopReviews phải có annotation @GetMapping");

        RequestMapping classMapping = SellerReviewController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/api/seller/reviews"}, classMapping.value());
    }

    @Test
    @DisplayName("TC-61: controller_getShopReviews_Success_Returns200 - Trả về 200 OK với PageResponse<SellerReviewResponse>")
    void controller_getShopReviews_Success_Returns200() {
        UUID sellerUserId = UUID.randomUUID();
        User user = new User();
        user.setId(sellerUserId);
        user.setEmail("seller@techhub.com");
        user.setPassword("hashed");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.SELLER));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        PageResponse<SellerReviewResponse> mockPageResponse = new PageResponse<>(
                Collections.emptyList(), 0, 10, 0, 0, true
        );

        when(sellerReviewService.getShopReviews(eq(sellerUserId), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(mockPageResponse);

        ApiResponse<PageResponse<SellerReviewResponse>> response = controller.getShopReviews(
                5, false, true, null, null, null, null, 0, 10, "createdAt", "DESC"
        );

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Shop reviews retrieved successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(0, response.getData().totalElements());

        verify(sellerReviewService, times(1)).getShopReviews(
                eq(sellerUserId), eq(5), eq(false), eq(true), any(), any(), any(), any(), any(Pageable.class)
        );
    }

    // =========================================================================
    // API 5: POST /api/seller/reviews/{id}/response (TC-74 đến TC-75)
    // =========================================================================

    @Test
    @DisplayName("TC-74: controller_respondToReview_PreAuthorize_RequiresRoleSeller - Bắt buộc quyền ROLE_SELLER")
    void controller_respondToReview_PreAuthorize_RequiresRoleSeller() throws NoSuchMethodException {
        Method method = SellerReviewController.class.getMethod("respondToReview", UUID.class, SellerResponseRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method respondToReview phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('SELLER')"),
                "@PreAuthorize phải yêu cầu hasRole('SELLER')");

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(postMapping, "Method respondToReview phải có annotation @PostMapping");
        assertArrayEquals(new String[]{"/{id}/response"}, postMapping.value());
    }

    @Test
    @DisplayName("TC-75: controller_respondToReview_Success_Returns200 - Trả về 200 OK với SellerReviewResponse")
    void controller_respondToReview_Success_Returns200() {
        UUID sellerUserId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        User user = new User();
        user.setId(sellerUserId);
        user.setEmail("seller@techhub.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.SELLER));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        SellerResponseRequest request = new SellerResponseRequest("Cảm ơn quý khách đã mua sắm!");
        SellerReviewResponse mockResponse = new SellerReviewResponse(
                reviewId, UUID.randomUUID(), UUID.randomUUID(), "ORD-123",
                UUID.randomUUID(), "Product A", "Variant A", "SKU-A", "img.jpg",
                5, "Tốt", Collections.emptyList(), "N*** A***", null,
                "Cảm ơn quý khách đã mua sắm!", java.time.LocalDateTime.now(), 2L, java.time.LocalDateTime.now().minusDays(1)
        );

        when(sellerReviewService.respondToReview(eq(sellerUserId), eq(reviewId), eq(request)))
                .thenReturn(mockResponse);

        ApiResponse<SellerReviewResponse> response = controller.respondToReview(reviewId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Review responded successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("Cảm ơn quý khách đã mua sắm!", response.getData().sellerResponse());
        assertEquals(2L, response.getData().helpfulCount());

        verify(sellerReviewService, times(1)).respondToReview(eq(sellerUserId), eq(reviewId), eq(request));
    }
}
