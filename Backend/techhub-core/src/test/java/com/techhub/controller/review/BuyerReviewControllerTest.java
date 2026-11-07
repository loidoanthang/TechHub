package com.techhub.controller.review;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.request.review.CreateReviewRequest;
import com.techhub.model.dto.request.review.UpdateReviewRequest;
import com.techhub.model.dto.response.review.ReviewResponse;
import com.techhub.model.entity.User;
import com.techhub.model.enums.Role;
import com.techhub.model.enums.UserStatus;
import com.techhub.security.CustomUserDetails;
import com.techhub.service.review.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho BuyerReviewController (POST /api/buyer/reviews)")
class BuyerReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private BuyerReviewController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-14: controller_createReview_PreAuthorize_RequiresRoleBuyer - Bắt buộc quyền ROLE_BUYER")
    void controller_createReview_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = BuyerReviewController.class.getMethod("createReview", CreateReviewRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method createReview phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"),
                "@PreAuthorize phải yêu cầu hasRole('BUYER')");
    }

    @Test
    @DisplayName("TC-15: controller_createReview_Success_Returns200 - Trả về 200 OK với ReviewResponse")
    void controller_createReview_Success_Returns200() {
        UUID buyerId = UUID.randomUUID();
        User user = new User();
        user.setId(buyerId);
        user.setEmail("buyer@techhub.com");
        user.setPassword("hashed");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID orderItemId = UUID.randomUUID();
        CreateReviewRequest request = new CreateReviewRequest(orderItemId, 5, "Tuyệt vời", null);

        ReviewResponse responseDto = new ReviewResponse(
                UUID.randomUUID(),
                orderItemId,
                UUID.randomUUID(),
                "Laptop ASUS Zenbook 14",
                "Space Gray / 16GB",
                "https://cdn.techhub.com/img.png",
                5,
                "Tuyệt vời",
                Collections.emptyList(),
                "V*** N",
                "https://cdn.techhub.com/avatar.png",
                null,
                null,
                0,
                LocalDateTime.now()
        );

        when(reviewService.createReview(eq(buyerId), eq(request))).thenReturn(responseDto);

        ApiResponse<ReviewResponse> response = controller.createReview(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Review submitted successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(5, response.getData().rating());
        assertEquals("Tuyệt vời", response.getData().comment());
        assertEquals("Laptop ASUS Zenbook 14", response.getData().productName());

        verify(reviewService, times(1)).createReview(eq(buyerId), eq(request));
    }

    @Test
    @DisplayName("TC-16: controller_createReview_EndpointMapping_IsCorrect - Kiểm tra đúng @RequestMapping và @PostMapping")
    void controller_createReview_EndpointMapping_IsCorrect() throws NoSuchMethodException {
        RequestMapping classMapping = BuyerReviewController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/api/buyer/reviews"}, classMapping.value());

        Method method = BuyerReviewController.class.getMethod("createReview", CreateReviewRequest.class);
        PostMapping methodMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(methodMapping);
    }

    // =========================================================================
    // API 2: PUT /api/buyer/reviews/{id} Unit Tests (TC-30 -> TC-31)
    // =========================================================================

    @Test
    @DisplayName("TC-30: controller_updateReview_PreAuthorize_RequiresRoleBuyer - Bắt buộc quyền ROLE_BUYER cho endpoint PUT")
    void controller_updateReview_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = BuyerReviewController.class.getMethod("updateReview", UUID.class, UpdateReviewRequest.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method updateReview phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"),
                "@PreAuthorize phải yêu cầu hasRole('BUYER')");

        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        assertNotNull(putMapping, "Method updateReview phải có annotation @PutMapping");
        assertArrayEquals(new String[]{"/{id}"}, putMapping.value());
    }

    @Test
    @DisplayName("TC-31: controller_updateReview_Success_Returns200 - Trả về 200 OK với ReviewResponse sau khi cập nhật")
    void controller_updateReview_Success_Returns200() {
        UUID buyerId = UUID.randomUUID();
        User user = new User();
        user.setId(buyerId);
        user.setEmail("buyer@techhub.com");
        user.setPassword("hashed");
        user.setStatus(UserStatus.ACTIVE);
        user.setRoles(Set.of(Role.BUYER));

        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UUID reviewId = UUID.randomUUID();
        UpdateReviewRequest request = new UpdateReviewRequest(4, "Cập nhật đánh giá", null);

        ReviewResponse responseDto = new ReviewResponse(
                reviewId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Laptop ASUS Zenbook 14",
                "Space Gray / 16GB",
                "https://cdn.techhub.com/img.png",
                4,
                "Cập nhật đánh giá",
                Collections.emptyList(),
                "V*** N",
                "https://cdn.techhub.com/avatar.png",
                null,
                null,
                1,
                LocalDateTime.now()
        );

        when(reviewService.updateReview(eq(buyerId), eq(reviewId), eq(request))).thenReturn(responseDto);

        ApiResponse<ReviewResponse> response = controller.updateReview(reviewId, request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Review updated successfully", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(reviewId, response.getData().id());
        assertEquals(4, response.getData().rating());
        assertEquals("Cập nhật đánh giá", response.getData().comment());

        verify(reviewService, times(1)).updateReview(eq(buyerId), eq(reviewId), eq(request));
    }
}
