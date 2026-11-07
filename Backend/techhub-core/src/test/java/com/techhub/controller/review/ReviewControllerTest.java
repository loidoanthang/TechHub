package com.techhub.controller.review;

import com.techhub.common.ApiResponse;
import com.techhub.model.dto.response.review.HelpfulVoteResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho ReviewController (POST /api/reviews/{id}/helpful)")
class ReviewControllerTest {

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-41: controller_toggleHelpful_PreAuthorize_RequiresRoleBuyer - Bắt buộc quyền ROLE_BUYER cho endpoint toggle")
    void controller_toggleHelpful_PreAuthorize_RequiresRoleBuyer() throws NoSuchMethodException {
        Method method = ReviewController.class.getMethod("toggleHelpfulVote", UUID.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize, "Method toggleHelpfulVote phải có annotation @PreAuthorize");
        assertTrue(preAuthorize.value().contains("hasRole('BUYER')"),
                "@PreAuthorize phải yêu cầu hasRole('BUYER')");
    }

    @Test
    @DisplayName("TC-42: controller_toggleHelpful_Success_Vote_Returns200 - Trả về 200 OK khi đánh dấu hữu ích (Vote)")
    void controller_toggleHelpful_Success_Vote_Returns200() {
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
        HelpfulVoteResponse responseDto = new HelpfulVoteResponse(reviewId, true, 5L);

        when(reviewService.toggleHelpfulVote(eq(buyerId), eq(reviewId))).thenReturn(responseDto);

        ApiResponse<HelpfulVoteResponse> response = controller.toggleHelpfulVote(reviewId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Review marked as helpful", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(reviewId, response.getData().reviewId());
        assertTrue(response.getData().isHelpful());
        assertEquals(5L, response.getData().helpfulCount());

        verify(reviewService, times(1)).toggleHelpfulVote(eq(buyerId), eq(reviewId));
    }

    @Test
    @DisplayName("TC-43: controller_toggleHelpful_Success_Unvote_Returns200 - Trả về 200 OK khi hủy đánh dấu hữu ích (Unvote)")
    void controller_toggleHelpful_Success_Unvote_Returns200() {
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
        HelpfulVoteResponse responseDto = new HelpfulVoteResponse(reviewId, false, 4L);

        when(reviewService.toggleHelpfulVote(eq(buyerId), eq(reviewId))).thenReturn(responseDto);

        ApiResponse<HelpfulVoteResponse> response = controller.toggleHelpfulVote(reviewId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Review unmarked as helpful", response.getMessage());
        assertNotNull(response.getData());
        assertEquals(reviewId, response.getData().reviewId());
        assertFalse(response.getData().isHelpful());
        assertEquals(4L, response.getData().helpfulCount());

        verify(reviewService, times(1)).toggleHelpfulVote(eq(buyerId), eq(reviewId));
    }

    @Test
    @DisplayName("TC-44: controller_toggleHelpful_EndpointMapping_IsCorrect - Kiểm tra đúng @RequestMapping và @PostMapping")
    void controller_toggleHelpful_EndpointMapping_IsCorrect() throws NoSuchMethodException {
        RequestMapping classMapping = ReviewController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/api/reviews"}, classMapping.value());

        Method method = ReviewController.class.getMethod("toggleHelpfulVote", UUID.class);
        PostMapping methodMapping = method.getAnnotation(PostMapping.class);
        assertNotNull(methodMapping);
        assertArrayEquals(new String[]{"/{id}/helpful"}, methodMapping.value());
    }

    @Test
    @DisplayName("TC-45: dto_HelpfulVoteResponse_RecordIntegrity - Kiểm tra tính toàn vẹn của HelpfulVoteResponse Record")
    void dto_HelpfulVoteResponse_RecordIntegrity() {
        UUID reviewId = UUID.randomUUID();
        HelpfulVoteResponse dto = new HelpfulVoteResponse(reviewId, true, 10L);

        assertEquals(reviewId, dto.reviewId());
        assertTrue(dto.isHelpful());
        assertEquals(10L, dto.helpfulCount());
        assertTrue(dto.toString().contains("HelpfulVoteResponse"));
    }
}
