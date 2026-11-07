package com.techhub.controller.review;

import com.techhub.common.ApiResponse;
import com.techhub.service.review.AdminReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Unit Tests cho AdminReviewController (DELETE /api/admin/reviews/{id})")
class AdminReviewControllerTest {

    @Mock
    private AdminReviewService adminReviewService;

    @InjectMocks
    private AdminReviewController controller;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("TC-116: controller_deleteReview_RequiresAdminRole - Bắt buộc quyền ROLE_ADMIN")
    void controller_deleteReview_RequiresAdminRole() throws NoSuchMethodException {
        Method method = AdminReviewController.class.getMethod("deleteReview", UUID.class);

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize, "Method deleteReview phải có annotation @PreAuthorize");
        assertEquals("hasRole('ADMIN')", preAuthorize.value(), "Quyền hạn yêu cầu phải là hasRole('ADMIN')");

        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        assertNotNull(deleteMapping, "Method deleteReview phải có annotation @DeleteMapping");
        assertArrayEquals(new String[]{"/{id}"}, deleteMapping.value());
    }

    @Test
    @DisplayName("TC-117: controller_deleteReview_Success_Returns200 - Trả về HTTP 200 OK và message xác nhận")
    void controller_deleteReview_Success_Returns200() {
        UUID reviewId = UUID.randomUUID();
        doNothing().when(adminReviewService).deleteReview(reviewId);

        ApiResponse<Void> response = controller.deleteReview(reviewId);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Review removed successfully", response.getMessage());
        assertNull(response.getData());

        verify(adminReviewService, times(1)).deleteReview(reviewId);
    }

    @Test
    @DisplayName("TC-118: controller_deleteReview_CorrectRequestMapping - Đúng đường dẫn cấu hình /api/admin/reviews")
    void controller_deleteReview_CorrectRequestMapping() {
        RequestMapping classMapping = AdminReviewController.class.getAnnotation(RequestMapping.class);
        assertNotNull(classMapping);
        assertArrayEquals(new String[]{"/api/admin/reviews"}, classMapping.value());
    }
}
